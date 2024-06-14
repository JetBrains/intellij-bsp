package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.OutputPathsParams
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bsp.protocol.BazelBuildServer
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.DirectoryItem
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import org.jetbrains.plugins.bsp.android.androidSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.PerformanceLogger.logPerformance
import org.jetbrains.plugins.bsp.magicmetamodel.impl.TargetIdToModuleEntitiesMap
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesJava
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesScala
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toPair
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.WorkspaceModelProjectStructureDiff
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.connection.reactToExceptionIn
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.utils.findLibraryNameProvider
import org.jetbrains.plugins.bsp.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.utils.orDefault
import java.util.concurrent.CompletableFuture

internal class JVMSync : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean =
    true

  override fun execute(
    project: Project,
    server: BspServer,
    capabilities: BazelBuildServerCapabilities,
    baseInfos: List<BaseBspTargetInfo>,
    diff: AllProjectStructuresDiff,
    errorCallback: (Throwable) -> Unit,
    cancelOn: CompletableFuture<Void>
  ) {
    val allTargetsIds = baseInfos.map { it.target.id }

    val dependencySourcesFuture =
      query(capabilities.dependencySourcesProvider == true, "buildTarget/dependencySources",errorCallback, cancelOn) {
        server.buildTargetDependencySources(DependencySourcesParams(allTargetsIds))
      }

    val javaTargetIds = baseInfos.filter { it.target.languageIds.includesJava() }.map { it.target.id }
    val scalaTargetIds = baseInfos.filter { it.target.languageIds.includesScala() }.map { it.target.id }

    val libraries: WorkspaceLibrariesResult? =
      query(capabilities.workspaceLibrariesProvider, "workspace/libraries",errorCallback, cancelOn) {
        (server as BazelBuildServer).workspaceLibraries()
      }?.get()

    val directoriesFuture = query(capabilities.workspaceDirectoriesProvider, "workspace/directories",errorCallback, cancelOn) {
      (server as BazelBuildServer).workspaceDirectories()
    }

    val jvmBinaryJarsFuture = query(
      BspFeatureFlags.isAndroidSupportEnabled &&
          capabilities.jvmBinaryJarsProvider &&
          javaTargetIds.isNotEmpty(),
      "buildTarget/jvmBinaryJars",errorCallback, cancelOn
    ) {
      server.buildTargetJvmBinaryJars(JvmBinaryJarsParams(javaTargetIds))
    }

    // We use javacOptions only do build dependency tree based on classpath
    // If workspace/libraries endpoint is available (like in bazel-bsp)
    // we don't need javacOptions at all. For other servers (like SBT)
    // we still need to retrieve it
    // There's no capability for javacOptions
    val javacOptionsFuture = if (libraries == null)
      query(javaTargetIds.isNotEmpty(), "buildTarget/javacOptions", errorCallback, cancelOn) {
        server.buildTargetJavacOptions(JavacOptionsParams(javaTargetIds))
      }
    else null

    // Same for Scala
    val scalacOptionsFuture = if (libraries == null)
      query(scalaTargetIds.isNotEmpty() && BspFeatureFlags.isScalaSupportEnabled, "buildTarget/scalacOptions", errorCallback, cancelOn) {
        server.buildTargetScalacOptions(ScalacOptionsParams(scalaTargetIds))
      }
    else null

    val outputPathsFuture = query(capabilities.outputPathsProvider == true, "buildTarget/outputPaths", errorCallback, cancelOn) {
      server.buildTargetOutputPaths(OutputPathsParams(allTargetsIds))
    }

    val pp = ProjectDetails(
      targetsId = allTargetsIds,
      targets = baseInfos.map { it.target }.toSet(),
      sources = baseInfos.flatMap { it.sources },
      resources = baseInfos.flatMap { it.resources },
      dependenciesSources = dependencySourcesFuture?.get()?.items ?: emptyList(),
      javacOptions = javacOptionsFuture?.get()?.items ?: emptyList(),
      scalacOptions = scalacOptionsFuture?.get()?.items ?: emptyList(),
      pythonOptions = emptyList(),
      outputPathUris =  emptyList(),
      libraries = libraries?.libraries,
      directories = directoriesFuture?.get()
        ?: WorkspaceDirectoriesResult(listOf(DirectoryItem(project.rootDir.url)), emptyList()),
      jvmBinaryJars = jvmBinaryJarsFuture?.get()?.items ?: emptyList(),
    )

    updateInternalModelSubtask(pp, project, diff.getDiff(WorkspaceModelProjectStructureDiff::class.java).diff)
  }

  private fun <Result> query(
    check: Boolean,
    queryName: String,
    errorCallback: (Throwable) -> Unit,
    cancelOn: CompletableFuture<Void>,
    doQuery: () -> CompletableFuture<Result>,
  ): CompletableFuture<Result>? =
    if (check) doQuery()
      .reactToExceptionIn(cancelOn)
      .catchSyncErrors(errorCallback)
      .exceptionally {
        thisLogger().warn("Query '$queryName' has failed", it)
        null
      }
    else null

  private fun updateInternalModelSubtask(projectDetails: ProjectDetails,     project: Project, diff: MutableEntityStorage) {
        val projectBasePath = project.rootDir.toNioPath()
        val moduleNameProvider = project.findModuleNameProvider().orDefault()
        val libraryNameProvider = project.findLibraryNameProvider().orDefault()

        val targetIdToModuleEntitiesMap = logPerformance("create-target-id-to-module-entities-map") {
          val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails)

          project.temporaryTargetUtils.saveTargets(
            targetIds = projectDetails.targetsId,
            transformer = transformer,
            libraries = projectDetails.libraries,
            moduleNameProvider = moduleNameProvider,
            libraryNameProvider = libraryNameProvider,
            defaultJdkName = projectDetails.defaultJdkName,
          )

          TargetIdToModuleEntitiesMap(
            projectDetails = projectDetails,
            projectBasePath = projectBasePath,
            targetsMap = projectDetails.targets.associate { it.toBuildTargetInfo().toPair() },
            moduleNameProvider = moduleNameProvider,
            libraryNameProvider = libraryNameProvider,
            isAndroidSupportEnabled = BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
            transformer = transformer,
          )
        }

        logPerformance("load-modules") {
          val workspaceModel = WorkspaceModel.getInstance(project)
          val virtualFileUrlManager = workspaceModel.getVirtualFileUrlManager()

          val workspaceModelUpdater = WorkspaceModelUpdater.create(
            diff,
            virtualFileUrlManager,
            projectBasePath,
            project,
            BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
          )

          val modulesToLoad = targetIdToModuleEntitiesMap.values

          workspaceModelUpdater.loadModules(modulesToLoad + project.temporaryTargetUtils.getAllLibraryModules())
          workspaceModelUpdater.loadLibraries(project.temporaryTargetUtils.getAllLibraries())
          workspaceModelUpdater
            .loadDirectories(projectDetails.directories, projectDetails.outputPathUris, virtualFileUrlManager)
        }

    }

  private fun WorkspaceModelUpdater.loadDirectories(
    directories: WorkspaceDirectoriesResult,
    outputPathUris: List<String>,
    virtualFileUrlManager: VirtualFileUrlManager,
  ) {
    val includedDirectories = directories.includedDirectories.map { virtualFileUrlManager.getOrCreateFromUri(it.uri) }
    val excludedDirectories = directories.excludedDirectories.map { virtualFileUrlManager.getOrCreateFromUri(it.uri) }
    val outputPaths = outputPathUris.map { virtualFileUrlManager.getOrCreateFromUri(it) }

    loadDirectories(includedDirectories, excludedDirectories + outputPaths)
  }
}
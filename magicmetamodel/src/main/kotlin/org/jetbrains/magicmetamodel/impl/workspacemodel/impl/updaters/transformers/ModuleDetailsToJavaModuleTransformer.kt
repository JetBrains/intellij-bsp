package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmBuildTarget
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.*
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

internal class ModuleDetailsToJavaModuleTransformer(
  moduleNameProvider: ((BuildTargetIdentifier) -> String)?,
  private val projectBasePath: Path,
): ModuleDetailsToModuleTransformer<JavaModule>(moduleNameProvider) {

  override val type = "JAVA_MODULE"
  private val sourcesItemToJavaSourceRootTransformer = SourcesItemToJavaSourceRootTransformer(projectBasePath)
  private val resourcesItemToJavaResourceRootTransformer = ResourcesItemToJavaResourceRootTransformer(projectBasePath)

  override fun transform(inputEntity: ModuleDetails): JavaModule =
    JavaModule(
      module = toModule(inputEntity),
      baseDirContentRoot = toBaseDirContentRoot(inputEntity),
      sourceRoots = sourcesItemToJavaSourceRootTransformer.transform(inputEntity.sources.map {
        BuildTargetAndSourceItem(
          inputEntity.target,
          it,
        )
      }),
      resourceRoots = resourcesItemToJavaResourceRootTransformer.transform(inputEntity.resources),
      libraries = DependencySourcesItemToLibraryTransformer.transform(inputEntity.dependenciesSources.map {
        DependencySourcesAndJavacOptions(
          it,
          inputEntity.javacOptions
        )
      }),
      compilerOutput = toCompilerOutput(inputEntity),
      jvmJdkInfo = toJdkInfo(inputEntity)
    )

  override fun toModule(inputEntity: ModuleDetails): Module {
    val bspModuleDetails = BspModuleDetails(
      target = inputEntity.target,
      allTargetsIds = inputEntity.allTargetsIds,
      dependencySources = inputEntity.dependenciesSources,
      type = type,
      javacOptions = inputEntity.javacOptions,
      pythonOptions = inputEntity.pythonOptions,
    )

    return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails).applyHACK(inputEntity, projectBasePath)
  }

  private fun Module.applyHACK(inputEntity: ModuleDetails, projectBasePath: Path): Module {
    val dummyJavaModuleDependencies = calculateDummyJavaModuleNames(inputEntity, projectBasePath).map { ModuleDependency(it) }
    return this.copy(modulesDependencies = modulesDependencies + dummyJavaModuleDependencies)
  }

  private fun toCompilerOutput(inputEntity: ModuleDetails): Path? =
    inputEntity.javacOptions?.classDirectory?.let { URI(it).toPath() }

  private fun toJdkInfo(inputEntity: ModuleDetails): JvmJdkInfo? {
    val jvmBuildTarget = extractJvmBuildTarget(inputEntity.target)
    return if (jvmBuildTarget != null) JvmJdkInfo(javaVersion = jvmBuildTarget.javaVersion, javaHome = jvmBuildTarget.javaHome)
    else null
  }
}

public fun extractJvmBuildTarget(target: BuildTarget): JvmBuildTarget? =
  if (target.dataKind == BuildTargetDataKind.JVM) Gson().fromJson(target.data as JsonObject, JvmBuildTarget::class.java)
  else null

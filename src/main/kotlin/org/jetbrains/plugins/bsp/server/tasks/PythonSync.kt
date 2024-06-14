package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsParams
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesPython
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.WorkspaceModelProjectStructureDiff
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.connection.reactToExceptionIn
import org.jetbrains.workspacemodel.entities.BspEntitySource
import java.util.concurrent.CompletableFuture


public class PythonSync : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean =
    false

  override fun execute(
    project: Project,
    server: BspServer,
    capabilities: BazelBuildServerCapabilities,
    baseInfos: List<BaseBspTargetInfo>,
    diff: AllProjectStructuresDiff,
    errorCallback: (Throwable) -> Unit,
    cancelOn: CompletableFuture<Void>,
  ) {
    println("PPPP")
    val pythonTargetsIds = baseInfos.filter { it.target.languageIds.includesPython() }.map { it.target.id }
    val pythonOptions =
      query(pythonTargetsIds.isNotEmpty(), "buildTarget/pythonOptions", errorCallback, cancelOn) {
        server.buildTargetPythonOptions(PythonOptionsParams(pythonTargetsIds))
      }?.get() ?: error("XD")

    val rr = diff.getDiff(WorkspaceModelProjectStructureDiff::class.java)
    val tt = rr.diff

    val workspaceModel = WorkspaceModel.getInstance(project)
    val virtualFileUrlManager = workspaceModel.getVirtualFileUrlManager()

    tt.add(baseInfos, pythonOptions.items, virtualFileUrlManager)
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

  private fun MutableEntityStorage.add(baseInfos: List<BaseBspTargetInfo>, python: List<PythonOptionsItem>, v: VirtualFileUrlManager) {
    baseInfos.forEach { baseInfo ->
      val pyth = python.filter { it.target == baseInfo.target }
      add1(baseInfo, pyth, v)
    }
  }

  private fun MutableEntityStorage.add1(baseInfo: BaseBspTargetInfo, python: List<PythonOptionsItem>, v: VirtualFileUrlManager) {
    val deps = baseInfo.target.dependencies.map {
      ModuleDependency(
        module = ModuleId(it.uri),
        exported = false,
        scope = DependencyScope.COMPILE,
        productionOnTest = true,
      )
    }
    val module = addEntity(
      ModuleEntity(
        name = baseInfo.target.id.uri, // TODO name mod
        dependencies = deps,
        entitySource = BspEntitySource // TODO,
      ) {
        this.type = "PYTHON_MODULE"
      }
    )

    baseInfo
      .sources
      .flatMap { it.sources }
      .forEach {
        val content = addEntity(
          ContentRootEntity(
            url = v.getOrCreateFromUri(it.uri) ?: error("XD"), // TODO,
            excludedPatterns = ArrayList(),
            entitySource = BspEntitySource // TODO,
          ) {
//            this.excludedUrls = excludes
            this.module = module
          },
        )

        addEntity(
          SourceRootEntity(
            url = v.getOrCreateFromUri(it.uri) ?: error("XD"), // TODO
            rootType = "python-source",
            entitySource = BspEntitySource // TODO,
          ) {
            this.contentRoot = content
          },
        )
      }

//
//    private fun calculateModuleDefaultDependencies(entityToAdd: PythonModule): List<ModuleDependencyItem> =
//      if (isPythonSupportEnabled && entityToAdd.sdkInfo != null)
//        defaultDependencies + SdkDependency(SdkId(entityToAdd.sdkInfo.toString(), PYTHON_SDK_ID))
//      else
//        defaultDependencies
//
//    private companion object {
//      val defaultDependencies = listOf(
//        ModuleSourceDependency,
//      )
//    }
  }
}

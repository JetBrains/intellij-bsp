package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsParams
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.SdkEntity
import com.intellij.platform.workspace.jps.entities.SdkRoot
import com.intellij.platform.workspace.jps.entities.SdkRootTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.impl.legacyBridge.sdk.SdkBridgeImpl
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.PythonSdkInfo.Companion.PYTHON_SDK_ID
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesPython
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.WorkspaceModelProjectStructureDiff
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.connection.reactToExceptionIn
import org.jetbrains.workspacemodel.entities.BspEntitySource
import java.util.concurrent.CompletableFuture

private val defaultModuleDependencies = listOf(ModuleSourceDependency)

public class PythonSync : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean =
    BspFeatureFlags.isPythonSupportEnabled || true

  override fun execute(
    project: Project,
    server: BspServer,
    capabilities: BazelBuildServerCapabilities,
    baseInfos: List<BaseBspTargetInfo>,
    diff: AllProjectStructuresDiff,
    errorCallback: (Throwable) -> Unit,
    cancelOn: CompletableFuture<Void>,
  ) {
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

    addEntity(
      SdkEntity(
        additionalData = "",
        entitySource = SdkBridgeImpl.createEntitySourceForSdk(),
        name = "XD4",
        roots = listOf(),
        type = PythonSdkType.getInstance().name
      ) {
        homePath = v.getOrCreateFromUri("/opt/homebrew/bin/python3")
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
  }

//    private fun calculateModuleDefaultDependencies(entityToAdd: PythonModule): List<ModuleDependencyItem> =
//      if (isPythonSupportEnabled && entityToAdd.sdkInfo != null)
//        defaultDependencies + SdkDependency(SdkId(entityToAdd.sdkInfo.toString(), PYTHON_SDK_ID))
//      else
//        defaultDependencies
}

public data class PythonSdkInfo(val version: String, val originalName: String) {
  override fun toString(): String = "$originalName$SEPARATOR$version"

  public companion object {
    public const val PYTHON_SDK_ID: String = "PythonSDK"
    private const val SEPARATOR = '-'
    public fun fromString(value: String): PythonSdkInfo? {
      val parts = value.split(SEPARATOR)
      return parts.takeIf { it.size == 2 }?.let {
        PythonSdkInfo(it[0], it[1])
      }
    }
  }
}
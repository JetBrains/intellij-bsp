package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModule
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleDependencyItem
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import java.nio.file.Path

internal data class PythonSdkInfo(val version: String, val interpreter: Path)

internal data class PythonLibrary(val sources: String?) : WorkspaceModelEntity()

internal data class PythonModule(
  val module: Module,
  val sourceRoots: List<GenericSourceRoot>,
  val resourceRoots: List<PythonResourceRoot>,
  val libraries: List<PythonLibrary>,
  val sdkInfo: PythonSdkInfo?,
) : WorkspaceModelEntity()

internal class PythonModuleWithSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity> {

  override fun addEntity(entityToAdd: PythonModule): ModuleEntity {
    val moduleEntityUpdater =
      ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, calculateModuleDefaultDependencies(entityToAdd))

    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.module)

    val sourceEntityUpdater = SourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    sourceEntityUpdater.addEntries(entityToAdd.sourceRoots, moduleEntity)

    val pythonResourceEntityUpdater = PythonResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    pythonResourceEntityUpdater.addEntries(entityToAdd.resourceRoots, moduleEntity)

    val module = moduleEntity.findModule(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder)
    if (module != null && entityToAdd.sdkInfo != null) {
      val modifiableModule = ModuleRootManager.getInstance(module).modifiableModel
      modifiableModule.sdk = ProjectJdkTable.getInstance().findJdk(entityToAdd.sdkInfo.version, "PythonSDK")
      modifiableModule.commit()
    }

    return moduleEntity
  }

  private fun calculateModuleDefaultDependencies(entityToAdd: PythonModule): List<ModuleDependencyItem> =
    if (entityToAdd.sdkInfo != null) {
      defaultDependencies + ModuleDependencyItem.SdkDependency(entityToAdd.sdkInfo.version, "PythonSDK")
    } else defaultDependencies

  private companion object {
    val defaultDependencies = listOf(
      ModuleDependencyItem.ModuleSourceDependency,
    )
  }
}

internal class PythonModuleWithoutSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity> {

  override fun addEntity(entityToAdd: PythonModule): ModuleEntity {
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig)
    return moduleEntityUpdater.addEntity(entityToAdd.module)
  }
}

internal class PythonModuleUpdater(
  workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity> {

  private val pythonModuleWithSourcesUpdater = PythonModuleWithSourcesUpdater(workspaceModelEntityUpdaterConfig)
  private val pythonModuleWithoutSourcesUpdater = PythonModuleWithoutSourcesUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(entityToAdd: PythonModule): ModuleEntity =
    when (Pair(entityToAdd.sourceRoots.size, entityToAdd.resourceRoots.size)) {
      Pair(0, 0) -> pythonModuleWithoutSourcesUpdater.addEntity(entityToAdd)
      else -> pythonModuleWithSourcesUpdater.addEntity(entityToAdd)
    }
}
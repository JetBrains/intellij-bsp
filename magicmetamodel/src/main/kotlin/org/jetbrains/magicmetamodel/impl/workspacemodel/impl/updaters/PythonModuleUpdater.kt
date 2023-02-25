package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity

internal data class PythonModule(
  val module: Module,
  // todo - add other fields
) : WorkspaceModelEntity()

// todo
internal class PythonModuleWithSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity> {
  override fun addEntity(entityToAdd: PythonModule): ModuleEntity {
    TODO("Not yet implemented")
  }

}


internal class PythonModuleWithoutSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity> {

  override fun addEntity(entityToAdd: PythonModule): ModuleEntity {
    TODO("Not yet implemented")
  }
}

internal class PythonModuleUpdater(
  workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity> {

  override fun addEntity(entityToAdd: PythonModule): ModuleEntity {
    TODO("Not yet implemented")
  }
}
package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.goide.vgo.project.workspaceModel.entities.VgoDependencyEntity
import com.goide.vgo.project.workspaceModel.entities.VgoStandaloneModuleEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GoModule
import org.jetbrains.workspacemodel.entities.BspEntitySource

internal class GoModuleUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<GoModule, VgoStandaloneModuleEntity> {
  override fun addEntity(entityToAdd: GoModule): VgoStandaloneModuleEntity {
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig)
    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.module)

    val vgoModule = VgoStandaloneModuleEntity(
      moduleId = moduleEntity.symbolicId,
      entitySource = BspEntitySource,
      importPath = entityToAdd.importPath,
      root = entityToAdd.root.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
    )
    val builtVgoModule = workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(vgoModule)

    entityToAdd.goDependencies?.forEach {
      val vgoModuleDeps = VgoDependencyEntity(
        importPath = it.importPath,
        entitySource = BspEntitySource,
        isMainModule = false,
        internal = true,
      ) {
        this.module = vgoModule
        this.root = it.root.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
      }
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(vgoModuleDeps)
    }

    return builtVgoModule
  }
}
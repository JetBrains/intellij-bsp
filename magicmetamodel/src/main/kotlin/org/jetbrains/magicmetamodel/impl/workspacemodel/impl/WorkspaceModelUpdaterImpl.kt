package org.jetbrains.magicmetamodel.impl.workspacemodel.impl

import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleName
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonModuleUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntityUpdaterConfig
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModuleRemover
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToPythonModuleTransformer

internal class WorkspaceModelUpdaterImpl(
  workspaceEntityStorageBuilder: MutableEntityStorage,
  val virtualFileUrlManager: VirtualFileUrlManager,
) : WorkspaceModelUpdater {

  private val workspaceModelEntityUpdaterConfig = WorkspaceModelEntityUpdaterConfig(
    workspaceEntityStorageBuilder = workspaceEntityStorageBuilder,
    virtualFileUrlManager = virtualFileUrlManager,
  )
  private val javaModuleUpdater = JavaModuleUpdater(workspaceModelEntityUpdaterConfig)
  private val pythonModuleUpdater = PythonModuleUpdater(workspaceModelEntityUpdaterConfig)
  private val workspaceModuleRemover = WorkspaceModuleRemover(workspaceModelEntityUpdaterConfig)

  override fun loadModule(moduleDetails: ModuleDetails) {
    // TODO for now we are supporting only java modules
    val javaModule = ModuleDetailsToJavaModuleTransformer.transform(moduleDetails)
    val pythonModule = ModuleDetailsToPythonModuleTransformer.transform(moduleDetails)

    javaModuleUpdater.addEntity(javaModule)
    pythonModuleUpdater.addEntity(pythonModule)
  }

  override fun removeModule(module: ModuleName) {
    workspaceModuleRemover.removeEntity(module)
  }

  override fun clear() {
    workspaceModuleRemover.clear()
  }
}

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleId
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleName
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonModuleUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntityUpdaterConfig
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModuleRemover
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToDummyJavaModulesTransformerHACK
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToPythonModuleTransformer
import java.nio.file.Path

internal class WorkspaceModelUpdaterImpl(
  workspaceEntityStorageBuilder: MutableEntityStorage,
  val virtualFileUrlManager: VirtualFileUrlManager,
  moduleNameProvider: ModuleNameProvider,
  projectBasePath: Path,
) : WorkspaceModelUpdater {

  private val workspaceModelEntityUpdaterConfig = WorkspaceModelEntityUpdaterConfig(
    workspaceEntityStorageBuilder = workspaceEntityStorageBuilder,
    virtualFileUrlManager = virtualFileUrlManager,
    projectBasePath = projectBasePath
  )
  private val javaModuleUpdater = JavaModuleUpdater(workspaceModelEntityUpdaterConfig)
  private val pythonModuleUpdater = PythonModuleUpdater(workspaceModelEntityUpdaterConfig)
  private val workspaceModuleRemover = WorkspaceModuleRemover(workspaceModelEntityUpdaterConfig)
  private val moduleDetailsToJavaModuleTransformer = ModuleDetailsToJavaModuleTransformer(moduleNameProvider, projectBasePath)
  private val moduleDetailsToPythonModuleTransformer = ModuleDetailsToPythonModuleTransformer(moduleNameProvider, projectBasePath)
  private val moduleDetailsToDummyJavaModulesTransformerHACK = ModuleDetailsToDummyJavaModulesTransformerHACK(projectBasePath)

  override fun loadModule(moduleDetails: ModuleDetails) {
    // TODO for now we are supporting only java modules
    val dummyJavaModules = moduleDetailsToDummyJavaModulesTransformerHACK.transform(moduleDetails)
    javaModuleUpdater.addEntries(dummyJavaModules.filterNot { it.module.isAlreadyAdded() })
//   TODO  pythonModuleUpdater.addEntries(dummyJavaModules.filterNot { it.module.isAlreadyAdded() })
    val javaModule = moduleDetailsToJavaModuleTransformer.transform(moduleDetails)
    val pythonModule = moduleDetailsToPythonModuleTransformer.transform(moduleDetails)
    javaModuleUpdater.addEntity(javaModule)
    pythonModuleUpdater.addEntity(pythonModule)

  }

  private fun Module.isAlreadyAdded() = workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.contains(ModuleId(this.name))

  override fun removeModule(module: ModuleName) {
    workspaceModuleRemover.removeEntity(module)
  }

  override fun clear() {
    workspaceModuleRemover.clear()
  }
}

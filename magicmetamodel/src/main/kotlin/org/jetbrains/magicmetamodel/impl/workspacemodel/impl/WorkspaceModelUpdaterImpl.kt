package org.jetbrains.magicmetamodel.impl.workspacemodel.impl

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleName
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.BspEntitySource
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntityUpdaterConfig
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModuleRemover
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import java.net.URI
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

  private val workspaceModuleRemover = WorkspaceModuleRemover(workspaceModelEntityUpdaterConfig)
  private val moduleDetailsToJavaModuleTransformer =
    ModuleDetailsToJavaModuleTransformer(moduleNameProvider, projectBasePath)

  override fun loadModule(moduleDetails: ModuleDetails) {
    // TODO for now we are supporting only java modules
    val javaModule = moduleDetailsToJavaModuleTransformer.transform(moduleDetails)
    javaModuleUpdater.addEntity(javaModule)
  }

  override fun loadLibraries(libraries: List<Library>) {
    libraries.forEach { entityToAdd ->
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(
        LibraryEntity(
          name = entityToAdd.displayName,
          tableId = LibraryTableId.ProjectLibraryTableId,
          roots = listOfNotNull(entityToAdd.classesJar?.let {
            LibraryRoot(
              url = workspaceModelEntityUpdaterConfig.virtualFileUrlManager.fromUrl("jar://${URI.create(it).path}!/"),
              type = LibraryRootTypeId.COMPILED
            )
          }),
          entitySource = BspEntitySource,
        ) {
          this.excludedRoots = arrayListOf()
        }
      )
    }
  }

  override fun removeModule(module: ModuleName) {
    workspaceModuleRemover.removeEntity(module)
  }

  override fun clear() {
    workspaceModuleRemover.clear()
  }
}

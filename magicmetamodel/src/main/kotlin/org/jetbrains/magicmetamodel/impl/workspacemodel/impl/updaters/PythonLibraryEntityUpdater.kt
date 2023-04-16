package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryEntity
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRoot
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRootTypeId
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleId
import com.intellij.workspaceModel.storage.bridgeEntities.addLibraryEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.annotation.Untainted
import kotlin.io.path.Path


internal data class PythonLibrary(
  val displayName: String,
  val sources: String?,
) : WorkspaceModelEntity()

internal class PythonLibraryEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<PythonLibrary, LibraryEntity> {

  override fun addEntity(entityToAdd: PythonLibrary, parentModuleEntity: ModuleEntity): LibraryEntity {
    val sourcesInHelpers = copyExternalSources(entityToAdd)

    return addPythonLibraryEntity(
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      parentModuleEntity,
      entityToAdd.copy(sources = sourcesInHelpers.toString())
    )
  }

  private fun copyExternalSources(entityToAdd: PythonLibrary): Path? =
    entityToAdd.sources?.let { Files.copy(Path(it), workspaceModelEntityUpdaterConfig.pythonHelpersPath, StandardCopyOption.REPLACE_EXISTING) }

  private fun addPythonLibraryEntity(
    builder: MutableEntityStorage,
    parentModuleEntity: ModuleEntity,
    entityToAdd: PythonLibrary,
  ): LibraryEntity {
      return builder.addLibraryEntity(
      entityToAdd.displayName,
      tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
      roots = listOfNotNull(toLibrarySourcesRoot(entityToAdd)),
      excludedRoots = ArrayList(),
      source = DoNotSaveInDotIdeaDirEntitySource
    )
  }

  private fun toLibrarySourcesRoot(entityToAdd: PythonLibrary): LibraryRoot? =
    entityToAdd.sources?.let {
      LibraryRoot(
        url = workspaceModelEntityUpdaterConfig.virtualFileUrlManager.fromUrl(it),
        type = LibraryRootTypeId.SOURCES,
      )
    }
}
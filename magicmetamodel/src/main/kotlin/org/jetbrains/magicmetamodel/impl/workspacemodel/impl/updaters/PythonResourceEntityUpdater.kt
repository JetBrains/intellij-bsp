package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import java.nio.file.Path
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.SourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addSourceRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl

internal data class PythonResourceRoot(
  val resourcePath: Path,
) : WorkspaceModelEntity()

internal class PythonResourceEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<PythonResourceRoot, SourceRootEntity> {

  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(entityToAdd: PythonResourceRoot, parentModuleEntity: ModuleEntity): SourceRootEntity {
    val contentRootEntity = addContentRootEntity(entityToAdd, parentModuleEntity)

    return addSourceRootEntity(
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      contentRootEntity,
      entityToAdd
    )
  }

  private fun addContentRootEntity(
    entityToAdd: PythonResourceRoot,
    parentModuleEntity: ModuleEntity
  ): ContentRootEntity {
    val contentRoot = ContentRoot(
      url = entityToAdd.resourcePath
    )

    return contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
  }

  private fun addSourceRootEntity(
    builder: MutableEntityStorage,
    contentRootEntity: ContentRootEntity,
    entityToAdd: PythonResourceRoot,
  ): SourceRootEntity = builder.addSourceRootEntity(
    contentRoot = contentRootEntity,
    url = entityToAdd.resourcePath.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
    rootType = ROOT_TYPE,
    source = DoNotSaveInDotIdeaDirEntitySource,
  )

  private companion object {
    private const val DEFAULT_GENERATED = false
    private const val DEFAULT_RELATIVE_OUTPUT_PATH = ""

    private const val ROOT_TYPE = "python-resource"
  }
}
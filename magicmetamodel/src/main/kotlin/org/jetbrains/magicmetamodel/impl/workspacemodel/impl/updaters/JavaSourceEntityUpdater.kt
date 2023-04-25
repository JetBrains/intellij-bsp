package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.JavaSourceRootPropertiesEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.SourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addJavaSourceRootEntity
import java.nio.file.Path

internal data class JavaSourceRoot(
  val sourcePath: Path,
  val generated: Boolean,
  val packagePrefix: String,
  val rootType: String,
  val excludedFiles: List<Path> = ArrayList(),
  val targetId: BuildTargetIdentifier
) : WorkspaceModelEntity()

internal class JavaSourceEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaSourceRoot, JavaSourceRootPropertiesEntity> {

  private val sourceEntityUpdater = SourceEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(
    entityToAdd: JavaSourceRoot,
    parentModuleEntity: ModuleEntity
  ): JavaSourceRootPropertiesEntity {
    val sourceRootEntity = sourceEntityUpdater.addEntity(entityToAdd.run {
      GenericSourceRoot(
        sourcePath,
        rootType,
        excludedFiles,
        targetId
      )
    }, parentModuleEntity)

    return addJavaSourceRootEntity(
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      sourceRootEntity,
      entityToAdd
    )
  }

  private fun addJavaSourceRootEntity(
    builder: MutableEntityStorage,
    sourceRoot: SourceRootEntity,
    entityToAdd: JavaSourceRoot,
  ): JavaSourceRootPropertiesEntity = builder.addJavaSourceRootEntity(
    sourceRoot = sourceRoot,
    generated = entityToAdd.generated,
    packagePrefix = entityToAdd.packagePrefix,
  )
}

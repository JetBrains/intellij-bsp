package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.JavaSourceRootPropertiesEntity
import com.intellij.workspaceModel.storage.bridgeEntities.SourceRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import org.jetbrains.workspace.model.matchers.entries.ExpectedContentRootEntity
import org.jetbrains.workspace.model.matchers.entries.ExpectedSourceRootEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelWithParentJavaModuleBaseTest
import org.jetbrains.workspace.model.test.framework.WorkspaceModelWithParentPythonModuleBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

@DisplayName("pythonSourceEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
class PythonSourceEntityUpdaterTest : WorkspaceModelWithParentPythonModuleBaseTest() {

  private lateinit var pythonSourceEntityUpdater: PythonSourceEntityUpdater

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager)
    pythonSourceEntityUpdater = PythonSourceEntityUpdater(workspaceModelEntityUpdaterConfig)
  }

  @Test
  fun `should add one python source root to the workspace model`() {
    // given
    val sourceDir = URI.create("file:///root/dir/example/package/").toPath()
    val generated = false

    val pythonSourceRoot = PythonSourceRoot(
      sourcePath = sourceDir,
      generated = generated,
      rootType = "python-source",
      targetId = BuildTargetIdentifier("target"),
    )

    // when
    val returnedPythonSourceRootEntity = runTestWriteAction {
      pythonSourceEntityUpdater.addEntity(pythonSourceRoot, parentModuleEntity)
    }

    // then
    val virtualSourceDir = sourceDir.toVirtualFileUrl(virtualFileUrlManager)
    val expectedContentRootEntity = ExpectedContentRootEntity(
      url = virtualSourceDir,
      excludedPatterns = emptyList(),
      parentModuleEntity = parentModuleEntity,
      excludedUrls = emptyList(),
    )

    // todo - is sourceRoot for python correct?
    val expectedPythonSourceRootEntity = ExpectedSourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir,
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir,
        rootType = "python-source"
      ) {},
      parentModuleEntity = parentModuleEntity,
    )
    // todo
    returnedPythonSourceRootEntity.contentRoot shouldBeEqual expectedContentRootEntity
    loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedPythonSourceRootEntity)
  }

  @Test
  fun `should add multiple python source roots to the workspace model`() {
    // given
    val sourceDir1 = URI.create("file:///root/dir/example/package/").toPath()
    val generated1 = false

    val pythonSourceRoot1 = PythonSourceRoot(
      sourcePath = sourceDir1,
      generated = generated1,
      rootType = "python-source",
      targetId = BuildTargetIdentifier("target1"),
    )

    val sourceDir2 = URI.create("file:///another/root/dir/another/example/package/").toPath()
    val generated2 = true

    val pythonSourceRoot2 = PythonSourceRoot(
      sourcePath = sourceDir2,
      generated = generated2,
      rootType = "python-test",
      targetId = BuildTargetIdentifier("target2"),
    )

    val pythonSourceRoots = listOf(pythonSourceRoot1, pythonSourceRoot2)

    // when
    val returnedPythonSourceRootEntities = runTestWriteAction {
      pythonSourceEntityUpdater.addEntries(pythonSourceRoots, parentModuleEntity)
    }

    // then
    val virtualSourceDir1 = sourceDir1.toVirtualFileUrl(virtualFileUrlManager)
    val expectedContentRootEntity1 = ExpectedContentRootEntity(
      url = virtualSourceDir1,
      excludedPatterns = emptyList(),
      parentModuleEntity = parentModuleEntity,
      excludedUrls = emptyList(),
    )

    val expectedPythonSourceRootEntity1 = ExpectedSourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir1,
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir1,
        rootType = "python-source"
      ) {},
      parentModuleEntity = parentModuleEntity,
    )

    val virtualSourceDir2 = sourceDir2.toVirtualFileUrl(virtualFileUrlManager)
    val expectedContentRootEntity2 = ExpectedContentRootEntity(
      url = virtualSourceDir2,
      excludedPatterns = emptyList(),
      parentModuleEntity = parentModuleEntity,
      excludedUrls = emptyList(),
    )

    val expectedPythonSourceRootEntity2 = ExpectedSourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir2,
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir2,
        rootType = "python-test"
      ) {},
      parentModuleEntity = parentModuleEntity,
    )

    val expectedPythonContentRootEntities = listOf(expectedContentRootEntity1, expectedContentRootEntity2)
    val expectedPythonSourceRootEntities = listOf(expectedPythonSourceRootEntity1, expectedPythonSourceRootEntity2)

    // TODO - making separate expected Content and Source RootEntities, change?
    returnedPythonSourceRootEntities.map { it.contentRoot } shouldContainExactlyInAnyOrder expectedPythonContentRootEntities
    loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder expectedPythonSourceRootEntities
  }
}

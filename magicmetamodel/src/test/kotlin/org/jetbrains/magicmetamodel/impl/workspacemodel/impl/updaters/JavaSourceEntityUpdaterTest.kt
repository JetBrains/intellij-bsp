package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.api.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.JavaSourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.SourceRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import org.jetbrains.workspace.model.matchers.entries.ExpectedJavaSourceRootEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelWithParentJavaModuleBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

@DisplayName("javaSourceEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
class JavaSourceEntityUpdaterTest : WorkspaceModelWithParentJavaModuleBaseTest() {

  private lateinit var javaSourceEntityUpdater: JavaSourceEntityUpdater

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectConfigSource)
    javaSourceEntityUpdater = JavaSourceEntityUpdater(workspaceModelEntityUpdaterConfig)
  }

  @Test
  fun `should add one java source root to the workspace model`() {
    // given
    val sourceDir = URI.create("file:///root/dir/example/package/").toPath()
    val generated = false
    val packagePrefix = "example.package"

    val javaSourceRoot = JavaSourceRoot(sourceDir, generated, packagePrefix)

    // when
    val returnedJavaSourceRootEntity = runTestWriteAction {
      javaSourceEntityUpdater.addEntity(javaSourceRoot, parentModuleEntity)
    }

    // then
    val virtualSourceDir = sourceDir.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaSourceRootEntity = ExpectedJavaSourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir,
        excludedUrls = emptyList(),
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir,
        rootType = "java-source"
      ),
      javaSourceRootEntity = JavaSourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        generated = generated,
        packagePrefix = packagePrefix
      ),
      parentModuleEntity = parentModuleEntity,
    )

    returnedJavaSourceRootEntity shouldBeEqual expectedJavaSourceRootEntity
    loadedEntries(JavaSourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRootEntity)
  }

  @Test
  fun `should add multiple java source roots to the workspace model`() {
    // given
    val sourceDir1 = URI.create("file:///root/dir/example/package/").toPath()
    val generated1 = false
    val packagePrefix1 = "example.package"

    val javaSourceRoot1 = JavaSourceRoot(sourceDir1, generated1, packagePrefix1)

    val sourceDir2 = URI.create("file:///another/root/dir/another/example/package/").toPath()
    val generated2 = true
    val packagePrefix2 = "another.example.package"

    val javaSourceRoot2 = JavaSourceRoot(sourceDir2, generated2, packagePrefix2)

    val javaSourceRoots = listOf(javaSourceRoot1, javaSourceRoot2)

    // when
    val returnedJavaSourceRootEntries = runTestWriteAction {
      javaSourceEntityUpdater.addEntries(javaSourceRoots, parentModuleEntity)
    }

    // then
    val virtualSourceDir1 = sourceDir1.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaSourceRootEntity1 = ExpectedJavaSourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir1,
        excludedUrls = emptyList(),
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir1,
        rootType = "java-source"
      ),
      javaSourceRootEntity = JavaSourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        generated = generated1,
        packagePrefix = packagePrefix1
      ),
      parentModuleEntity = parentModuleEntity,
    )

    val virtualSourceDir2 = sourceDir2.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaSourceRootEntity2 = ExpectedJavaSourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir2,
        excludedUrls = emptyList(),
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualSourceDir2,
        rootType = "java-source"
      ),
      javaSourceRootEntity = JavaSourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        generated = generated2,
        packagePrefix = packagePrefix2
      ),
      parentModuleEntity = parentModuleEntity,
    )

    val expectedJavaSourceRootEntries = listOf(expectedJavaSourceRootEntity1, expectedJavaSourceRootEntity2)

    returnedJavaSourceRootEntries shouldContainExactlyInAnyOrder expectedJavaSourceRootEntries
    loadedEntries(JavaSourceRootEntity::class.java) shouldContainExactlyInAnyOrder expectedJavaSourceRootEntries
  }
}

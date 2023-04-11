package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.LibraryEntity
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRoot
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRootTypeId
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleId
import org.jetbrains.workspace.model.matchers.entries.ExpectedLibraryEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelWithParentJavaModuleBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

@DisplayName("PythonLibraryEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
internal class PythonLibraryEntityUpdaterTest : WorkspaceModelWithParentJavaModuleBaseTest() {

  private lateinit var pythonLibraryEntityUpdater: PythonLibraryEntityUpdater
  private lateinit var helpers: Path

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    helpers = createTempDirectory("helpers-")

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath, helpers)
    pythonLibraryEntityUpdater = PythonLibraryEntityUpdater(workspaceModelEntityUpdaterConfig)
  }

  @Test
  fun `should add one pythonLibrary to the workspace model`() {
    // given
    val source = createTempFile()
    source.toFile().deleteOnExit()

    val pythonLibrary = PythonLibrary(
      displayName = "BSP: test-1.0.0",
      sources = source.toString(),
    )

    // when
    val returnedPythonLibraryEntity = runTestWriteAction {
      pythonLibraryEntityUpdater.addEntity(pythonLibrary, parentModuleEntity)
    }

    // then
    val expectedPythonLibraryRoot = LibraryRoot(
      url = virtualFileUrlManager.fromUrl(source.fileName.resolve(helpers).toString()),
      type = LibraryRootTypeId.SOURCES,
    )

    val expectedPythonLibraryEntity = ExpectedLibraryEntity(
      libraryEntity = LibraryEntity(
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        name = "BSP: test-1.0.0",
        roots = listOf(expectedPythonLibraryRoot),
        entitySource = DoNotSaveInDotIdeaDirEntitySource,
      )
    )

    // TODO checking whether the files are actually copied
    returnedPythonLibraryEntity shouldBeEqual expectedPythonLibraryEntity
    loadedEntries(LibraryEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedPythonLibraryEntity)
  }

  @Test
  fun `should add multiple libraries to the workspace model`() {
    // given

    val source1 = createTempFile()
    source1.toFile().deleteOnExit()

    val source2 = createTempFile()
    source2.toFile().deleteOnExit()

    val pythonLibrary1 = PythonLibrary(
      displayName = "BSP: test1-1.0.0",
      sources = source1.toString()
    )

    val pythonLibrary2 = PythonLibrary(
      displayName = "BSP: test2-2.0.0",
      sources = source2.toString()
    )

    val libraries = listOf(pythonLibrary1, pythonLibrary2)

    // when
    val returnedPythonLibraryEntries = runTestWriteAction {
      pythonLibraryEntityUpdater.addEntries(libraries, parentModuleEntity)
    }

    // then
    val expectedPythonLibrarySourcesRoot1 = LibraryRoot(
      url = virtualFileUrlManager.fromUrl(source1.fileName.resolve(helpers).toString()),
      type = LibraryRootTypeId.SOURCES,
    )

    val expectedPythonLibraryEntity1 = ExpectedLibraryEntity(
      libraryEntity = LibraryEntity(
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        name = "BSP: test1-1.0.0",
        roots = listOf(expectedPythonLibrarySourcesRoot1),
        entitySource = parentModuleEntity.entitySource,
      )
    )

    val expectedPythonLibrarySourcesRoot2 = LibraryRoot(
      url = virtualFileUrlManager.fromUrl(source2.fileName.resolve(helpers).toString()),
      type = LibraryRootTypeId.SOURCES,
    )

    val expectedPythonLibraryEntity2 = ExpectedLibraryEntity(
      libraryEntity = LibraryEntity(
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        name = "BSP: test2-2.0.0",
        roots = listOf(expectedPythonLibrarySourcesRoot2),
        entitySource = parentModuleEntity.entitySource,
      )
    )

    val expectedPythonLibraryEntries = listOf(expectedPythonLibraryEntity1, expectedPythonLibraryEntity2)

    returnedPythonLibraryEntries shouldContainExactlyInAnyOrder expectedPythonLibraryEntries
    loadedEntries(LibraryEntity::class.java) shouldContainExactlyInAnyOrder expectedPythonLibraryEntries
  }
}

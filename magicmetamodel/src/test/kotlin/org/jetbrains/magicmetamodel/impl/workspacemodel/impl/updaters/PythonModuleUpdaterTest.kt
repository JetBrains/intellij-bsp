@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.*
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import org.jetbrains.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.toPath
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor


internal class PythonModuleUpdaterTest : WorkspaceModelBaseTest() {

  @Nested
  @DisplayName("pythonModuleWithSourcesUpdater.addEntity(entityToAdd) tests")
  inner class PythonModuleWithSourcesUpdaterTest {
    @Test
    fun `should add one python module with sources to the workspace model`() {
      runTestForUpdaters(listOf(PythonModuleWithSourcesUpdater::class, PythonModuleUpdater::class)) { updater ->
        // given
        // todo - simplest possible case, no dependencies or libraries
        val module = Module(
          name = "module1",
          type = "PYTHON_MODULE",
          modulesDependencies = listOf(),
          librariesDependencies = listOf(),
        )

        // todo - add other fields one PythonModule expands
        val pythonModule = PythonModule(
          module = module,
        )


        // when
        val returnedModuleEntity = runTestWriteAction {
          updater.addEntity(pythonModule)
        }

        // then
        val expectedModuleEntity = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = DoNotSaveInDotIdeaDirEntitySource,
            dependencies = listOf()
          ) {
            type = "PYTHON_MODULE"
          }
        )

        // first basic check
        returnedModuleEntity shouldBeEqual expectedModuleEntity
        // todo - test other things

      }
    }
  }

  // todo - maybe needs more updates from Java's version other than JavaModule -> PythonModule
  private fun runTestForUpdaters(
    updaters: List<KClass<out WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity>>>,
    test: (WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity>) -> Unit,
  ) =
    updaters
      .map { it.primaryConstructor!! }
      .forEach { runTest(it, test) }

  private fun runTest(
    updaterConstructor: KFunction<WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity>>,
    test: (WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity>) -> Unit,
  ) {
    beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager)

    test(updaterConstructor.call(workspaceModelEntityUpdaterConfig))
  }
}

@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.workspaceModel.storage.bridgeEntities.*
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import org.jetbrains.workspace.model.matchers.entries.*
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
        val module = Module(
          name = "module1",
          type = "PYTHON_MODULE",
          modulesDependencies = listOf(
            ModuleDependency(
              moduleName = "module2",
            ),
            ModuleDependency(
              moduleName = "module3",
            ),
          ),
          // todo - add libraries, what to do with sourcesJar and classesJar?
          librariesDependencies = listOf(),
        )

        val baseDirContentRoot = ContentRoot(
          url = URI.create("file:///root/dir/python_example/").toPath()
        )

        val sourcePath1 = URI.create("file:///root/dir/example/package/one").toPath()
        val sourcePath2 = URI.create("file:///root/dir/example/package/two").toPath()

        val sourceRoots = listOf(
          PythonSourceRoot(
            sourcePath = sourcePath1,
            generated = false,
            rootType = "python-source",
            targetId = BuildTargetIdentifier("target"),
          ),
          PythonSourceRoot(
            sourcePath = sourcePath2,
            generated = false,
            rootType = "python-source",
            targetId = BuildTargetIdentifier("target"),
          ),
        )

        val resourcePath1 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
        val resourcePath2 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()

        val resourceRoots = listOf(
          PythonResourceRoot(
            resourcePath = resourcePath1,
          ),
          PythonResourceRoot(
            resourcePath = resourcePath2,
          ),
        )

        val sdkInfo = PythonSdkInfo(version = "3", interpreter = Path("fake/path/to/interpreter"))

        val pythonModule = PythonModule(
          module = module,
          baseDirContentRoot = baseDirContentRoot,
          sourceRoots = sourceRoots,
          resourceRoots = resourceRoots,
          libraries = listOf(),
          sdkInfo = sdkInfo,
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
            dependencies = listOf(
              ModuleDependencyItem.Exportable.ModuleDependency(
                module = ModuleId("module2"),
                exported = true,
                // todo - why is it COMPILE?
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
                productionOnTest = true,
              ),
              ModuleDependencyItem.Exportable.ModuleDependency(
                module = ModuleId("module3"),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
                productionOnTest = true,
              ),
              ModuleDependencyItem.ModuleSourceDependency,
              ModuleDependencyItem.SdkDependency("3", "PythonSDK")
            )
          ) {
            type = "PYTHON_MODULE"
          }
        )

        returnedModuleEntity shouldBeEqual expectedModuleEntity

        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)

        val virtualSourceDir1 = sourcePath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonSourceRootEntity1 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir1,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir1,
            rootType = "python-source",
          ) {
            // todo - anything here to check for python?
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        val virtualSourceDir2 = sourcePath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonSourceRootEntity2 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir2,
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir2,
            rootType = "python-source"
          ) {
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )


        val virtualResourceUrl1 = resourcePath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonResourceRootEntity1 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl1,
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl1,
            rootType = "python-resource"
          ) {
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )
        val virtualResourceUrl2 = resourcePath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonResourceRootEntity2 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl2,
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl2,
            rootType = "python-resource"
          ) {
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedPythonSourceRootEntity1,
          expectedPythonSourceRootEntity2,
          expectedPythonResourceRootEntity1,
          expectedPythonResourceRootEntity2,
        )
      }
    }

    @Test
    fun `should add multiple python modules with sources to the workspace model`() {
      runTestForUpdaters(listOf(PythonModuleWithSourcesUpdater::class, PythonModuleUpdater::class)) { updater ->
        // given
        val module1 = Module(
          name = "module1",
          type = "PYTHON_MODULE",
          modulesDependencies = listOf(
            ModuleDependency(
              moduleName = "module2",
            ),
            ModuleDependency(
              moduleName = "module3",
            ),
          ),
          // todo - add libraries
          librariesDependencies = listOf(),
        )

        val baseDirContentRoot1 = ContentRoot(
          url = URI.create("file:///root/dir/example/").toPath()
        )

        val sourcePath11 = URI.create("file:///root/dir/example/package/one").toPath()
        val sourcePath12 = URI.create("file:///root/dir/example/package/two").toPath()
        val sourceRoots1 = listOf(
          PythonSourceRoot(
            sourcePath = sourcePath11,
            generated = false,
            rootType = "python-source",
            targetId = BuildTargetIdentifier("target"),
          ),
          PythonSourceRoot(
            sourcePath = sourcePath12,
            generated = false,
            rootType = "python-source",
            targetId = BuildTargetIdentifier("target"),
          ),
        )

        val resourcePath11 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
        val resourcePath12 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()
        val resourceRoots1 = listOf(
          PythonResourceRoot(
            resourcePath = resourcePath11,
          ),
          PythonResourceRoot(
            resourcePath = resourcePath12,
          ),
        )

        val sdkInfo1 = PythonSdkInfo(version = "3", interpreter = Path("fake/path/to/interpreter"))

        val pythonModule1 = PythonModule(
          module = module1,
          sourceRoots = sourceRoots1,
          resourceRoots = resourceRoots1,
          libraries = listOf(),
          baseDirContentRoot = baseDirContentRoot1,
          sdkInfo = sdkInfo1,
        )

        val module2 = Module(
          name = "module2",
          type = "PYTHON_MODULE",
          modulesDependencies = listOf(
            ModuleDependency(
              moduleName = "module3",
            ),
          ),
          librariesDependencies = listOf(),
        )

        val baseDirContentRoot2 = ContentRoot(
          url = URI.create("file:///another/root/dir/example/").toPath()
        )

        val sourcePath21 = URI.create("file:///another/root/dir/another/example/package/").toPath()
        val sourceRoots2 = listOf(
          PythonSourceRoot(
            sourcePath = sourcePath21,
            generated = false,
            rootType = "python-test",
            targetId = BuildTargetIdentifier("target"),
          ),
        )

        val resourcePath21 = URI.create("file:///another/root/dir/another/example/resource/File1.txt").toPath()
        val resourceRoots2 = listOf(
          PythonResourceRoot(
            resourcePath = resourcePath21,
          ),
        )

        val sdkInfo2 = PythonSdkInfo(version = "3", interpreter = Path("fake/path/to/interpreter"))

        val pythonModule2 = PythonModule(
          module = module2,
          baseDirContentRoot = baseDirContentRoot2,
          sourceRoots = sourceRoots2,
          resourceRoots = resourceRoots2,
          libraries = listOf(),
          sdkInfo = sdkInfo2,
        )

        val pythonModules = listOf(pythonModule1, pythonModule2)

        // when
        val returnedModuleEntries = runTestWriteAction {
          updater.addEntries(pythonModules)
        }

        // then
        val expectedModuleEntity1 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = DoNotSaveInDotIdeaDirEntitySource,
            dependencies = listOf(
              ModuleDependencyItem.Exportable.ModuleDependency(
                module = ModuleId("module2"),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
                productionOnTest = true,
              ),
              ModuleDependencyItem.Exportable.ModuleDependency(
                module = ModuleId("module3"),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
                productionOnTest = true,
              ),
              ModuleDependencyItem.SdkDependency("3", "PythonSDK"),
              ModuleDependencyItem.ModuleSourceDependency,
            )
          ) {
            type = "PYTHON_MODULE"
          }
        )

        val expectedModuleEntity2 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module2",
            entitySource = DoNotSaveInDotIdeaDirEntitySource,
            dependencies = listOf(
              ModuleDependencyItem.Exportable.ModuleDependency(
                module = ModuleId("module3"),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
                productionOnTest = true,
              ),
              ModuleDependencyItem.SdkDependency("3", "PythonSDK"),
              ModuleDependencyItem.ModuleSourceDependency,
            )
          ) {
            type = "PYTHON_MODULE"
          }
        )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries

        val virtualSourceDir11 = sourcePath11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonSourceRootEntity11 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir11,
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir11,
            rootType = "python-source"
          ) {},
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualSourceDir12 = sourcePath12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonSourceRootEntity12 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir12,
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir12,
            rootType = "python-source"
          ) {},
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualSourceDir21 = sourcePath21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonSourceRootEntity21 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualSourceDir21,
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualSourceDir21,
            rootType = "python-test"
          ) {},
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        val virtualResourceUrl11 = resourcePath11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonResourceRootEntity11 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl11,
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl11,
            rootType = "python-resource"
          ) {},
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualResourceUrl12 = resourcePath12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonResourceRootEntity12 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl12,
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl12,
            rootType = "python-resource"
          ) {},
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualResourceUrl21 = resourcePath21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonResourceRootEntity21 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualResourceUrl21,
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualResourceUrl21,
            rootType = "python-resource"
          ) {},
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedPythonSourceRootEntity11,
          expectedPythonSourceRootEntity12,
          expectedPythonSourceRootEntity21,
          expectedPythonResourceRootEntity11,
          expectedPythonResourceRootEntity12,
          expectedPythonResourceRootEntity21,
        )
      }
    }
  }


  @Nested
  @DisplayName("pythonModuleWithoutSourcesUpdater.addEntity(entityToAdd) tests")
  inner class PythonModuleWithoutSourcesUpdaterTest {
    @Test
    fun `should add one python module without sources to the workspace model`() {
      runTestForUpdaters(listOf(PythonModuleWithoutSourcesUpdater::class, PythonModuleUpdater::class)) { updater ->
        // given
        val module = Module(
          name = "module1",
          type = "PYTHON_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val baseDirContentRootPath = URI.create("file:///root/dir/").toPath()
        val baseDirContentRoot = ContentRoot(
          url = baseDirContentRootPath,
        )

        val pythonModule = PythonModule(
          module = module,
          baseDirContentRoot = baseDirContentRoot,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
          sdkInfo = null,
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
            dependencies = emptyList(),
          ) {
            type = "PYTHON_MODULE"
          }
        )

        returnedModuleEntity shouldBeEqual expectedModuleEntity
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)

        val virtualBaseDirContentRootPath = baseDirContentRootPath.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntity = ExpectedContentRootEntity(
          url = virtualBaseDirContentRootPath,
          excludedPatterns = emptyList(),
          excludedUrls = emptyList(),
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedContentRootEntity)
      }
    }

    @Test
    fun `should add multiple python modules without sources to the workspace model`() {
      runTestForUpdaters(listOf(PythonModuleWithoutSourcesUpdater::class, PythonModuleUpdater::class)) { updater ->
        // given
        val module1 = Module(
          name = "module1",
          type = "PYTHON_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val baseDirContentRootPath1 = URI.create("file:///root/dir1/").toPath()
        val baseDirContentRoot1 = ContentRoot(
          url = baseDirContentRootPath1,
        )

        val pythonModule1 = PythonModule(
          module = module1,
          baseDirContentRoot = baseDirContentRoot1,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
          sdkInfo = null,
        )

        val module2 = Module(
          name = "module2",
          type = "PYTHON_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val baseDirContentRootPath2 = URI.create("file:///root/dir2/").toPath()
        val baseDirContentRoot2 = ContentRoot(
          url = baseDirContentRootPath2,
        )

        val pythonModule2 = PythonModule(
          module = module2,
          baseDirContentRoot = baseDirContentRoot2,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
          sdkInfo = null,
        )

        val javaModules = listOf(pythonModule1, pythonModule2)

        // when
        val returnedModuleEntries = runTestWriteAction {
          updater.addEntries(javaModules)
        }

        // then
        val expectedModuleEntity1 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = DoNotSaveInDotIdeaDirEntitySource,
            dependencies = emptyList(),
          ) {
            type = "PYTHON_MODULE"
          }
        )
        val expectedModuleEntity2 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module2",
            entitySource = DoNotSaveInDotIdeaDirEntitySource,
            dependencies = emptyList(),
          ) {
            type = "PYTHON_MODULE"
          }
        )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries

        val virtualBaseDirContentRootPath1 = baseDirContentRootPath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntity1 = ExpectedContentRootEntity(
          url = virtualBaseDirContentRootPath1,
          excludedPatterns = emptyList(),
          excludedUrls = emptyList(),
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )

        val virtualBaseDirContentRootPath2 = baseDirContentRootPath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntity2 = ExpectedContentRootEntity(
          url = virtualBaseDirContentRootPath2,
          excludedPatterns = emptyList(),
          excludedUrls = emptyList(),
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedContentRootEntity1,
          expectedContentRootEntity2
        )
      }
    }
  }

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
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath)

    test(updaterConstructor.call(workspaceModelEntityUpdaterConfig))
  }
}

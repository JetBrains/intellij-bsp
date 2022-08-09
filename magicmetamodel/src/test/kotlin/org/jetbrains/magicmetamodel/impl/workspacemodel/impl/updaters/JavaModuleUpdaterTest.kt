@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.api.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.JavaResourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.JavaSourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.LibraryId
import com.intellij.workspaceModel.storage.bridgeEntities.api.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleDependencyItem
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleId
import com.intellij.workspaceModel.storage.bridgeEntities.api.SourceRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import org.jetbrains.workspace.model.matchers.entries.ExpectedContentRootEntity
import org.jetbrains.workspace.model.matchers.entries.ExpectedJavaResourceRootEntity
import org.jetbrains.workspace.model.matchers.entries.ExpectedJavaSourceRootEntity
import org.jetbrains.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

// TODO add libraries tests
internal class JavaModuleUpdaterTest : WorkspaceModelBaseTest() {

  @Nested
  @DisplayName("javaModuleWithSourcesUpdater.addEntity(entityToAdd) tests")
  inner class JavaModuleWithSourcesUpdaterTest {

    @Test
    fun `should add one java module with sources to the workspace model`() {
      runTestForUpdaters(listOf(JavaModuleWithSourcesUpdater::class, JavaModuleUpdater::class)) { updater ->
        // given
        val module = Module(
          name = "module1",
          type = "JAVA_MODULE",
          modulesDependencies = listOf(
            ModuleDependency(
              moduleName = "module2",
            ),
            ModuleDependency(
              moduleName = "module3",
            ),
          ),
          librariesDependencies = listOf(
            LibraryDependency(
              libraryName = "lib1",
            ),
            LibraryDependency(
              libraryName = "lib2",
            ),
          ),
        )

        val baseDirContentRoot = ContentRoot(
          url = URI.create("file:///root/dir/example/").toPath()
        )

        val sourceDir1 = URI.create("file:///root/dir/example/package/one").toPath()
        val sourcePackagePrefix1 = "example.package.one"
        val sourceDir2 = URI.create("file:///root/dir/example/package/two").toPath()
        val sourcePackagePrefix2 = "example.package.two"
        val sourceRoots = listOf(
          JavaSourceRoot(
            sourceDir = sourceDir1,
            generated = false,
            packagePrefix = sourcePackagePrefix1,
          ),
          JavaSourceRoot(
            sourceDir = sourceDir2,
            generated = false,
            packagePrefix = sourcePackagePrefix2,
          ),
        )

        val resourcePath1 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
        val resourcePath2 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()
        val resourceRoots = listOf(
          JavaResourceRoot(
            resourcePath = resourcePath1,
          ),
          JavaResourceRoot(
            resourcePath = resourcePath2,
          ),
        )

        val libraries = listOf(
          Library(
            displayName = "lib1",
            sourcesJar = "jar:///lib1/1.0.0/lib1-1.0.0-sources.jar!/",
            classesJar = "jar:///lib1/1.0.0/lib1.jar!/",
          ),
          Library(
            displayName = "lib2",
            sourcesJar = "jar:///lib2/1.0.0/lib2-2.0.0-sources.jar!/",
            classesJar = "jar:///lib2/1.0.0/lib2-2.0.0.jar!/",
          ),
        )

        val javaModule = JavaModule(
          module = module,
          baseDirContentRoot = baseDirContentRoot,
          sourceRoots = sourceRoots,
          resourceRoots = resourceRoots,
          libraries = libraries,
        )

        // when
        val returnedModuleEntity = runTestWriteAction {
          updater.addEntity(javaModule)
        }

        // then
        val expectedModuleEntity = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = projectConfigSource,
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
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib1",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
                ),
                exported = false,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib2",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
                ),
                exported = false,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
              ModuleDependencyItem.ModuleSourceDependency,
            )
          ) {
            type = "JAVA_MODULE"
          }
        )

        returnedModuleEntity shouldBeEqual expectedModuleEntity
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)

        val virtualSourceDir1 = sourceDir1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity1 = ExpectedJavaSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir1,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir1,
            rootType = "java-source",
          ),
          javaSourceRootEntity = JavaSourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            generated = false,
            packagePrefix = sourcePackagePrefix1
          ),
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )
        val virtualSourceDir2 = sourceDir2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity2 = ExpectedJavaSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir2,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir2,
            rootType = "java-source"
          ),
          javaSourceRootEntity = JavaSourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            generated = false,
            packagePrefix = sourcePackagePrefix2
          ),
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        loadedEntries(JavaSourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedJavaSourceRootEntity1,
          expectedJavaSourceRootEntity2
        )

        val virtualResourceUrl1 = resourcePath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity1 = ExpectedJavaResourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl1,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl1,
            rootType = "java-resource"
          ),
          javaResourceRootEntity = JavaResourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            generated = false,
            relativeOutputPath = ""
          ),
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )
        val virtualResourceUrl2 = resourcePath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity2 = ExpectedJavaResourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl2,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl2,
            rootType = "java-resource"
          ),
          javaResourceRootEntity = JavaResourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            generated = false,
            relativeOutputPath = ""
          ),
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        loadedEntries(JavaResourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedJavaResourceRootEntity1,
          expectedJavaResourceRootEntity2
        )
      }
    }

    @Test
    fun `should add multiple java module with sources to the workspace model`() {
      runTestForUpdaters(listOf(JavaModuleWithSourcesUpdater::class, JavaModuleUpdater::class)) { updater ->
        // given
        val module1 = Module(
          name = "module1",
          type = "JAVA_MODULE",
          modulesDependencies = listOf(
            ModuleDependency(
              moduleName = "module2",
            ),
            ModuleDependency(
              moduleName = "module3",
            ),
          ),
          librariesDependencies = listOf(
            LibraryDependency(
              libraryName = "lib1",
            ),
            LibraryDependency(
              libraryName = "lib2",
            ),
          ),
        )

        val baseDirContentRoot1 = ContentRoot(
          url = URI.create("file:///root/dir/example/").toPath()
        )

        val sourceDir11 = URI.create("file:///root/dir/example/package/one").toPath()
        val sourcePackagePrefix11 = "example.package.one"
        val sourceDir12 = URI.create("file:///root/dir/example/package/two").toPath()
        val sourcePackagePrefix12 = "example.package.two"
        val sourceRoots1 = listOf(
          JavaSourceRoot(
            sourceDir = sourceDir11,
            generated = false,
            packagePrefix = sourcePackagePrefix11,
          ),
          JavaSourceRoot(
            sourceDir = sourceDir12,
            generated = false,
            packagePrefix = sourcePackagePrefix12,
          ),
        )

        val resourcePath11 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
        val resourcePath12 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()
        val resourceRoots1 = listOf(
          JavaResourceRoot(
            resourcePath = resourcePath11,
          ),
          JavaResourceRoot(
            resourcePath = resourcePath12,
          ),
        )

        val libraries1 = listOf(
          Library(
            displayName = "lib1",
            sourcesJar = "jar:///lib1/1.0.0/lib1-1.0.0-sources.jar!/",
            classesJar = "jar:///lib1/1.0.0/lib1-1.0.0.jar!/",
          ),
          Library(
            displayName = "lib2",
            sourcesJar = "jar:///lib2/2.0.0/lib2-2.0.0-sources.jar!/",
            classesJar = "jar:///lib2/1.0.0/lib2-2.0.0.jar!/",
          ),
        )

        val javaModule1 = JavaModule(
          module = module1,
          sourceRoots = sourceRoots1,
          resourceRoots = resourceRoots1,
          libraries = libraries1,
          baseDirContentRoot = baseDirContentRoot1,
        )

        val module2 = Module(
          name = "module2",
          type = "JAVA_MODULE",
          modulesDependencies = listOf(
            ModuleDependency(
              moduleName = "module3",
            ),
          ),
          librariesDependencies = listOf(
            LibraryDependency(
              libraryName = "lib1",
            ),
          ),
        )

        val baseDirContentRoot2 = ContentRoot(
          url = URI.create("file:///another/root/dir/example/").toPath()
        )

        val sourceDir21 = URI.create("file:///another/root/dir/another/example/package/").toPath()
        val sourcePackagePrefix21 = "another.example.package"
        val sourceRoots2 = listOf(
          JavaSourceRoot(
            sourceDir = sourceDir21,
            generated = false,
            packagePrefix = sourcePackagePrefix21,
          ),
        )

        val resourcePath21 = URI.create("file:///another/root/dir/another/example/resource/File1.txt").toPath()
        val resourceRoots2 = listOf(
          JavaResourceRoot(
            resourcePath = resourcePath21,
          ),
        )

        val libraries2 = listOf(
          Library(
            displayName = "lib1",
            sourcesJar = "jar:///lib1/1.0.0/lib1-1.0.0-sources.jar!/",
            classesJar = "jar:///lib1/1.0.0/lib1-1.0.0.jar!/",
          ),
        )

        val javaModule2 = JavaModule(
          module = module2,
          baseDirContentRoot = baseDirContentRoot2,
          sourceRoots = sourceRoots2,
          resourceRoots = resourceRoots2,
          libraries = libraries2,
        )

        val javaModules = listOf(javaModule1, javaModule2)

        // when
        val returnedModuleEntries = runTestWriteAction {
          updater.addEntries(javaModules)
        }

        // then
        val expectedModuleEntity1 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = projectConfigSource,
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
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib1",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
                ),
                exported = false,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib2",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
                ),
                exported = false,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
              ModuleDependencyItem.ModuleSourceDependency,
            )
          ) {
            type = "JAVA_MODULE"
          }
        )

        val expectedModuleEntity2 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module2",
            entitySource = projectConfigSource,
            dependencies = listOf(
              ModuleDependencyItem.Exportable.ModuleDependency(
                module = ModuleId("module3"),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
                productionOnTest = true,
              ),
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib1",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module2")),
                ),
                exported = false,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
              ModuleDependencyItem.ModuleSourceDependency,
            )
          ) {
            type = "JAVA_MODULE"
          }
        )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries

        val virtualSourceDir11 = sourceDir11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity11 = ExpectedJavaSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir11,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir11,
            rootType = "java-source"
          ),
          javaSourceRootEntity = JavaSourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            generated = false,
            packagePrefix = sourcePackagePrefix11
          ),
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualSourceDir12 = sourceDir12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity12 = ExpectedJavaSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir12,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir12,
            rootType = "java-source"
          ),
          javaSourceRootEntity = JavaSourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            generated = false,
            packagePrefix = sourcePackagePrefix12
          ),
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualSourceDir21 = sourceDir21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity21 = ExpectedJavaSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualSourceDir21,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualSourceDir21,
            rootType = "java-source"
          ),
          javaSourceRootEntity = JavaSourceRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            generated = false,
            packagePrefix = sourcePackagePrefix21
          ),
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        loadedEntries(JavaSourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedJavaSourceRootEntity11,
          expectedJavaSourceRootEntity12,
          expectedJavaSourceRootEntity21
        )

        val virtualResourceUrl11 = resourcePath11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity11 = ExpectedJavaResourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl11,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl11,
            rootType = "java-resource"
          ),
          javaResourceRootEntity = JavaResourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            generated = false,
            relativeOutputPath = ""
          ),
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualResourceUrl12 = resourcePath12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity12 = ExpectedJavaResourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl12,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl12,
            rootType = "java-resource"
          ),
          javaResourceRootEntity = JavaResourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            generated = false,
            relativeOutputPath = ""
          ),
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualResourceUrl21 = resourcePath21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity21 = ExpectedJavaResourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualResourceUrl21,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualResourceUrl21,
            rootType = "java-resource"
          ),
          javaResourceRootEntity = JavaResourceRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            generated = false,
            relativeOutputPath = ""
          ),
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        loadedEntries(JavaResourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedJavaResourceRootEntity11,
          expectedJavaResourceRootEntity12,
          expectedJavaResourceRootEntity21
        )
      }
    }
  }

  @Nested
  @DisplayName("javaModuleWithoutSourcesUpdater.addEntity(entityToAdd) tests")
  inner class JavaModuleWithoutSourcesUpdaterTest {

    @Test
    fun `should add one java module without sources to the workspace model`() {
      runTestForUpdaters(listOf(JavaModuleWithoutSourcesUpdater::class, JavaModuleUpdater::class)) { updater ->
        // given
        val module = Module(
          name = "module1",
          type = "JAVA_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val baseDirContentRootPath = URI.create("file:///root/dir/").toPath()
        val baseDirContentRoot = ContentRoot(
          url = baseDirContentRootPath,
        )

        val javaModule = JavaModule(
          module = module,
          baseDirContentRoot = baseDirContentRoot,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
        )

        // when
        val returnedModuleEntity = runTestWriteAction {
          updater.addEntity(javaModule)
        }

        // then
        val expectedModuleEntity = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = projectConfigSource,
            dependencies = emptyList(),
          ) {
            type = "JAVA_MODULE"
          }
        )

        returnedModuleEntity shouldBeEqual expectedModuleEntity
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)

        val virtualBaseDirContentRootPath = baseDirContentRootPath.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntity = ExpectedContentRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualBaseDirContentRootPath,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedContentRootEntity)
      }
    }

    @Test
    fun `should add multiple java module without sources to the workspace model`() {
      runTestForUpdaters(listOf(JavaModuleWithoutSourcesUpdater::class, JavaModuleUpdater::class)) { updater ->
        // given
        val module1 = Module(
          name = "module1",
          type = "JAVA_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val baseDirContentRootPath1 = URI.create("file:///root/dir1/").toPath()
        val baseDirContentRoot1 = ContentRoot(
          url = baseDirContentRootPath1,
        )

        val javaModule1 = JavaModule(
          module = module1,
          baseDirContentRoot = baseDirContentRoot1,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
        )

        val module2 = Module(
          name = "module2",
          type = "JAVA_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val baseDirContentRootPath2 = URI.create("file:///root/dir2/").toPath()
        val baseDirContentRoot2 = ContentRoot(
          url = baseDirContentRootPath2,
        )

        val javaModule2 = JavaModule(
          module = module2,
          baseDirContentRoot = baseDirContentRoot2,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
        )

        val javaModules = listOf(javaModule1, javaModule2)

        // when
        val returnedModuleEntries = runTestWriteAction {
          updater.addEntries(javaModules)
        }

        // then
        val expectedModuleEntity1 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = projectConfigSource,
            dependencies = emptyList(),
          ) {
            type = "JAVA_MODULE"
          }
        )
        val expectedModuleEntity2 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module2",
            entitySource = projectConfigSource,
            dependencies = emptyList(),
          ) {
            type = "JAVA_MODULE"
          }
        )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries

        val virtualBaseDirContentRootPath1 = baseDirContentRootPath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntity1 = ExpectedContentRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualBaseDirContentRootPath1,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )

        val virtualBaseDirContentRootPath2 = baseDirContentRootPath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntity2 = ExpectedContentRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualBaseDirContentRootPath2,
            excludedUrls = emptyList(),
            excludedPatterns = emptyList()
          ),
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
    updaters: List<KClass<out WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>>>,
    test: (WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>) -> Unit,
  ) =
    updaters
      .map { it.primaryConstructor!! }
      .forEach { runTest(it, test) }

  private fun runTest(
    updaterConstructor: KFunction<WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>>,
    test: (WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>) -> Unit,
  ) {
    beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectConfigSource)

    test(updaterConstructor.call(workspaceModelEntityUpdaterConfig))
  }
}

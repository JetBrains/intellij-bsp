package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.jetbrains.bsp.bsp4kt.BuildTarget
import com.jetbrains.bsp.bsp4kt.BuildTargetCapabilities
import com.jetbrains.bsp.bsp4kt.BuildTargetIdentifier
import com.jetbrains.bsp.bsp4kt.SourceItem
import com.jetbrains.bsp.bsp4kt.SourceItemKind
import com.jetbrains.bsp.bsp4kt.SourcesItem
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaSourceRoot
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.toPath

@DisplayName("SourcesItemToWorkspaceModelJavaSourceRootTransformer.transform(sourcesItem)")
class SourcesItemToJavaSourceRootTransformerTest {

  private val projectBasePath = Path("")
  private val projectBasePathURIStr = projectBasePath.toUri().toString()

  private val sourcesItemToJavaSourceRootTransformer = SourcesItemToJavaSourceRootTransformer(projectBasePath)

  @Test
  fun `should return no sources roots for no sources items`() {
    // given
    val emptySources = listOf<BuildTargetAndSourceItem>()

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(emptySources)

    // then
    javaSources shouldBe emptyList()
  }

  @Test
  fun `should return single source root for sources item with one file source`() {
    // given
    val rootDir = "${projectBasePathURIStr}root/dir"
    val sourceItem = SourceItem(
      "$rootDir/example/package/File.java",
      SourceItemKind.File,
      false
    )
    val sourceRoots = listOf(rootDir)

    val buildTargetAndSourceItem = BuildTargetAndSourceItem(
      buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        null, 
        null,
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(),
      ),
      sourcesItem = SourcesItem(
        BuildTargetIdentifier("target"),
        listOf(sourceItem),
        sourceRoots
      )
    )

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItem)

    // then
    val expectedJavaSourceRoot = JavaSourceRoot(
      sourcePath = URI.create("$rootDir/example/package/File.java").toPath(),
      generated = false,
      packagePrefix = "example.package",
      rootType = "java-source",
    )

    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot)
  }

  @Test
  fun `should return single source test root for sources item with one file source`() {
    // given
    val rootDir = "${projectBasePathURIStr}root/dir"
    val sourceItem = SourceItem(
      "$rootDir/example/package/File.java",
      SourceItemKind.File,
      false
    )
    val sourceRoots = listOf(rootDir)

    val buildTargetAndSourceItem = BuildTargetAndSourceItem(
      buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        null,
        null,
        listOf("test"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(),
      ),
      sourcesItem = SourcesItem(
        BuildTargetIdentifier("target"),
        listOf(sourceItem)
      , sourceRoots)
    )

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItem)

    // then
    val expectedJavaSourceRoot = JavaSourceRoot(
      sourcePath = URI.create("$rootDir/example/package/File.java").toPath(),
      generated = false,
      packagePrefix = "example.package",
      rootType = "java-test",
    )

    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot)
  }

  @Test
  fun `should return single source root for sources item with one dir source`() {
    // given
    val rootDir = "${projectBasePathURIStr}root/dir"
    val sourceItem = SourceItem(
      "$rootDir/example/package/",
      SourceItemKind.Directory,
      false
    )
    val sourceRoots = listOf(rootDir)

    val buildTargetAndSourceItem = BuildTargetAndSourceItem(
      buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        null,
        null,
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(),
      ),
      sourcesItem = SourcesItem(
        BuildTargetIdentifier("target"),
        listOf(sourceItem)
      , sourceRoots)
    )

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItem)

    // then
    val expectedJavaSourceRoot = JavaSourceRoot(
      sourcePath = URI.create("$rootDir/example/package/").toPath(),
      generated = false,
      packagePrefix = "example.package",
      rootType = "java-source",
    )

    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot)
  }

  @Test
  fun `should return sources roots for sources item with multiple sources`() {
    // given
    val rootDir = "${projectBasePathURIStr}root/dir"
    val anotherRootDir = "${projectBasePathURIStr}another/root/dir"

    val sourceItem1 = SourceItem(
      "$rootDir/example/package/File1.java",
      SourceItemKind.File,
      false
    )
    val sourceItem2 = SourceItem(
      "$rootDir/example/package/File2.java",
      SourceItemKind.File,
      false
    )
    val sourceItem3 = SourceItem(
      "$anotherRootDir/another/example/package/",
      SourceItemKind.Directory,
      false
    )
    val sourceRoots = listOf(
      rootDir,
      anotherRootDir,
    )

    val buildTargetAndSourceItem = BuildTargetAndSourceItem(
      buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        null,
        null,
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(),
      ),
      sourcesItem = SourcesItem(
        BuildTargetIdentifier("target"),
        listOf(sourceItem1, sourceItem2, sourceItem3)
      , sourceRoots)
    )

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItem)

    // then
    val expectedJavaSourceRoot1 = JavaSourceRoot(
      sourcePath = URI.create("$rootDir/example/package/File1.java").toPath(),
      generated = false,
      packagePrefix = "example.package",
      rootType = "java-source",
    )
    val expectedJavaSourceRoot2 = JavaSourceRoot(
      sourcePath = URI.create("$rootDir/example/package/File2.java").toPath(),
      generated = false,
      packagePrefix = "example.package",
      rootType = "java-source",
    )
    val expectedJavaSourceRoot3 = JavaSourceRoot(
      sourcePath = URI.create("$anotherRootDir/another/example/package/").toPath(),
      generated = false,
      packagePrefix = "another.example.package",
      rootType = "java-source",
    )
    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2, expectedJavaSourceRoot3)
  }

  @Test
  fun `should return sources roots for multiple sources items`() {
    // given
    val rootDir = "${projectBasePathURIStr}root/dir"
    val anotherRootDir = "${projectBasePathURIStr}another/root/dir"

    val sourceItem1 = SourceItem(
      "$rootDir/example/package/File1.java",
      SourceItemKind.File,
      false
    )
    val sourceItem2 = SourceItem(
      "$rootDir/example/package/File2.java",
      SourceItemKind.File,
      false
    )
    val sourceItem3 = SourceItem(
      "$anotherRootDir/another/example/package/",
      SourceItemKind.Directory,
      false
    )
    val sourceRoots = listOf(
      rootDir,
      anotherRootDir,
    )

    val buildTargetAndSourceItem1 = BuildTargetAndSourceItem(
      buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        null,
        null,
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(),
      ),
      sourcesItem = SourcesItem(
        BuildTargetIdentifier("target"),
        listOf(sourceItem1)
      , sourceRoots)
    )
    val buildTargetAndSourceItem2 = BuildTargetAndSourceItem(
      buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        null,
        null,
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(),
      ),
      sourcesItem = SourcesItem(
        BuildTargetIdentifier("target"),
        listOf(sourceItem2, sourceItem3)
      , sourceRoots)
    )

    val buildTargetAndSourceItems = listOf(buildTargetAndSourceItem1, buildTargetAndSourceItem2)

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItems)

    // then
    val expectedJavaSourceRoot1 = JavaSourceRoot(
      sourcePath = URI.create("$rootDir/example/package/File1.java").toPath(),
      generated = false,
      packagePrefix = "example.package",
      rootType = "java-source",
    )
    val expectedJavaSourceRoot2 = JavaSourceRoot(
      sourcePath = URI.create("$rootDir/example/package/File2.java").toPath(),
      generated = false,
      packagePrefix = "example.package",
      rootType = "java-source",
    )
    val expectedJavaSourceRoot3 = JavaSourceRoot(
      sourcePath = URI.create("$anotherRootDir/another/example/package/").toPath(),
      generated = false,
      packagePrefix = "another.example.package",
      rootType = "java-source",
    )
    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2, expectedJavaSourceRoot3)
  }

  @Test
  fun `should return sources root if source items are not in project base path`() {
    // given
    val rootDir = "file:///var/tmp/root/dir"
    val anotherRootDir = "file:///var/tmp/another/root/dir"

    val sourceItem1 = SourceItem(
      "$rootDir/example/package/File1.java",
      SourceItemKind.File,
      false
    )
    val sourceItem2 = SourceItem(
      "$rootDir/example/package/File2.java",
      SourceItemKind.File,
      false
    )
    val sourceItem3 = SourceItem(
      "$anotherRootDir/another/example/package/",
      SourceItemKind.Directory,
      false
    )
    val sourceRoots = listOf(
      rootDir,
      anotherRootDir,
    )

    val buildTargetAndSourceItem1 = BuildTargetAndSourceItem(
      buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        null,
        null,
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(),
      ),
      sourcesItem = SourcesItem(
        BuildTargetIdentifier("target"),
        listOf(sourceItem1)
      , sourceRoots)
    )
    val buildTargetAndSourceItem2 = BuildTargetAndSourceItem(
      buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        null,
        null,
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(),
      ),
      sourcesItem = SourcesItem(
        BuildTargetIdentifier("target"),
        listOf(sourceItem2, sourceItem3)
      , sourceRoots)
    )

    val buildTargetAndSourceItems = listOf(buildTargetAndSourceItem1, buildTargetAndSourceItem2)

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItems)

    // then
    javaSources shouldBe emptyList()
  }

  @Test
  fun `should return only source roots that have source items in project base path`() {
    // given
    val rootDir = "${projectBasePathURIStr}root/dir"
    val anotherRootDir = "file:///var/tmp/another/root/dir"

    val sourceItem1 = SourceItem(
      "$rootDir/example/package/File1.java",
      SourceItemKind.File,
      false
    )
    val sourceItem2 = SourceItem(
      "$anotherRootDir/example/package/File2.java",
      SourceItemKind.File,
      false
    )
    val sourceItem3 = SourceItem(
      "$rootDir/another/example/package/",
      SourceItemKind.Directory,
      false
    )
    val sourceRoots = listOf(
      rootDir,
      anotherRootDir,
    )

    val buildTargetAndSourceItem1 = BuildTargetAndSourceItem(
      buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        null,
        null,
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(),
      ),
      sourcesItem = SourcesItem(
        BuildTargetIdentifier("target"),
        listOf(sourceItem1)
      , sourceRoots)
    )
    val buildTargetAndSourceItem2 = BuildTargetAndSourceItem(
      buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        null,
        null,
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(),
      ),
      sourcesItem = SourcesItem(
        BuildTargetIdentifier("target"),
        listOf(sourceItem2, sourceItem3)
      , sourceRoots)
    )

    val buildTargetAndSourceItems = listOf(buildTargetAndSourceItem1, buildTargetAndSourceItem2)

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItems)

    // then
    val expectedJavaSourceRoot1 = JavaSourceRoot(
      sourcePath = URI.create("$rootDir/example/package/File1.java").toPath(),
      generated = false,
      packagePrefix = "example.package",
      rootType = "java-source",
    )
    val expectedJavaSourceRoot2 = JavaSourceRoot(
      sourcePath = URI.create("$rootDir/another/example/package/").toPath(),
      generated = false,
      packagePrefix = "another.example.package",
      rootType = "java-source",
    )
    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2)
  }
}

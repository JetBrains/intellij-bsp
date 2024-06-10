package org.jetbrains.plugins.bsp.projectStructure

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AllProjectStructuresProvider tests")
class AllProjectStructuresProviderTest : MockProjectBaseTest() {
  @Nested
  @DisplayName("AllProjectStructuresProvider.newDiff() tests")
  inner class NewDiff {
    @Test
    fun `should diff with all registered diffs and updaters`() {
      // given
      val diff1 = TestableProjectStructureDiff()
      val provider1 = TestableProjectStructureProvider(diff1)
      ProjectStructureProvider.ep.point.registerExtension(provider1, projectModel.disposableRule.disposable)

      val diff2 = AnotherTestableProjectStructureDiff()
      val provider2 = TestableProjectStructureProvider(diff2)
      ProjectStructureProvider.ep.point.registerExtension(provider2, projectModel.disposableRule.disposable)

      val updater1 = TestableProjectStructureUpdater(emptyList())
      ProjectStructureUpdater.ep.point.registerExtension(updater1, projectModel.disposableRule.disposable)

      val allProjectStructuresProvider = AllProjectStructuresProvider(project)

      val targetId = BuildTargetIdentifier("target")
      val targetInfo = targetId.toMockBuildTargetInfo()

      // then
      val allProjectStructuresDiff = allProjectStructuresProvider.newDiff()

      allProjectStructuresDiff.addTarget(targetInfo)

      runBlocking {
        allProjectStructuresDiff.applyAll()
      }

      // when
      diff1.hasBeenApplied shouldBe true
      diff1.updates shouldContainExactlyInAnyOrder listOf(targetId)
      diff2.hasBeenApplied shouldBe true
      diff2.updates shouldBe emptyList()
    }
  }
}

package org.jetbrains.plugins.bsp.projectStructure.workspaceModel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PythonProjectStructureUpdater tests")
class PythonProjectStructureUpdaterTest : MockProjectBaseTest() {
  private lateinit var updater: PythonProjectStructureUpdater

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    updater = PythonProjectStructureUpdater()
  }

  @Nested
  @DisplayName("PythonProjectStructureUpdater.isSupported(buildTarget) tests")
  inner class IsSupported {
    @Test
    fun `should return false if target doesnt contain python language`() {
      // given
      val buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        emptyList(),
        listOf("java", "kotlin", "not python"),
        emptyList(),
        BuildTargetCapabilities(),
      )

      // when & then
      updater.isSupported(buildTarget) shouldBe false
    }

    @Test
    fun `should return true if target contains python language`() {
      // given
      val buildTarget = BuildTarget(
        BuildTargetIdentifier("target"),
        emptyList(),
        listOf("java", "kotlin", "python"),
        emptyList(),
        BuildTargetCapabilities(),
      )

      // when & then
      updater.isSupported(buildTarget) shouldBe true
    }
  }

  @Nested
  @DisplayName("PythonProjectStructureUpdater.addTarget(targetInfo) tests")
  inner class AddTarget {

  }
}

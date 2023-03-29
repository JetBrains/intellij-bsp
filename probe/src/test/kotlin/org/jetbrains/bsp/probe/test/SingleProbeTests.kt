package org.jetbrains.bsp.probe.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.virtuslab.ideprobe.ProbeDriver
import org.virtuslab.ideprobe.robot.RobotProbeDriver
import scala.Option
import scala.runtime.BoxedUnit

class SingleProbeTests {

  companion object {
    const val LATEST_VERSION = "231.7665.28"
  }

  @Test
  fun openFreshInstanceOfBazelBspProjectAndCheckImportedTargets() {
    with(IdeProbeTestRunner()) {
      val fixture = fixtureWithWorkspaceFromGit(
        "https://github.com/JetBrains/bazel-bsp.git",
        "2.3.0"
      ).withBuild(LATEST_VERSION)
      runAfterOpen(fixture, Option.apply(null)) { probe, robot ->
        val stripeButton = robot.findElement(query.className("StripeButton", "text" to "BSP"))
        stripeButton.doClick()
        val buildPanel = robot.findElement(query.className("BspToolWindowPanel"))
        val loaded =
          buildPanel.findElement(query.className("ActionButton", "myaction.key" to "widget.loaded.targets.tab.name"))
        loaded.click()
        val targetsTree = buildPanel.findElement(query.className("Tree"))
        Assertions.assertEquals(9, targetsTree.fullTexts().size)
        robot.assertNoProblems(probe)
        BoxedUnit.UNIT
      }
    }
  }

  @Test
  fun openFreshInstanceOfBazelProjectAndCheckBuildConsoleOutputForErrors() {
    with(IdeProbeTestRunner()) {
      val fixture = fixtureWithWorkspaceFromGit(
        "https://github.com/bazelbuild/bazel.git", "6.0.0"
      ).withBuild(LATEST_VERSION)
      runAfterOpen(fixture, Option.apply(null)) { probe, robot ->
        robot.assertNoProblems(probe)
        BoxedUnit.UNIT
      }
    }
  }

  private fun RobotProbeDriver.assertNoProblems(probe: ProbeDriver) {
    findElement(query.className("StripeButton", "text" to "Problems")).doClick()
    val errors = findElement(query.className("ContentTabLabel", "visible_text" to "Project Errors"))
    var problemsText = ""
    probe.await(tryUntilSuccessful {
      errors.fixture().click()
      val problemsTree = findElement(query.className("ProblemsViewPanel"))
        .findElement(query.className("Tree"))
      problemsText = problemsTree.fullText().split("\n").firstOrNull().orEmpty()
    })
    Assertions.assertEquals("No errors found by the IDE", problemsText)
  }
}
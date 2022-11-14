package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.AbstractActionWithTarget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.BuildTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.DebugWithLocalJvmRunnerAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RunTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RunWithLocalJvmRunnerAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.TestTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetContainer
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

public class LoadedTargetsMouseListener(
  private val container: BuildTargetContainer,
  private val project: Project,
) : MouseListener {

  override fun mouseClicked(e: MouseEvent?) {
    e?.let { mouseClickedNotNull(it) }
  }

  private fun mouseClickedNotNull(mouseEvent: MouseEvent) {
    if (mouseEvent.mouseButton == MouseButton.Right) {
      showPopup(mouseEvent)
    }
  }

  private fun showPopup(mouseEvent: MouseEvent) {
    val actionGroup = calculatePopupGroup()
    if (actionGroup != null) {
      val context = DataManager.getInstance().getDataContext(mouseEvent.component)
      val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
      JBPopupFactory.getInstance()
        .createActionGroupPopup(null, actionGroup, context, mnemonics, true)
        .showInBestPositionFor(context)
    }
  }

  private fun calculatePopupGroup(): ActionGroup? {
    val target = container.getSelectedBuildTarget()
    val isConnected = BspConnectionService.getInstance(project).value?.isConnected() == true

    return if (target != null && isConnected) {
      val actions = mutableListOf<AnAction>()
      if (target.capabilities.canCompile) {
        actions.addAction(BuildTargetAction::class.java)
      }
      if (target.capabilities.canRun) {
        actions.addAction(RunTargetAction::class.java)
        if (target.isJvmTarget()) {
          actions.addAction(RunWithLocalJvmRunnerAction::class.java)
          actions.addAction(DebugWithLocalJvmRunnerAction::class.java)
        }
      }
      if (target.capabilities.canTest) {
        actions.addAction(TestTargetAction::class.java)
      }
      DefaultActionGroup().also { it.addAll(actions) }
    } else null
  }

  private fun MutableList<AnAction>.addAction(
     actionClass : Class<out AbstractActionWithTarget>
  ) : AbstractActionWithTarget =
    actions.getOrPut(actionClass) {
      actionClass.constructors.first { it.parameterCount == 0 }.newInstance() as AbstractActionWithTarget
    }.also {
      it.target = container.getSelectedBuildTarget()?.id
      add(it)
    }

  private fun BuildTarget.isJvmTarget(): Boolean = dataKind == "jvm"

  override fun mousePressed(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseReleased(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseEntered(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseExited(e: MouseEvent?) { /* nothing to do */ }

  private companion object {
    val actions = HashMap<Class<out AbstractActionWithTarget>, AbstractActionWithTarget>()
  }
}

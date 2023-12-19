package org.jetbrains.plugins.bsp.jewel.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.StickyTargetAction

// `CURRENTLY UNUSED`
// Almost the same as in the Swing implementation of the UI
internal fun swingToolbar(
  panelState: ToolbarTab,
  setPanelState: (ToolbarTab) -> Unit,
): ActionToolbar {
  val actionManager = ActionManager.getInstance()
  val actionGroup = actionManager
    .getAction("Bsp.ActionsToolbar") as DefaultActionGroup

  val notLoadedTargetsActionName = BspPluginBundle.message("widget.not.loaded.targets.tab.name")
  val loadedTargetsActionName = BspPluginBundle.message("widget.loaded.targets.tab.name")

  actionGroup.childActionsOrStubs.iterator().forEach {
    if (it.shouldBeDisposedAfterReload()) {
      actionGroup.remove(it)
    }
  }
  actionGroup.addSeparator()
  actionGroup.add(StickyTargetAction(
    hintText = notLoadedTargetsActionName,
    icon = BspPluginIcons.unloadedTargetsFilterIcon,
    onPerform = { setPanelState(ToolbarTab.NOT_LOADED_TARGETS) },
    selectionProvider = { panelState == ToolbarTab.NOT_LOADED_TARGETS },
  ))
  actionGroup.add(StickyTargetAction(
    hintText = loadedTargetsActionName,
    icon = BspPluginIcons.loadedTargetsFilterIcon,
    onPerform = { setPanelState(ToolbarTab.LOADED_TARGETS) },
    selectionProvider = { panelState == ToolbarTab.LOADED_TARGETS },
  ))

  actionGroup.addSeparator()

  // Filtering not implemented and therefore just icon is added:
//  actionGroup.add(FilterActionGroup(listsUpdater.targetFilter))
  actionGroup.add(DummyFilterAction())

  return actionManager.createActionToolbar("Bsp Toolbar", actionGroup, true)
}

private fun AnAction.shouldBeDisposedAfterReload(): Boolean {
  val notLoadedTargetsActionName = BspPluginBundle.message("widget.not.loaded.targets.tab.name")
  val loadedTargetsActionName = BspPluginBundle.message("widget.loaded.targets.tab.name")
  val filterAction = BspPluginBundle.message("widget.filter.action.group")
  return this.templateText == notLoadedTargetsActionName ||
    this.templateText == loadedTargetsActionName || this.templateText == filterAction
}

private class DummyFilterAction : AnAction(
  { BspPluginBundle.message("widget.filter.action.group") },
  AllIcons.General.Filter
) {
  override fun actionPerformed(e: AnActionEvent) {}
}

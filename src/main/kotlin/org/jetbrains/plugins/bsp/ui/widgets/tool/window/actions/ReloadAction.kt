package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.project.stateStore
import org.jetbrains.plugins.bsp.services.*
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class ReloadAction : AnAction(BspAllTargetsWidgetBundle.message("reload.action.text")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!
    val connectionService = project.getService(BspConnectionService::class.java)
    val bspConsoleService = BspConsoleService.getInstance(project)

    val bspResolver =
      VeryTemporaryBspResolver(project.stateStore.projectBasePath, connectionService.server!!, bspConsoleService.bspSyncConsole, bspConsoleService.bspBuildConsole)

    runBackgroundableTask("Reload action", project) {
      bspResolver.collectModel()
    }
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project
    val connectionService = project?.getService(BspConnectionService::class.java)
    e.presentation.isEnabled = connectionService?.isRunning() == true
  }
}

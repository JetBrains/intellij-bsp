package org.jetbrains.plugins.bsp.ui.widgets.toolwindow.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.project.stateStore
import java.util.concurrent.TimeUnit
import javax.swing.Icon
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.plugins.bsp.services.BspSyncConsoleService
import org.jetbrains.plugins.bsp.services.BspUtilService
import org.jetbrains.plugins.bsp.services.VeryTemporaryBspResolver

public class RestartAction(actionName: String, icon: Icon) : AnAction({actionName}, icon) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!
    val bspConnectionService = project.getService(BspConnectionService::class.java)
    bspConnectionService.disconnect()

    Runtime.getRuntime().exec(
      BspUtilService.coursierInstallCommand,
      emptyArray(),
      project.stateStore.projectBasePath.toFile()
    ).waitFor(15, TimeUnit.SECONDS)

    bspConnectionService.reconnect(project.locationHash)

    val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)
    val bspResolver =
      VeryTemporaryBspResolver(project.stateStore.projectBasePath, bspConnectionService.server!!, bspSyncConsoleService.bspSyncConsole)
    bspResolver.collectModel()
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project
    val connectionService = project?.getService(BspConnectionService::class.java)
    e.presentation.isEnabled = connectionService?.isRunning() == true
  }
}

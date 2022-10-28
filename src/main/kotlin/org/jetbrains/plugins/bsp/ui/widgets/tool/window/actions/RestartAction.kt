package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.project.stateStore
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetailsParser
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.services.BspUtilService
import org.jetbrains.plugins.bsp.services.VeryTemporaryBspResolver
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import org.jetbrains.plugins.bsp.ui.console.ConsoleOutputStream
import javax.swing.Icon

public class RestartAction(actionName: String, icon: Icon) : AnAction({ actionName }, icon) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!
    val bspUtilService = BspUtilService.getInstance()
    val bspConnectionService = project.getService(BspConnectionService::class.java)
    val bspConsoleService = BspConsoleService.getInstance(project)

    val projectPath = project.getUserData(BspUtilService.projectPathKey)
    val selectedBuildTool = bspUtilService.selectedBuildTool[project.locationHash]
    if ((projectPath != null) && (selectedBuildTool != null)) {
      runBackgroundableTask("Restart action", project) {
        bspConnectionService.disconnect()

        val bspSyncConsole: TaskConsole = bspConsoleService.bspSyncConsole
        bspSyncConsole.startTask("bsp-obtain-config", "Obtain config", "Obtaining...")
        val bspConnectionDetailsGeneratorProvider = BspConnectionDetailsGeneratorProvider(projectPath, BspConnectionDetailsGeneratorExtension.extensions())
        val generatedConnectionDetailsFile = bspConnectionDetailsGeneratorProvider.generateBspConnectionDetailFileForGeneratorWithName(selectedBuildTool, ConsoleOutputStream("bsp-obtain-config", bspSyncConsole))
        generatedConnectionDetailsFile?.let {
          bspConnectionService.connect(LocatedBspConnectionDetailsParser.parseFromFile(it)!!)

          val bspResolver = VeryTemporaryBspResolver(project.stateStore.projectBasePath, bspConnectionService.server!!, bspConsoleService.bspSyncConsole, bspConsoleService.bspBuildConsole)
          bspResolver.collectModel()
        }
      }
    }
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project
    val connectionService = project?.getService(BspConnectionService::class.java)
    e.presentation.isEnabled = connectionService?.isRunning() == true
  }
}

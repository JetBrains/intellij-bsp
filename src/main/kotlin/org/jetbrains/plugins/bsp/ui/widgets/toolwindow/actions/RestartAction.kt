package org.jetbrains.plugins.bsp.ui.widgets.toolwindow.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.project.stateStore
import javax.swing.Icon
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.plugins.bsp.services.BspSyncConsoleService
import org.jetbrains.plugins.bsp.services.BspUtilService
import org.jetbrains.plugins.bsp.services.VeryTemporaryBspResolver
import org.jetbrains.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.protocol.connection.LocatedBspConnectionDetailsParser

public class RestartAction(actionName: String, icon: Icon) : AnAction({actionName}, icon) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!
    val bspUtilService = BspUtilService.getInstance()
    val bspConnectionService = project.getService(BspConnectionService::class.java)

    val projectPath = project.getUserData(BspUtilService.key)
    val selectedBuildTool = bspUtilService.selectedBuildTool[project.locationHash]
    if ((projectPath != null) && (selectedBuildTool != null)) {
      bspConnectionService.disconnect()

      val bspConnectionDetailsGeneratorProvider = BspConnectionDetailsGeneratorProvider(projectPath, BspConnectionDetailsGeneratorExtension.extensions())
      val generatedConnectionDetailsFile = bspConnectionDetailsGeneratorProvider.generateBspConnectionDetailFileForGeneratorWithName(selectedBuildTool)
      generatedConnectionDetailsFile?.let {
        bspConnectionService.connect(LocatedBspConnectionDetailsParser.parseFromFile(it)!!)

        val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)
        val bspResolver = VeryTemporaryBspResolver(project.stateStore.projectBasePath, bspConnectionService.server!!, bspSyncConsoleService.bspSyncConsole)
        bspResolver.collectModel()
      }
    }
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project
    val connectionService = project?.getService(BspConnectionService::class.java)
    e.presentation.isEnabled = connectionService?.isRunning() == true
  }
}

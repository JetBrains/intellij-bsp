package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.project.stateStore
import org.jetbrains.plugins.bsp.services.*
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class BuildTargetAction(
  private val target: BuildTargetIdentifier
) : AnAction(BspAllTargetsWidgetBundle.message("widget.build.target.popup.message")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!
    val bspConnectionService = project.getService(BspConnectionService::class.java)
    val bspConsoleService = BspConsoleService.getInstance(project)

    val bspResolver = VeryTemporaryBspResolver(
      project.stateStore.projectBasePath,
      bspConnectionService.server!!,
      bspConsoleService.bspSyncConsole,
      bspConsoleService.bspBuildConsole
    )
    runBackgroundableTask("Build single target", project) {
      bspResolver.buildTarget(target)
    }
  }
}
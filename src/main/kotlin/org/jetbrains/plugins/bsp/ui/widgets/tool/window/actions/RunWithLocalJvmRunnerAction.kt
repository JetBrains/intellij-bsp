package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.tasks.JvmTestEnvironmentTask
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

internal class RunWithLocalJvmRunnerAction
  : LocalJvmRunnerAction(BspAllTargetsWidgetBundle.message("widget.run.target.with.runner.popup.message")) {

  override fun getEnvironment(project: Project): JvmEnvironmentItem? =
    target?.let { target ->
      JvmTestEnvironmentTask(project).executeIfConnected(target)?.items?.first()
    }

  override fun getExecutor(): Executor = DefaultRunExecutor.getRunExecutorInstance()
}

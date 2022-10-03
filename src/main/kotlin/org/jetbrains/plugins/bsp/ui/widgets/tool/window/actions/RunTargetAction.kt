package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerEx
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.bsp.services.BspUtilService
import org.jetbrains.plugins.bsp.ui.run.configuration.BspRunConfigurationType
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class RunTargetAction(
  private val target: BuildTargetIdentifier
) : AnAction(BspAllTargetsWidgetBundle.message("widget.run.target.popup.message")) {

  override fun actionPerformed(e: AnActionEvent) {
    e.project?.let { project ->

      val factory = BspRunConfigurationType().configurationFactories.first()
      val setting = RunManager.getInstance(project).createConfiguration("run ${target.uri}", factory)
      val runManagerEx = RunManagerEx.getInstanceEx(project)
      runManagerEx.setTemporaryConfiguration(setting)
      val runExecutor = DefaultRunExecutor.getRunExecutorInstance()
      ProgramRunner.getRunner(runExecutor.id, setting.configuration)?.let {
        try {
          val executionEnvironment = ExecutionEnvironmentBuilder(project, runExecutor)
            .runnerAndSettings(it, setting)
            .build()
          executionEnvironment.putUserData(BspUtilService.targetIdKey, target)
          it.execute(executionEnvironment)
        }
        catch (e: Exception) {
          Messages.showErrorDialog(project, e.message, "error")
        }
      }
    }
  }
}
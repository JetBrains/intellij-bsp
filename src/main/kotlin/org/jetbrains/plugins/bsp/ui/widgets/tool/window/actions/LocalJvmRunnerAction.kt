package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.utils.orDefault
import java.util.*

internal abstract class LocalJvmRunnerAction(
  label: String
) : AbstractActionWithTarget(label) {

  abstract fun getEnvironment(project: Project): JvmEnvironmentItem?

  abstract fun getExecutor(): Executor

  open fun runWithEnvironment(environment: JvmEnvironmentItem, uri: String, module: Module, project: Project) {
    environment.mainClasses
      ?.firstOrNull() // TODO: support targets with multiple main classes
      ?.let { mainClass ->
        val applicationConfiguration = ApplicationConfiguration("Run $uri ${UUID.randomUUID()}", project).apply {
          setModule(module)
          mainClassName = mainClass.className
          programParameters = mainClass.arguments.joinToString(" ")
          putUserData(jvmEnvironment, environment)
        }
        val runManager = RunManagerImpl.getInstanceImpl(project)
        val settings = RunnerAndConfigurationSettingsImpl(runManager, applicationConfiguration)
        RunManager.getInstance(project).setTemporaryConfiguration(settings)
        val runExecutor = getExecutor()
        ApplicationManager.getApplication().invokeLater {
          ProgramRunner.getRunner(runExecutor.id, settings.configuration)?.let { runner ->
            val executionEnvironment = ExecutionEnvironmentBuilder(project, runExecutor)
              .runnerAndSettings(runner, settings)
              .build()
            runner.execute(executionEnvironment)
          }
        }
      }
  }

  override fun actionPerformed(e: AnActionEvent) {
    target?.let { target ->
      val uri = target.uri
      val project = e.project ?: return
      val moduleNameProvider = project.findModuleNameProvider().orDefault()
      val module = project.modules.find { it.name == moduleNameProvider(target) } ?: return
      ApplicationManager.getApplication().executeOnPooledThread {
        getEnvironment(project)?.let {
          runWithEnvironment(it, uri, module, project)
        }
      }
    }
  }

  companion object {
    val jvmEnvironment = Key<JvmEnvironmentItem>("jvmEnvironment")
  }

}

package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.server.tasks.RunTargetTask
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import javax.swing.Icon

internal class BspRunConfigurationType(project: Project) : ConfigurationType {
  private val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)

  override fun getDisplayName(): String =
    BspPluginBundle.message("run.config.type.display.name", assetsExtension.presentableName)

  override fun getConfigurationTypeDescription(): String =
    BspPluginBundle.message("run.config.type.description", assetsExtension.presentableName)

  override fun getIcon(): Icon = assetsExtension.icon

  override fun getId(): String = ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> =
    arrayOf(BspRunFactory(this))

  companion object {
    const val ID: String = "BspRunConfiguration"
  }
}

public class BspRunFactory(t: ConfigurationType) : ConfigurationFactory(t) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
    return BspRunConfiguration(project, this,
      BspPluginBundle.message("run.config.name", assetsExtension.presentableName))
  }

  override fun getId(): String =
    BspRunConfigurationType.ID
}

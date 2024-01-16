package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

public class BspRunConfiguration(
  project: Project,
  configurationFactory: BspRunFactory,
  name: String,
) : LocatableConfigurationBase<RunProfileState>(project, configurationFactory, name),
  RunConfigurationWithSuppressedDefaultDebugAction,
  DumbAware {
  public var runType: BspRunType? = null
  public var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
  public var targetUri: String? = null
  public var debugType: BspDebugType? = null

  override fun checkConfiguration() {
    // TODO: check if targetUri is valid
    // Check if target can be debugged
    // Check if target can be run/tested
  }


  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    // By default, a new unique execution ID is assigned to each new ExecutionEnvironment
    val originId = environment.executionId.toString()
    return BspCommandLineState(project, environment, this, originId)
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    return BspRunConfigurationEditor(this)
  }
}

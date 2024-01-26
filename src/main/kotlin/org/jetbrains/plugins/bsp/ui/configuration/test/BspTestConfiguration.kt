package org.jetbrains.plugins.bsp.ui.configuration.test

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.ui.configuration.BspTestConfigurationType

public class BspTestConfiguration(
  project: Project,
  configurationFactory: BspTestConfigurationType,
  name: String,
) : LocatableConfigurationBase<RunProfileState>(project, configurationFactory, name),
  RunConfigurationWithSuppressedDefaultDebugAction,
  SMRunnerConsolePropertiesProvider,
  DumbAware {
  public var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
  public var targetUris: List<String> = emptyList()

  override fun checkConfiguration() {
    // TODO: check if targetUri is valid
    // Check if target can be debugged
    // Check if target can be run/tested
  }


  override fun getState(executor: Executor, environment: ExecutionEnvironment): BspTestCommandLineState {
    // By default, a new unique execution ID is assigned to each new ExecutionEnvironment
    val originId = environment.executionId.toString()
    return BspTestCommandLineState(project, environment, this, originId)
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    return BspRunConfigurationEditor(this)
  }

  override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties {
    TODO("Not yet implemented")
  }
}

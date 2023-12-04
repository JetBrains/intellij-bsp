package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

public class BspRunConfiguration(
  project: Project,
  configurationFactory: BspRunFactory,
  name: String,
  public val debugType: BspDebugType? = null,
  public val targetUri: String? = null,
) : LocatableConfigurationBase<Nothing>(project, configurationFactory, name),
  RunConfigurationWithSuppressedDefaultDebugAction {

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    // By default, a new unique execution ID is assigned to each new ExecutionEnvironment
    BspCommandLineState(project, environment, this, environment.executionId.toString())

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    // TODO https://youtrack.jetbrains.com/issue/BAZEL-627
    TODO("Not yet implemented")
  }
}

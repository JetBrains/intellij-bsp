package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.ui.configuration.BspBuildConfigurationType
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationType
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationTypeBase
import org.jetbrains.plugins.bsp.ui.configuration.BspTestConfigurationType
import java.util.*

public sealed class BspRunConfigurationBase(
  project: Project,
  configurationFactory: BspRunConfigurationTypeBase,
  name: String,
) : LocatableConfigurationBase<RunProfileState>(project, configurationFactory, name),
  RunConfigurationWithSuppressedDefaultDebugAction,
  DumbAware {
  public var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT


  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val originId = UUID.randomUUID().toString()
    return when (this) {
      is BspRunConfiguration -> BspRunCommandLineState(project, environment, this, originId)
      is BspTestConfiguration -> BspTestCommandLineState(project, environment, this, originId)
      else -> throw IllegalStateException("Unknown configuration type: $this")
    }
  }

  override fun getConfigurationEditor(): SettingsEditor<out BspRunConfigurationBase> {
    return BspRunConfigurationEditor(this)
  }
}

public class BspRunConfiguration(
  project: Project,
  configurationFactory: BspRunConfigurationType,
  name: String,
) : BspRunConfigurationBase(project, configurationFactory, name) {
  public var targetUri: String? = null
  public var debugType: BspDebugType? = null

  override fun checkConfiguration() {
    // TODO: check if targetUri is valid
    // Check if target can be debugged
    // Check if target can be run/tested
  }
}

public class BspTestConfiguration(
  project: Project,
  configurationFactory: BspTestConfigurationType,
  name: String,
) : BspRunConfigurationBase(project, configurationFactory, name),
  SMRunnerConsolePropertiesProvider {
  public var targetUris: List<String> = emptyList()

  override fun checkConfiguration() {
    // TODO: check if targetUri is valid
    // Check if target can be debugged
    // Check if target can be run/tested
  }

  override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties {
    return SMTRunnerConsoleProperties(this, "BSP", executor)
  }
}

public class BspBuildConfiguration(
  project: Project,
  configurationFactory: BspBuildConfigurationType,
  name: String,
) : BspRunConfigurationBase(project, configurationFactory, name) {
  override fun checkConfiguration() {

  }
}
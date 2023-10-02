package org.jetbrains.plugins.bsp.neo

import com.intellij.execution.Executor
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons

public class BspRunConfigurationType : SimpleConfigurationType(ID, BspPluginBundle.message("run.configuration.run.name"), icon = NotNullLazyValue.createConstantValue(
  BspPluginIcons.bsp)) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    TODO("Not yet implemented")
  }

  public companion object {
    public const val ID: String = "BSP_RUN_CONFIGURATION"
  }

}
public class BspRunConfiguration(
  project: Project,
  name: String,
  factory: BspRunConfigurationType
) : LocatableConfigurationBase<Nothing>(project, factory, name),
  RunConfigurationWithSuppressedDefaultDebugAction {
  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    TODO("Not yet implemented")
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    TODO("Not yet implemented")
  }

}
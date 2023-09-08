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
import org.jetbrains.plugins.bsp.config.BspPluginIcons

public class BspTestConfigurationType : SimpleConfigurationType(ID, "BSP Test", icon = NotNullLazyValue.createConstantValue(
  BspPluginIcons.bsp)) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    TODO("Not yet implemented")
  }

  public companion object {
    public const val ID: String = "BSP_TEST_CONFIGURATION"
  }

}

public class BspTestConfiguration(
  project: Project,
  name: String,
  factory: BspTestConfigurationType
) : LocatableConfigurationBase<Nothing>(project, factory, name),
  RunConfigurationWithSuppressedDefaultDebugAction {
  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    TODO("Not yet implemented")
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    TODO("Not yet implemented")
  }

}
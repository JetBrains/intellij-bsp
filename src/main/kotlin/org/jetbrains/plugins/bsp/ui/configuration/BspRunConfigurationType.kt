package org.jetbrains.plugins.bsp.ui.configuration

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.run.BspTestConfiguration
import javax.swing.Icon

public sealed class BspRunConfigurationTypeBase(public val id: String, name: String, description: String, icon: NotNullLazyValue<Icon>) : SimpleConfigurationType(id, name, description, icon)

public object BspRunConfigurationType : BspRunConfigurationTypeBase(
  id = "BspRunConfiguration",
  name = BspPluginBundle.message("runconfig.run.name"),
  description = BspPluginBundle.message("runconfig.run.description"),
  icon = NotNullLazyValue.createValue { BspPluginIcons.bsp },
) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration =
    BspRunConfiguration(project, this, name)
}

public object BspTestConfigurationType : BspRunConfigurationTypeBase(
  id = "BspTestConfiguration",
  name = BspPluginBundle.message("runconfig.test.name"),
  description = BspPluginBundle.message("runconfig.test.description"),
  icon = NotNullLazyValue.createValue { BspPluginIcons.bsp },
) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration =
    BspTestConfiguration(project, this, name)
}

public object BspBuildConfigurationType : BspRunConfigurationTypeBase(
  id = "BspBuildConfiguration",
  name = BspPluginBundle.message("runconfig.build.name"),
  description = BspPluginBundle.message("runconfig.build.description"),
  icon = NotNullLazyValue.createValue { BspPluginIcons.bsp },
) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    TODO()
  }
}
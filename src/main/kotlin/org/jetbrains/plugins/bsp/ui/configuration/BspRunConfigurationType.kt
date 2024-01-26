package org.jetbrains.plugins.bsp.ui.configuration

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfiguration
import javax.swing.Icon

public sealed class BspBaseRunConfigurationType(id: String, name: String, description: String, icon: NotNullLazyValue<Icon>) : SimpleConfigurationType(id, name, description, icon) {
  internal companion object {
    internal fun assetsExtension(project: Project): BuildToolAssetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
  }
}

public class BspRunConfigurationType(project: Project) : BspBaseRunConfigurationType(
  id = "BspRunConfiguration",
  name = BspPluginBundle.message("runconfig.run.name", assetsExtension(project).presentableName),
  description = BspPluginBundle.message("runconfig.run.description", assetsExtension(project).presentableName),
  icon = NotNullLazyValue.createValue { assetsExtension(project).icon },
) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration =
    BspRunConfiguration(project, this, name)
}

public class BspTestConfigurationType(project: Project) : BspBaseRunConfigurationType(
  id = "BspTestConfiguration",
  name = BspPluginBundle.message("runconfig.test.name", assetsExtension(project).presentableName),
  description = BspPluginBundle.message("runconfig.test.description", assetsExtension(project).presentableName),
  icon = NotNullLazyValue.createValue { assetsExtension(project).icon },
) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration = TODO()
}

public class BspBuildConfigurationType(project: Project) : BspBaseRunConfigurationType(
  id = "BspBuildConfiguration",
  name = BspPluginBundle.message("runconfig.build.name", assetsExtension(project).presentableName),
  description = BspPluginBundle.message("runconfig.build.description", assetsExtension(project).presentableName),
  icon = NotNullLazyValue.createValue { assetsExtension(project).icon },
) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    TODO()
  }
}
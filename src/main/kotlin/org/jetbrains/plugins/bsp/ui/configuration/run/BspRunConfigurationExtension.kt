package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.configuration.RunConfigurationExtensionBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration

public class BspRunConfigurationExtension : RunConfigurationExtensionBase<BspRunConfiguration>() {
  override fun isApplicableFor(configuration: BspRunConfiguration): Boolean {
    TODO("Not yet implemented")
  }

  override fun isEnabledFor(applicableConfiguration: BspRunConfiguration, runnerSettings: RunnerSettings?): Boolean {
    TODO("Not yet implemented")
  }

  override fun patchCommandLine(
    configuration: BspRunConfiguration,
    runnerSettings: RunnerSettings?,
    cmdLine: GeneralCommandLine,
    runnerId: String
  ) {
    // Not used
  }

}


package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase

/**
 * Supports the run configuration flow for BSP run configurations.
 *
 * <p>Provides language-specific configuration state, validation, presentation, and runner.
 */
public interface BspRunHandler {
  public val settings: BspRunConfigurationSettings

  /**
   * The name of the run handler (shown in the UI).
   */
  public val name: String

  public fun getRunProfileState(
    executor: Executor,
    environment: ExecutionEnvironment,
  ): RunProfileState
}

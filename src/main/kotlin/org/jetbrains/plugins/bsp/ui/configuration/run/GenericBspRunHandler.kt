package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import java.util.*

public open class GenericBspRunHandler : BspRunHandler {
  override fun canRun(targets: List<BuildTargetInfo>): Boolean = targets.all { it.capabilities.canRun }

  override fun canDebug(targets: List<BuildTargetInfo>): Boolean = false

  override fun getRunProfileState(
    project: Project,
    executor: Executor,
    environment: ExecutionEnvironment,
    configuration: BspRunConfigurationBase,
  ): RunProfileState = if (configuration !is BspRunConfiguration) {
    throw IllegalArgumentException("GenericBspRunHandler can only handle BspRunConfiguration")
  } else {
    this.thisLogger().warn("Using generic run handler for ${configuration.name}")
    BspRunCommandLineState(project, environment, configuration, UUID.randomUUID().toString())
  }
}

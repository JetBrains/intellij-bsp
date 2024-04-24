package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.thisLogger
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import org.jetbrains.plugins.bsp.ui.configuration.BspTestConfiguration
import java.util.UUID

public class OtherHandler(private val configuration: BspRunConfigurationBase) : BspRunHandler {
  override val settings: OtherState = OtherState()

  override val name: String = "Other Handler"

  override fun getRunProfileState(
    executor: Executor,
    environment: ExecutionEnvironment,
  ): RunProfileState = when (configuration) {
    is BspTestConfiguration -> {
      thisLogger().warn("Using test handler for ${configuration.name}")
      BspTestCommandLineState(environment, configuration, UUID.randomUUID().toString())
    }

    is BspRunConfiguration -> {
      thisLogger().warn("Using run handler for ${configuration.name}")
      BspRunCommandLineState(environment, configuration, UUID.randomUUID().toString())
    }

    else -> {
      throw IllegalArgumentException("Other can only handle BspRunConfiguration")
    }
  }
}

public class GenericBspRunHandler(private val configuration: BspRunConfigurationBase) : BspRunHandler {
  override val settings: BspRunConfigurationState = SomeState()

  override val name: String = "Generic BSP Run Handler"

  override fun getRunProfileState(
    executor: Executor,
    environment: ExecutionEnvironment,
  ): RunProfileState = when (configuration) {
    is BspTestConfiguration -> {
      thisLogger().warn("Using generic test handler for ${configuration.name}")
      BspTestCommandLineState(environment, configuration, UUID.randomUUID().toString())
    }

    is BspRunConfiguration -> {
      thisLogger().warn("Using generic run handler for ${configuration.name}")
      BspRunCommandLineState(environment, configuration, UUID.randomUUID().toString())
    }

    else -> {
      throw IllegalArgumentException("GenericBspRunHandler can only handle BspRunConfiguration")
    }
  }
}

package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.thisLogger
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.BspTestConfiguration
import java.util.UUID

public class OtherHandler : BspRunHandler {
  override val settings: TestState = TestState()

  override val name: String = "Other Handler"

  override fun getRunProfileState(
    executor: Executor,
    environment: ExecutionEnvironment,
  ): RunProfileState {
    return when (val configuration = environment.runProfile) {
      is BspTestConfiguration -> {
        thisLogger().warn("Using test handler for ${configuration.name}")
        BspTestCommandLineState(environment, UUID.randomUUID().toString())
      }

      is BspRunConfiguration -> {
        thisLogger().warn("Using run handler for ${configuration.name}")
        BspRunCommandLineState(environment, UUID.randomUUID().toString())
      }

      else -> {
        throw IllegalArgumentException("Other can only handle BspRunConfiguration")
      }
    }
  }
}

  public class GenericBspRunHandler : BspRunHandler {
    override val settings: BspRunConfigurationState = SomeState()

    override val name: String = "Generic BSP Run Handler"

    override fun getRunProfileState(
      executor: Executor,
      environment: ExecutionEnvironment,
    ): RunProfileState {
      return when (val configuration = environment.runProfile) {
        is BspTestConfiguration -> {
          thisLogger().warn("Using generic test handler for ${configuration.name}")
          BspTestCommandLineState(environment, UUID.randomUUID().toString())
        }

        is BspRunConfiguration -> {
          thisLogger().warn("Using generic run handler for ${configuration.name}")
          BspRunCommandLineState(environment, UUID.randomUUID().toString())
        }

        else -> {
          throw IllegalArgumentException("GenericBspRunHandler can only handle BspRunConfiguration")
        }
      }
    }
  }


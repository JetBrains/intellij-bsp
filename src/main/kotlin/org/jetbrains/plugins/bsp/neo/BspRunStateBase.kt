package org.jetbrains.plugins.bsp.neo

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment

public abstract class BspRunStateBase(
  environment: ExecutionEnvironment,
  private val runConfiguration: BspRunConfiguration
) : CommandLineState(environment) {
  override fun startProcess(): BspProcessHandler {
    val handler = BspProcessHandler()
    handler.startNotify()
    return handler
  }
}
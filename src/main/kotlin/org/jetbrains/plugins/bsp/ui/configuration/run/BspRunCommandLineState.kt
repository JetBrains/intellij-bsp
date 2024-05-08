package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import java.util.concurrent.CompletableFuture

public abstract class BspCommandLineStateBase(
  private val environment: ExecutionEnvironment,
  private val configuration: BspRunConfigurationBase,
  private val originId: OriginId,
) : CommandLineState(environment) {
  /** Throws an exception if the server does not support running the configuration */
  protected abstract fun checkRunCapabilities(capabilities: BazelBuildServerCapabilities)

  protected abstract fun createAndAddTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener

  protected abstract fun startBsp(server: BspServer): CompletableFuture<*>

  final override fun startProcess(): BspProcessHandler<out Any> {
    // We have to start runFuture later, because we need to register the listener first
    // Otherwise, we might miss some events
    val computationStarter = CompletableFuture<Unit>()
    val runFuture = computationStarter.thenCompose {
      // The "useless" type below is actually needed because of a bug in Kotlin compiler
      val completableFuture: CompletableFuture<*> =
        configuration.project.connection.runWithServer { server: BspServer, capabilities: BazelBuildServerCapabilities ->
          checkRunCapabilities(capabilities)
          startBsp(server)
        }
      completableFuture
    }

    val handler = BspProcessHandler(runFuture)
    val runListener = createAndAddTaskListener(handler)

    with(BspTaskEventsService.getInstance(configuration.project)) {
      saveListener(originId, runListener)
      runFuture.handle { _, _ ->
        removeListener(originId)
      }
    }

    computationStarter.complete(Unit)
    handler.startNotify()

    return handler
  }
}

internal class BspRunCommandLineState(
  environment: ExecutionEnvironment,
  private val configuration: BspRunConfiguration,
  private val originId: OriginId,
) : BspCommandLineStateBase(environment, configuration, originId) {
  override fun checkRunCapabilities(capabilities: BazelBuildServerCapabilities) {
    if (configuration.targets.singleOrNull() == null || capabilities.runProvider == null) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }
  }

  override fun createAndAddTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener =
    BspRunTaskListener(handler)

  override fun startBsp(server: BspServer): CompletableFuture<*> {
    // SAFETY: safe to unwrap because we checked in checkRunCapabilities
    val targetId = BuildTargetIdentifier(configuration.targets.single())
    val runParams = RunParams(targetId)
    runParams.originId = originId
    return server.buildTargetRun(runParams)
  }
}

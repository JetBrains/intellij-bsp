package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
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
import kotlin.math.log

public abstract class BspCommandLineStateBase(
  private val environment: ExecutionEnvironment,
  private val configuration: BspRunConfigurationBase,
  private val originId: OriginId
) : CommandLineState(environment) {
  protected abstract fun checkRun(capabilities: BazelBuildServerCapabilities)

  protected abstract fun makeTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener

  protected abstract fun startBsp(server: BspServer): CompletableFuture<Any>

  final override fun startProcess(): BspProcessHandler<out Any> {
    // We have to start runFuture later, because we need to register the listener first
    // Otherwise, we might miss some events
    val computationStarter = CompletableFuture<Unit>()
    val runFuture = computationStarter.thenCompose {
      configuration.project.connection.runWithServer { server, capabilities ->
          checkRun(capabilities)
          startBsp(server)
        }
      }

    val handler = BspProcessHandler(runFuture)
    val runListener = makeTaskListener(handler)

    with(BspTaskEventsService.getInstance(configuration.project)) {
      addListener(originId, runListener)
      runFuture.handle { _, _ ->
        removeListener(originId) }
    }

    computationStarter.complete(Unit)
    handler.startNotify()

    return handler
  }
}

internal class BspRunCommandLineState(
  environment: ExecutionEnvironment,
  private val configuration: BspRunConfiguration,
  private val originId: OriginId
) : BspCommandLineStateBase(environment, configuration, originId) {
  override fun checkRun(capabilities: BazelBuildServerCapabilities) {
    if (configuration.targets.singleOrNull() == null || capabilities.runProvider == null) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }
  }

  override fun makeTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener {
    return BspRunTaskListener(handler)
  }

  override fun startBsp(server: BspServer): CompletableFuture<Any> {
    // SAFETY: safe to unwrap because we checked in checkRun
    val targetId = BuildTargetIdentifier(configuration.targets.single())
    val runParams = RunParams(targetId)
    runParams.originId = originId
    return server.buildTargetRun(runParams) as CompletableFuture<Any>
  }
}
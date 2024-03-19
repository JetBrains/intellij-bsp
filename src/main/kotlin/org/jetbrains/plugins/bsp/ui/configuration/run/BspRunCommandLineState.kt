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
  private val project: Project,
  private val environment: ExecutionEnvironment,
  private val configuration: BspRunConfigurationBase,
  private val originId: OriginId
) : CommandLineState(environment) {
  protected abstract fun checkRun(capabilities: BazelBuildServerCapabilities)

  protected abstract fun makeTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener

  protected abstract fun startBsp(server: BspServer): CompletableFuture<Any>

  final override fun startProcess(): BspProcessHandler<out Any> {
    val logger = logger<BspCommandLineStateBase>()
    logger.warn("kurwa process started")
    val computationStarter = CompletableFuture<Unit>()
    val runFuture = computationStarter.thenCompose {
      logger.warn("kurwa future started")
      project.connection.runWithServer { server, capabilities ->
          logger.warn("kurwa inside runWithServer")
          checkRun(capabilities)
          logger.warn("kurwa after checkRun")
          startBsp(server)
        }
      }.handle {
        result, error ->
      if (error != null) {
        logger.warn( "kurwa error: $error", error)
        throw ExecutionException(error)
      } else {
        logger.warn("kurwa result: $result")
      }
      result
    }

    val handler = BspProcessHandler(runFuture)
    val runListener = makeTaskListener(handler)

    logger.warn("kurwa before addListener")

    with(BspTaskEventsService.getInstance(project)) {
      addListener(originId, runListener)
      runFuture.handle { _, _ ->
        removeListener(originId) }
    }

    computationStarter.complete(Unit)
    handler.startNotify()

    logger.warn("kurwa after startNotify")
    return handler
  }
}

internal class BspRunCommandLineState(
  project: Project,
  environment: ExecutionEnvironment,
  private val configuration: BspRunConfiguration,
  private val originId: OriginId
) : BspCommandLineStateBase(project, environment, configuration, originId) {
  override fun checkRun(capabilities: BazelBuildServerCapabilities) {
    if (configuration.targets.singleOrNull()?.id == null || capabilities.runProvider == null) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }
  }

  override fun makeTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener {
    return BspRunTaskListener(handler)
  }

  override fun startBsp(server: BspServer): CompletableFuture<Any> {
    // SAFETY: safe to unwrap because we checked in checkRun
    val logger = logger<BspRunCommandLineState>()
    logger.warn("kurwa startBsp")
    val targetId = BuildTargetIdentifier(configuration.targets.single().id)
    val runParams = RunParams(targetId)
    runParams.originId = originId
    logger.warn("kurwa runParams: $runParams")
    return server.buildTargetRun(runParams) as CompletableFuture<Any>
  }
}
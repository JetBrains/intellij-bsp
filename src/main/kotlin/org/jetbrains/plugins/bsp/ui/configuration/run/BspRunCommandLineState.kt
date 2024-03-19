package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.RunResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.bsp.protocol.BazelBuildServer
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.services.TaskId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import java.util.concurrent.CompletableFuture

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
    val computationStarter = CompletableFuture<Unit>()
    val runFuture = computationStarter.thenCompose {
      project.connection.runWithServer { server, capabilities ->
          checkRun(capabilities)
          startBsp(server)
        }
      }

    val handler = BspProcessHandler(runFuture)
    val runListener = makeTaskListener(handler)

    with(BspTaskEventsService.getInstance(project)) {
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
  project: Project,
  environment: ExecutionEnvironment,
  private val configuration: BspRunConfiguration,
  private val originId: OriginId
) : BspCommandLineStateBase(project, environment, configuration, originId) {
  override fun checkRun(capabilities: BazelBuildServerCapabilities) {
    if (configuration.target?.id == null || capabilities.runProvider == null) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }
  }

  override fun makeTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener {
    return BspRunTaskListener(handler)
  }

  override fun startBsp(server: BspServer): CompletableFuture<Any> {
    // SAFETY: safe to unwrap because we checked in checkRun
    val targetId = BuildTargetIdentifier(configuration.target?.id!!)
    val runParams = RunParams(targetId)
    runParams.originId = originId
    return server.buildTargetRun(runParams) as CompletableFuture<Any>
  }
}
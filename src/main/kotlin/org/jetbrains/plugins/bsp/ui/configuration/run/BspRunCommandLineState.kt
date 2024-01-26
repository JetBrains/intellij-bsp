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
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.services.TaskId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import java.util.concurrent.CompletableFuture

internal class BspRunCommandLineState(
  val project: Project,
  environment: ExecutionEnvironment,
  private val configuration: BspRunConfiguration,
  private val originId: OriginId
) : CommandLineState(environment) {
  val remoteConnection: RemoteConnection? = createRemoteConnection()
  val log = logger<BspRunCommandLineState>()

  private fun createRemoteConnection(): RemoteConnection? =
    when (configuration.debugType) {
      BspDebugType.JDWP -> RemoteConnection(true, "localhost", "0", true)
      else -> null
    }

  fun canRun(capabilities: BuildServerCapabilities): Boolean =
    configuration.targetUri != null && capabilities.runProvider != null

  fun makeTaskListener(handler: BspProcessHandler<RunResult>): BspTaskListener {
    return object : BspTaskListener {
      val ansiEscapeDecoder = AnsiEscapeDecoder()

      override fun onOutputStream(taskId: TaskId?, text: String) {
        ansiEscapeDecoder.escapeText(text, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
          handler.notifyTextAvailable(s, key)
        }
      }

      override fun onErrorStream(taskId: TaskId?, text: String) {
        ansiEscapeDecoder.escapeText(text, ProcessOutputType.STDERR) { s: String, key: Key<Any> ->
          handler.notifyTextAvailable(s, key)
        }
      }

      // For compatibility with older BSP servers
      // TODO: Log messages in the correct place
      override fun onLogMessage(message: String) {
        ansiEscapeDecoder.escapeText(message, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
          log.warn("Log message: $s")
          handler.notifyTextAvailable(s, key)
        }
      }
    }
  }
  override fun startProcess(): BspProcessHandler<RunResult> {

    val computationStarter = CompletableFuture<Unit>()
    // We have to start runFuture later, because we need to register the listener first
    // Otherwise, we might miss some events
    val runFuture = computationStarter.thenCompose {
      project.connection.runWithServer { server, capabilities ->
        if (canRun(capabilities)) {
          val targetId = BuildTargetIdentifier(configuration.targetUri!!)
          val runParams = RunParams(targetId)
          runParams.originId = originId
          server.buildTargetRun(runParams).thenApply { runResult ->
            runResult
          }
        } else throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
      }
    }

    val handler = BspProcessHandler(runFuture)
    val runListener = makeTaskListener(handler)

    BspTaskEventsService.getInstance(project).addListener(originId, runListener)

    computationStarter.complete(Unit)
    handler.startNotify()
    return handler
  }

//  fun executer(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
//    val bspRunConsole = BspConsoleService.getInstance(project).bspRunConsole
//    val processHandler = startProcess()
//    val console = createConsole(executor)?.apply {
//      attachToProcess(processHandler)
//    }
//    configuration.targetUri?.let { uri ->
//      bspRunConsole.registerPrinter(processHandler)
//      processHandler.execute {
//        processHandler.printOutput(BspPluginBundle.message("console.task.run.start", uri))
//        try {
//          val portForDebugIfApplicable = if (executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
//            remoteConnection?.debuggerAddress?.toInt()
//          } else null
//          RunTargetTask(project, configuration.debugType, portForDebugIfApplicable)
//            .connectAndExecute(BuildTargetIdentifier(uri))
//            ?.apply {
//              when (statusCode) {
//                StatusCode.OK -> processHandler.printOutput(BspPluginBundle.message("console.task.status.ok"))
//                StatusCode.CANCELLED -> processHandler.printOutput(
//                  BspPluginBundle.message("console.task.status.cancelled"))
//                StatusCode.ERROR -> processHandler.printOutput(BspPluginBundle.message("console.task.status.error"))
//                else -> processHandler.printOutput(BspPluginBundle.message("console.task.status.other"))
//              }
//            }
//        } finally {
//          bspRunConsole.deregisterPrinter(processHandler)
//          processHandler.shutdown()
//        }
//      }
//    } ?: processHandler.shutdown()
//    return DefaultExecutionResult(console, processHandler)
//  }
}
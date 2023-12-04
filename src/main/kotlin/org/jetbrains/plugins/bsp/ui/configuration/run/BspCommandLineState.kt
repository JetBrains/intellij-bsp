package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.tasks.RunTargetTask
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.services.TaskId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService

internal class BspCommandLineState(
  val project: Project,
  environment: ExecutionEnvironment,
  private val configuration: BspRunConfiguration,
  private val originId: OriginId
) : CommandLineState(environment) {
  val remoteConnection: RemoteConnection? = createRemoteConnection()

  private fun createRemoteConnection(): RemoteConnection? =
    when (configuration.debugType) {
      BspDebugType.JDWP -> RemoteConnection(true, "localhost", "0", true)
      else -> null
    }

  override fun startProcess(): BspProcessHandler {
    val handler = BspProcessHandler()
    val ansiEscapeDecoder = AnsiEscapeDecoder()
    val runListener = object : BspTaskListener {
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
        ansiEscapeDecoder.escapeText(message, ProcessOutputType.SYSTEM) { s: String, key: Key<Any> ->
          handler.notifyTextAvailable(s, key)
        }
      }
    }

    BspTaskEventsService.getInstance(project).addListener(originId, runListener)

    handler.startNotify()
    return handler
  }

  fun executer(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    val bspRunConsole = BspConsoleService.getInstance(project).bspRunConsole
    val processHandler = startProcess()
    val console = createConsole(executor)?.apply {
      attachToProcess(processHandler)
    }
    configuration.targetUri?.let { uri ->
      bspRunConsole.registerPrinter(processHandler)
      processHandler.execute {
        processHandler.printOutput(BspPluginBundle.message("console.task.run.start", uri))
        try {
          val portForDebugIfApplicable = if (executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
            remoteConnection?.debuggerAddress?.toInt()
          } else null
          RunTargetTask(project, configuration.debugType, portForDebugIfApplicable)
            .connectAndExecute(BuildTargetIdentifier(uri))
            ?.apply {
              when (statusCode) {
                StatusCode.OK -> processHandler.printOutput(BspPluginBundle.message("console.task.status.ok"))
                StatusCode.CANCELLED -> processHandler.printOutput(
                  BspPluginBundle.message("console.task.status.cancelled"))
                StatusCode.ERROR -> processHandler.printOutput(BspPluginBundle.message("console.task.status.error"))
                else -> processHandler.printOutput(BspPluginBundle.message("console.task.status.other"))
              }
            }
        } finally {
          bspRunConsole.deregisterPrinter(processHandler)
          processHandler.shutdown()
        }
      }
    } ?: processHandler.shutdown()
    return DefaultExecutionResult(console, processHandler)
  }
}
package org.jetbrains.plugins.bsp.ui.configuration.test

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunResult
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TestFinish
import ch.epfl.scala.bsp4j.TestParams
import ch.epfl.scala.bsp4j.TestResult
import ch.epfl.scala.bsp4j.TestStart
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.services.TaskId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler

public class BspTestCommandLineState(
  private val project: Project,
  private val environment: ExecutionEnvironment,
  private val configuration: BspTestConfiguration,
  private val originId: OriginId
) : CommandLineState(environment) {
  private val log = logger<BspTestCommandLineState>()

  private fun canRun(capabilities: BuildServerCapabilities): Boolean =
    configuration.targetUris.isNotEmpty() && capabilities.testProvider != null

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    val properties = configuration.createTestConsoleProperties(executor)
    val handler = startProcess()

    val console: BaseTestsOutputConsoleView =
      SMTestRunnerConnectionUtil.createAndAttachConsole(BspPluginBundle.message("console.tasks.test.framework.name"), handler, properties)

    val actions = createActions(console, handler, executor)

    return DefaultExecutionResult(console, handler, *actions)
  }

  override fun startProcess(): BspProcessHandler<TestResult> {
    val cf = project.connection.runWithServer { server, capabilities ->
      if (canRun(capabilities)) {
        val targets = configuration.targetUris.map { BuildTargetIdentifier(it) }
        val runParams = TestParams(targets)
        runParams.originId = originId
        server.buildTargetTest(runParams)
      } else throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }

    val handler = BspProcessHandler(cf)
    val ansiEscapeDecoder = AnsiEscapeDecoder()

    val runListener = object : BspTaskListener {
      override fun onTaskStart(taskId: TaskId, parentId: TaskId?, message: String, data: Any?) {
        when (data) {
          is TestStart -> {
            val testSuiteStarted = "\n" + ServiceMessageBuilder.testStarted(data.displayName).toString() + "\n"
            handler.notifyTextAvailable(testSuiteStarted, ProcessOutputType.STDOUT)
          }
        }
      }

      override fun onTaskFinish(taskId: TaskId, message: String, status: StatusCode, data: Any?) {
        when (data) {
          is TestFinish -> {
            val testSuiteFinished = "\n" + ServiceMessageBuilder.testFinished(data.displayName).toString() + "\n"
            handler.notifyTextAvailable(testSuiteFinished, ProcessOutputType.STDOUT)
          }
        }
      }

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

    BspTaskEventsService.getInstance(project).addListener(originId, runListener)

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
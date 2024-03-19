package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TestFinish
import ch.epfl.scala.bsp4j.TestParams
import ch.epfl.scala.bsp4j.TestReport
import ch.epfl.scala.bsp4j.TestStart
import ch.epfl.scala.bsp4j.TestTask
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.services.TaskId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import java.util.concurrent.CompletableFuture

public class BspTestCommandLineState(
  private val project: Project,
  private val environment: ExecutionEnvironment,
  private val configuration: BspTestConfiguration,
  private val originId: OriginId
) : BspCommandLineStateBase(project, environment, configuration, originId) {
  private val log = logger<BspTestCommandLineState>()

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    val properties = configuration.createTestConsoleProperties(executor)
    val handler = startProcess()

    val console: BaseTestsOutputConsoleView =
      SMTestRunnerConnectionUtil.createAndAttachConsole(BspPluginBundle.message("console.tasks.test.framework.name"), handler, properties)

    val actions = createActions(console, handler, executor)

    return DefaultExecutionResult(console, handler, *actions)
  }

  override fun checkRun(capabilities: BazelBuildServerCapabilities) {
    if (configuration.targets.isEmpty() || capabilities.testProvider == null) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }
  }

  override fun makeTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener {
    val ansiEscapeDecoder = AnsiEscapeDecoder()

    return object : BspTaskListener {
      override fun onTaskStart(taskId: TaskId, parentId: TaskId?, message: String, data: Any?) {
        when (data) {
          is TestStart -> {
            val testSuiteStarted = "\n" + ServiceMessageBuilder.testSuiteStarted(data.displayName).toString() + "\n"
            handler.notifyTextAvailable(testSuiteStarted, ProcessOutputType.STDOUT)
          }

          is TestTask -> {
            val testStarted = "\n" + ServiceMessageBuilder.testStarted(data.target.uri).toString() + "\n"
            handler.notifyTextAvailable(testStarted, ProcessOutputType.STDOUT)
          }
        }
      }

      override fun onTaskFinish(taskId: TaskId, message: String, status: StatusCode, data: Any?) {
        when (data) {
          is TestFinish -> {
            val testSuiteFinished = "\n" + ServiceMessageBuilder.testSuiteFinished(data.displayName).toString() + "\n"
            handler.notifyTextAvailable(testSuiteFinished, ProcessOutputType.STDOUT)
          }

          is TestReport -> {
            val testFinished = "\n" + ServiceMessageBuilder.testFinished(data.target.uri).toString() + "\n"
            handler.notifyTextAvailable(testFinished, ProcessOutputType.STDOUT)
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
        val messageWithNewline = if (message.endsWith("\n")) message else "$message\n"
        ansiEscapeDecoder.escapeText(messageWithNewline, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
          log.warn("Log message: $s")
          handler.notifyTextAvailable(s, key)
        }
      }
    }
  }

  override fun startBsp(server: BspServer): CompletableFuture<Any> {
    val targets = configuration.targets.map { BuildTargetIdentifier(it.id) }
    val runParams = TestParams(targets)
    runParams.originId = originId
    return server.buildTargetTest(runParams) as CompletableFuture<Any>
  }
}
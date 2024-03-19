package org.jetbrains.plugins.bsp.ui.configuration.run

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
import org.jetbrains.plugins.bsp.ui.configuration.BspTestConfiguration
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

  override fun makeTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener = BspTestTaskListener(handler)

  override fun startBsp(server: BspServer): CompletableFuture<Any> {
    val targets = configuration.targets.map { BuildTargetIdentifier(it.id) }
    val runParams = TestParams(targets)
    runParams.originId = originId
    return server.buildTargetTest(runParams) as CompletableFuture<Any>
  }
}
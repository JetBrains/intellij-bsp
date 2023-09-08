package org.jetbrains.plugins.bsp.server.client

import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.CompileTask
import ch.epfl.scala.bsp4j.DiagnosticSeverity
import ch.epfl.scala.bsp4j.DidChangeBuildTarget
import ch.epfl.scala.bsp4j.LogMessageParams
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.ShowMessageParams
import ch.epfl.scala.bsp4j.TaskDataKind
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartDataKind
import ch.epfl.scala.bsp4j.TaskStartParams
import ch.epfl.scala.bsp4j.TestFinish
import ch.epfl.scala.bsp4j.TestStart
import ch.epfl.scala.bsp4j.TestStatus
import ch.epfl.scala.bsp4j.TestTask
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.connection.TimeoutHandler
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.ui.console.BspTargetRunConsole
import org.jetbrains.plugins.bsp.ui.console.BspTargetTestConsole
import org.jetbrains.plugins.bsp.ui.console.TaskConsole

public const val importSubtaskId: String = "import-subtask-id"

public class BspClient(
  private val bspSyncConsole: TaskConsole,
  private val bspBuildConsole: TaskConsole,
  private val bspRunConsole: BspTargetRunConsole,
  private val bspTestConsole: BspTargetTestConsole,
  private val timeoutHandler: TimeoutHandler,
  private val project: Project,
) : BuildClient {
  private val log = logger<BspClient>()
  private val gson = Gson()

  @Deprecated("Use actual originId once available")
  private val hardcodedOriginId = "jp2gmd" // TODO

  override fun onBuildShowMessage(params: ShowMessageParams) {
    onBuildEvent()
    addMessageToConsole(params.originId, params.message)
  }

  override fun onBuildLogMessage(params: LogMessageParams) {
    onBuildEvent()
    addMessageToConsole(params.originId, params.message)
  }

  override fun onBuildTaskStart(params: TaskStartParams) {
    onBuildEvent()

    val taskId = params.taskId.id
    val maybeParent = params.taskId.parents.firstOrNull()

    val displayName = when (params.dataKind) {
        TaskStartDataKind.TEST_START -> {
          val testStart = gson.fromJson(params.data as JsonObject, TestStart::class.java)
          testStart.displayName
        }

        TaskStartDataKind.TEST_TASK -> {
          val testTask = gson.fromJson(params.data as JsonObject, TestTask::class.java)
          testTask.target.uri
        }

        TaskStartDataKind.COMPILE_TASK -> {
          val compileTask = gson.fromJson(params.data as JsonObject, CompileTask::class.java)

          compileTask.target.uri
        }

        else -> taskId
      }

    project.service<BspTaskEventsService>().withListener(hardcodedOriginId) {
      if (maybeParent != null) {
        onSubtaskStart(taskId, maybeParent, displayName)
      } else {
        onTaskStart(taskId, displayName)
      }

      if (params.message != null) {
        onTaskProgress(taskId, params.message)
      }
    }
  }

  override fun onBuildTaskProgress(params: TaskProgressParams) {
    onBuildEvent()

    val taskId = params.taskId.id

    project.service<BspTaskEventsService>().withListener(hardcodedOriginId) {
      onTaskProgress(taskId, params.message)
    }
  }

  override fun onBuildTaskFinish(params: TaskFinishParams?) {
    onBuildEvent()
    when (params?.dataKind) {
      TaskDataKind.TEST_FINISH -> {
        val gson = Gson()
        val testFinish = gson.fromJson(params.data as JsonObject, TestFinish::class.java)
        val isSuite = if (params.message.isNullOrBlank()) false else params.message.take(3) == "<S>"
        when (testFinish.status) {
          TestStatus.FAILED -> bspTestConsole.failTest(testFinish.displayName, testFinish.message.orEmpty())
          TestStatus.PASSED -> bspTestConsole.passTest(isSuite, testFinish.displayName)
          else -> bspTestConsole.ignoreTest(testFinish.displayName)
        }
      }

      TaskDataKind.TEST_REPORT -> {}
    }
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
    onBuildEvent()
    addDiagnosticToConsole(params)
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget?) {
    onBuildEvent()
  }

  private fun onBuildEvent() {
    timeoutHandler.resetTimer()
  }

  private fun addMessageToConsole(originId: String?, message: String) {
    if (originId?.startsWith("build") == true) {
      bspBuildConsole.addMessage(originId, message)
    } else if (originId?.startsWith("test") == true) {
      bspTestConsole.print(message)
    } else if (originId?.startsWith("run") == true) {
      bspRunConsole.print(message)
    } else {
      bspSyncConsole.addMessage(originId ?: importSubtaskId, message)
    }
  }

  private fun addDiagnosticToConsole(params: PublishDiagnosticsParams) {
    if (params.originId != null && params.textDocument != null) {
      val targetConsole = if (params.originId?.startsWith("build") == true) bspBuildConsole else bspSyncConsole
      params.diagnostics.forEach {
        targetConsole.addDiagnosticMessage(
          params.originId,
          params.textDocument.uri,
          it.range.start.line,
          it.range.start.character,
          it.message,
          getMessageEventKind(it.severity),
        )
      }
    }
  }

  private fun getMessageEventKind(severity: DiagnosticSeverity?): MessageEvent.Kind =
    when (severity) {
      DiagnosticSeverity.ERROR -> MessageEvent.Kind.ERROR
      DiagnosticSeverity.WARNING -> MessageEvent.Kind.WARNING
      DiagnosticSeverity.INFORMATION -> MessageEvent.Kind.INFO
      DiagnosticSeverity.HINT -> MessageEvent.Kind.INFO
      null -> MessageEvent.Kind.SIMPLE
    }
}

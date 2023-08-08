package org.jetbrains.plugins.bsp.server.client

import com.jetbrains.bsp.bsp4kt.BuildClient
import com.jetbrains.bsp.bsp4kt.DiagnosticSeverity
import com.jetbrains.bsp.bsp4kt.DidChangeBuildTarget
import com.jetbrains.bsp.bsp4kt.LogMessageParams
import com.jetbrains.bsp.bsp4kt.PublishDiagnosticsParams
import com.jetbrains.bsp.bsp4kt.ShowMessageParams
import com.jetbrains.bsp.bsp4kt.TaskStartDataKind
import com.jetbrains.bsp.bsp4kt.TaskFinishDataKind
import com.jetbrains.bsp.bsp4kt.TaskFinishParams
import com.jetbrains.bsp.bsp4kt.TaskProgressParams
import com.jetbrains.bsp.bsp4kt.TaskStartParams
import com.jetbrains.bsp.bsp4kt.TestFinish
import com.jetbrains.bsp.bsp4kt.TestStart
import com.jetbrains.bsp.bsp4kt.TestStatus
import com.intellij.build.events.MessageEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.jetbrains.plugins.bsp.server.connection.TimeoutHandler
import org.jetbrains.plugins.bsp.ui.console.BspTargetRunConsole
import org.jetbrains.plugins.bsp.ui.console.BspTargetTestConsole
import org.jetbrains.plugins.bsp.ui.console.TaskConsole

public const val importSubtaskId: String = "import-subtask-id"

public class BspClient(
  private val bspSyncConsole: TaskConsole,
  private val bspBuildConsole: TaskConsole,
  private val bspRunConsole: BspTargetRunConsole,
  private val bspTestConsole: BspTargetTestConsole,
  private val timeoutHandler: TimeoutHandler
) : BuildClient {

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
    when (params.dataKind) {
      TaskStartDataKind.TestStart -> {
        val testStart = Json.decodeFromJsonElement<TestStart>(params.data!!)
        val isSuite = if (params.message.isNullOrBlank()) false else params.message!!.take(3) == "<S>"
        bspTestConsole.startTest(isSuite, testStart.displayName)
      }
    }
  }

  override fun onBuildTaskProgress(params: TaskProgressParams) {
    onBuildEvent()
  }

  override fun onBuildTaskFinish(params: TaskFinishParams) {
    onBuildEvent()
    when (params.dataKind) {
      TaskFinishDataKind.TestFinish -> {
        val testFinish = Json.decodeFromJsonElement<TestFinish>(params.data!!)
        val isSuite = if (params.message.isNullOrBlank()) false else params.message!!.take(3) == "<S>"
        when (testFinish.status) {
          TestStatus.Failed -> bspTestConsole.failTest(testFinish.displayName, testFinish.message.orEmpty())
          TestStatus.Passed -> bspTestConsole.passTest(isSuite, testFinish.displayName)
          else -> bspTestConsole.ignoreTest(testFinish.displayName)
        }
      }
    }
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
    onBuildEvent()
    addDiagnosticToConsole(params)
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget) {
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
    if (params.originId != null) {
      val targetConsole = if (params.originId?.startsWith("build") == true) bspBuildConsole else bspSyncConsole
      params.diagnostics.forEach {
        targetConsole.addDiagnosticMessage(
          params.originId!!,
          params.textDocument.uri,
          it.range.start.line,
          it.range.start.character,
          it.message,
          getMessageEventKind(it.severity)
        )
      }
    }
  }

  private fun getMessageEventKind(severity: DiagnosticSeverity?): MessageEvent.Kind =
    when (severity) {
      DiagnosticSeverity.Error -> MessageEvent.Kind.ERROR
      DiagnosticSeverity.Warning -> MessageEvent.Kind.WARNING
      DiagnosticSeverity.Information -> MessageEvent.Kind.INFO
      DiagnosticSeverity.Hint -> MessageEvent.Kind.INFO
      null -> MessageEvent.Kind.SIMPLE
    }
}

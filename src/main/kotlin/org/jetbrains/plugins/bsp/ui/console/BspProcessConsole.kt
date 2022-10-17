package org.jetbrains.plugins.bsp.ui.console

import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.FilePosition
import com.intellij.build.events.EventResult
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FileMessageEventImpl
import com.intellij.build.events.impl.FinishBuildEventImpl
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.events.impl.ProgressBuildEventImpl
import com.intellij.build.events.impl.StartBuildEventImpl
import com.intellij.build.events.impl.SuccessResultImpl
import java.io.File
import java.net.URI

public class BspProcessConsole (
  private val processView: BuildProgressListener,
  private val basePath: String
  ) {

  private val processesInProgress: MutableList<String> = mutableListOf()

  /**
   * Displays start of a process in this console.
   * Will not display anything if a process with given `processId` is already running
   *
   * @param title process title which will be displayed in the console
   * @param message message informing about the start of the process
   * @param processId ID of the newly started process
   */
  @Synchronized
  public fun startTask(title: String, message: String, processId: String): Unit =
    doUnlessProcessInProgress(processId) {
      processesInProgress.add(processId)
      doStartTask(title, message, processId)
    }

  private fun doStartTask(title: String, message: String, processId: String) {
    val processDescriptor = DefaultBuildDescriptor(processId, title, basePath, System.currentTimeMillis())
    // TODO one day
    //  .withRestartActions(restartAction)

    val startEvent = StartBuildEventImpl(processDescriptor, message)
    processView.onEvent(processId, startEvent)
  }

  /**
   * Displays finish of a process in this console.
   * Will not display anything if a process with given `processId` is not running or if `null` was passed
   *
   * @param message message informing about the start of the process
   * @param result result of the process execution (success by default, if nothing passed)
   * @param processId ID of the finished process (last started process by default, if nothing passed)
   */
  @Synchronized
  public fun finishTask(
    message: String,
    result: EventResult = SuccessResultImpl(),
    processId: String? = processesInProgress.lastOrNull()
  ): Unit =
    doIfProcessInProgress(processId) {
      processesInProgress.remove(processId)
      doFinishTask(message, result, processId!!)
    }

  private fun doFinishTask(message: String, result: EventResult, processId: String) {
    val event = FinishBuildEventImpl(processId, null, System.currentTimeMillis(), message, result)
    processView.onEvent(processId, event)
  }

  @Synchronized
  public fun startSubtask(id: Any, message: String, processId: String? = processesInProgress.lastOrNull()): Unit =
    doIfProcessInProgress(processId) {
      val event = ProgressBuildEventImpl(id, processId, System.currentTimeMillis(), message, -1, -1, "unit")
      processView.onEvent(processId!!, event)
    }

  @Synchronized
  public fun finishSubtask(id: Any, message: String, processId: String? = processesInProgress.lastOrNull()): Unit =
    doIfProcessInProgress(processId) {
      val event = FinishBuildEventImpl(id, null, System.currentTimeMillis(), message, SuccessResultImpl())
      processView.onEvent(processId!!, event)
    }

  @Synchronized
  public fun addDiagnosticMessage(params: PublishDiagnosticsParams) {
    params.diagnostics.forEach {
      if (it.message.isNotBlank()) {
        val messageToSend = prepareTextToPrint(it.message)
        val event = FileMessageEventImpl(
          params.originId,
          MessageEvent.Kind.ERROR,
          null,
          messageToSend,
          null,
          FilePosition(File(URI(params.textDocument.uri)), it.range.start.line, it.range.start.character)
        )
        processView.onEvent(params.originId, event)
      }
    }
  }

  @Synchronized
  public fun addMessage(id: Any?, message: String, processId: String? = processesInProgress.lastOrNull()): Unit =
    doIfProcessInProgress(processId) {
      if (message.isNotBlank()) {
        val messageToSend = prepareTextToPrint(message)
        doAddMessage(id, messageToSend, processId!!)
      }
    }

  private fun doAddMessage(id: Any?, message: String, processId: String) {
    val event = OutputBuildEventImpl(id, message, true)
    processView.onEvent(processId, event)
  }

  @Synchronized
  public fun addWarning() {
    // TODO
  }

  private inline fun doIfProcessInProgress(processId: String?, action: () -> Unit) {
    if (processId != null && processesInProgress.contains(processId)) {
      action()
    }
  }


  private inline fun doUnlessProcessInProgress(processId: String?, action: () -> Unit) {
    if (processId != null && !processesInProgress.contains(processId)) {
      action()
    }
  }

  private fun prepareTextToPrint(text: String): String =
    if (text.endsWith("\n")) text else text + "\n"
}

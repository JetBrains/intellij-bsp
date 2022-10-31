package org.jetbrains.plugins.bsp.ui.console

import ch.epfl.scala.bsp4j.DiagnosticSeverity
import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.FilePosition
import com.intellij.build.events.EventResult
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.*
import java.io.File
import java.net.URI

public class TaskConsole(
  private val taskView: BuildProgressListener,
  private val basePath: String
) {

  private val tasksInProgress: MutableList<Any> = mutableListOf()
  private val subtaskParentMap: MutableMap<Any, Any> = mutableMapOf()

  /**
   * Displays start of a task in this console.
   * Will not display anything if a task with given `taskId` is already running
   *
   * @param title task title which will be displayed in the console
   * @param message message informing about the start of the task
   * @param taskId ID of the newly started task
   */
  @Synchronized
  public fun startTask(taskId: Any, title: String, message: String): Unit =
    doUnlessTaskInProgress(taskId) {
      tasksInProgress.add(taskId)
      doStartTask(taskId, "BSP: $title", message)
    }

  private fun doStartTask(taskId: Any, title: String, message: String) {
    val taskDescriptor = DefaultBuildDescriptor(taskId, title, basePath, System.currentTimeMillis())
    val startEvent = StartBuildEventImpl(taskDescriptor, message)
    taskView.onEvent(taskId, startEvent)
  }

  /**
   * Displays finish of a task in this console.
   * Will not display anything if a task with given `taskId` is not running or if `null` was passed
   *
   * @param message message informing about the start of the task
   * @param result result of the task execution (success by default, if nothing passed)
   * @param taskId ID of the finished task (last started task by default, if nothing passed)
   */
  @Synchronized
  public fun finishTask(
    taskId: Any,
    message: String,
    result: EventResult = SuccessResultImpl()
  ): Unit =
    doIfTaskInProgress(taskId) {
      doFinishTask(taskId, message, result)
    }

  private fun doFinishTask(taskId: Any, message: String, result: EventResult) {
    tasksInProgress.remove(taskId)
    subtaskParentMap.entries.removeAll { it.value == taskId }
    val event = FinishBuildEventImpl(taskId, null, System.currentTimeMillis(), message, result)
    taskView.onEvent(taskId, event)
  }

  /**
   * Displays start of a subtask in this console
   *
   * @param parentTaskId id of the task being this subtask's parent
   * @param subtaskId id of the newly created subtask. **Has to be unique among all running subtasks in
   * this console - otherwise, unexpected behavior might occur**
   * @param message will be displayed as this subtask's title until it's finished
   */
  @Synchronized
  public fun startSubtask(parentTaskId: Any, subtaskId: Any, message: String): Unit =
    doIfTaskInProgress(parentTaskId) {
      doStartSubtask(parentTaskId, subtaskId, message)
    }

  private fun doStartSubtask(parentTaskId: Any, subtaskId: Any, message: String) {
    subtaskParentMap[subtaskId] = parentTaskId
    val event = ProgressBuildEventImpl(subtaskId, parentTaskId, System.currentTimeMillis(), message, -1, -1, "")
    taskView.onEvent(parentTaskId, event)
  }

  /**
   * Displays finishing of a subtask in this console
   *
   * @param subtaskId id of the subtask to be finished.
   * If there is no such subtask running, this method will not do anything
   * @param message will be displayed as this subtask's title after it is finished
   */
  @Synchronized
  public fun finishSubtask(subtaskId: Any, message: String) {
    if (subtaskParentMap.containsKey(subtaskId)) {
      val taskId = subtaskParentMap[subtaskId]
      doIfTaskInProgress(taskId) {
        doFinishSubtask(taskId!!, subtaskId, message)
      }
    }
  }

  private fun doFinishSubtask(taskId: Any, subtaskId: Any, message: String) {
    subtaskParentMap.remove(subtaskId)
    val event = FinishBuildEventImpl(subtaskId, null, System.currentTimeMillis(), message, SuccessResultImpl())
    taskView.onEvent(taskId, event)
  }

  /**
   * Adds a diagnostic message to a particular task in this console.
   *
   * @param taskId id of the task (or a subtask), to which the message will be added
   * @param fileURI absolute path to the file concerned by the diagnostic
   * @param line line number in given file (first line is 0)
   * @param column column number in given file (first column is 0)
   * @param message description of the diagnostic
   * @param severity severity of the diagnostic
   */
  @Synchronized
  public fun addDiagnosticMessage(
    taskId: Any,
    fileURI: String,
    line: Int,
    column: Int,
    message: String,
    severity: DiagnosticSeverity?
  ) {
    val parentTaskId = getSubtaskParent(taskId)
    val subtaskId =
      if (tasksInProgress.contains(taskId)) parentTaskId else taskId
    doIfTaskInProgress(parentTaskId) {
      if (message.isNotBlank()) {
        val messageToSend = prepareTextToPrint(message)
        val fullFileURI = if (fileURI.startsWith("file://")) fileURI else "file://$fileURI"
        val event = FileMessageEventImpl(
          subtaskId!!,
          getMessageEventKind(severity),
          null,
          messageToSend,
          null,
          FilePosition(File(URI(fullFileURI)), line, column)
        )
        taskView.onEvent(parentTaskId!!, event)
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

  /**
   * Adds a message to a particular task in this console. If the message is added to a subtask, it will also be
   * added to the subtask's parent task.
   *
   * @param taskId id of the task (or a subtask), to which the message will be added
   * @param message message to be added. New line will be inserted at its end if it's not present there already
   */
  @Synchronized
  public fun addMessage(taskId: Any, message: String) {
    val parentTaskId = getSubtaskParent(taskId)
    doIfTaskInProgress(parentTaskId) {
      if (message.isNotBlank()) {
        doAddMessage(parentTaskId!!, taskId, message)
      }
    }
  }

  private fun doAddMessage(parentTaskId: Any, taskId: Any, message: String) {
    val subtaskId =
      if (tasksInProgress.contains(taskId)) null else taskId
    val messageToSend = prepareTextToPrint(message)
    sendMessageEvent(parentTaskId, subtaskId, messageToSend)  // send message to the subtask
    if (subtaskId != null) sendMessageEvent(parentTaskId, null, messageToSend)  // send message to the parent
  }

  private fun sendMessageEvent(parentTaskId: Any, subtaskId: Any?, message: String) {
    val event = OutputBuildEventImpl(subtaskId, message, true)
    taskView.onEvent(parentTaskId, event)
  }

  private inline fun doIfTaskInProgress(taskId: Any?, action: () -> Unit) {
    if (taskId != null && tasksInProgress.contains(taskId)) {
      action()
    }
  }

  private inline fun doUnlessTaskInProgress(taskId: Any?, action: () -> Unit) {
    if (taskId != null && !tasksInProgress.contains(taskId)) {
      action()
    }
  }

  private fun prepareTextToPrint(text: String): String =
    if (text.endsWith("\n")) text else text + "\n"

  private fun getSubtaskParent(taskId: Any): Any? =
    if (tasksInProgress.contains(taskId)) taskId else subtaskParentMap[taskId]
}

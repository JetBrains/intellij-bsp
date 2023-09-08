package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.alsoIfNull

internal typealias OriginId = String

internal typealias TaskId = String

internal interface BspTaskListener {
  fun onDiagnostic(taskId: TaskId, severity: String, message: String)
  fun onOutput(text: String)
  fun onExit(code: Int)

  fun onTaskStart(taskId: TaskId, message: String)
  fun onSubtaskStart(taskId: TaskId, parentId: TaskId, message: String)
  fun onTaskProgress(taskId: TaskId, message: String)
  fun onTaskFinish(taskId: TaskId, message: String)
  fun onTaskFailed(taskId: TaskId, message: String)
  fun onTaskIgnored(taskId: TaskId, message: String)
}

@Service
internal class BspTaskEventsService {
  private val log = logger<BspTaskEventsService>()

  private val taskListeners: MutableMap<OriginId, BspTaskListener> = mutableMapOf()

  fun startTestTask(originId: OriginId, listener: BspTaskListener) {
    taskListeners[originId] = listener
  }

  private fun get(id: OriginId): BspTaskListener? = taskListeners[id].alsoIfNull {
    log.warn("No task listener found for task $id")
  }

  fun withListener(id: OriginId, block: BspTaskListener.() -> Unit) {
    get(id)?.also { it.block() }
  }

}
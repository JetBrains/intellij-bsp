package org.jetbrains.plugins.bsp.services

import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.alsoIfNull

internal typealias OriginId = String

internal typealias TaskId = String

internal interface BspTaskListener {
  fun onDiagnostic(textDocument: String, buildTarget: String, line: Int, character: Int, severity: MessageEvent.Kind, message: String) {}
  fun onOutputStream(taskId: TaskId?, text: String) {}
  fun onErrorStream(taskId: TaskId?, text: String) {}
  fun onExit(code: Int) {}

  fun onTaskStart(taskId: TaskId, parentId: TaskId?, message: String, data: Any?) {}
  fun onTaskProgress(taskId: TaskId, message: String, data: Any?) {}
  fun onTaskFinish(taskId: TaskId, message: String, status: StatusCode, data: Any?) {}

  fun onLogMessage(message: String) {}
  fun onShowMessage(message: String) {}
}

@Service(Service.Level.PROJECT)
internal class BspTaskEventsService {
  private val log = logger<BspTaskEventsService>()

  private val taskListeners: MutableMap<OriginId, BspTaskListener> = mutableMapOf()

  private fun get(id: OriginId): BspTaskListener? {
    val listener = taskListeners[id]
    if (listener == null) {
      log.warn("No task listener found for task $id")
    }
    return listener
  }

  fun addListener(id: OriginId, listener: BspTaskListener) {
    taskListeners[id] = listener
  }

  fun withListener(id: OriginId, block: BspTaskListener.() -> Unit) {
    get(id)?.also { it.block() }
  }

  fun removeListener(id: OriginId) {
    taskListeners.remove(id)
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project) = project.service<BspTaskEventsService>()
  }

}
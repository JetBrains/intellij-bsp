package org.jetbrains.plugins.bsp.ui.console

import ch.epfl.scala.bsp4j.*
import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.MessageEvent.Kind
import com.intellij.build.events.impl.*
import io.kotest.matchers.maps.shouldContainExactly
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

private data class TestableBuildEvent(
  val eventType: KClass<*>,
  val id: Any?,
  val parentId: Any?,
  val message: String,
)

private class MockBuildProgressListener : BuildProgressListener {

  val events: MutableMap<Any, List<TestableBuildEvent>> = mutableMapOf()

  override fun onEvent(buildId: Any, event: BuildEvent) {
    addEvent(buildId, sanitizeEvent(event))
  }

  private fun sanitizeEvent(eventToSanitize: BuildEvent): TestableBuildEvent = when (eventToSanitize) {
    is OutputBuildEventImpl -> TestableBuildEvent(
      eventToSanitize::class,
      null,
      eventToSanitize.parentId,
      eventToSanitize.message
    )

    else -> TestableBuildEvent(
      eventToSanitize::class,
      eventToSanitize.id,
      eventToSanitize.parentId,
      eventToSanitize.message
    )
  }

  private fun addEvent(buildId: Any, event: TestableBuildEvent) {
    events.merge(buildId, listOf(event)) { acc, x -> acc + x }
  }
}

class TaskConsoleTest {
  @Test
  fun `should start the task, start 3 subtasks, put 2 messages and for each subtask and finish the task (the happy path)`() {
    // given
    val buildProcessListener = MockBuildProgressListener()
    val basePath = "/project/"
    // when
    val taskConsole = TaskConsole(buildProcessListener, basePath)

    taskConsole.addMessage("task before start", "message before start - should be omitted")

    taskConsole.startTask("task", "Task", "Testing...")

    taskConsole.startSubtask("task", "subtask 1", "Starting subtask 1")
    taskConsole.addMessage("subtask 1", "message 1\n")
    taskConsole.addMessage("subtask 1", "message 2\n")
    taskConsole.finishSubtask("subtask 1", "Subtask 1 finished")

    taskConsole.startSubtask("task", "subtask 2", "Starting subtask 2")
    taskConsole.addMessage("subtask 2", "message 3")
    taskConsole.addMessage("subtask 2", "message 4")

    taskConsole.startSubtask("task", "subtask 3", "Starting subtask 3")
    taskConsole.addMessage("subtask 3", "message 5")
    taskConsole.addMessage("subtask 3", "message 6")

    taskConsole.finishSubtask("subtask 2", "Subtask 2 finished")
    taskConsole.finishSubtask("subtask 3", "Subtask 3 finished")

    taskConsole.finishTask("task", "Finished!", SuccessResultImpl())

    // then
    buildProcessListener.events shouldContainExactly mapOf(
      "task" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "task", null, "Testing..."),

        TestableBuildEvent(ProgressBuildEventImpl::class, "subtask 1", "task", "Starting subtask 1"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 1", "message 1\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, null, "message 1\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 1", "message 2\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, null, "message 2\n"),
        TestableBuildEvent(FinishBuildEventImpl::class, "subtask 1", null, "Subtask 1 finished"),

        TestableBuildEvent(ProgressBuildEventImpl::class, "subtask 2", "task", "Starting subtask 2"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 2", "message 3\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, null, "message 3\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 2", "message 4\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, null, "message 4\n"),

        TestableBuildEvent(ProgressBuildEventImpl::class, "subtask 3", "task", "Starting subtask 3"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 3", "message 5\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, null, "message 5\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 3", "message 6\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, null, "message 6\n"),

        TestableBuildEvent(FinishBuildEventImpl::class, "subtask 2", null, "Subtask 2 finished"),
        TestableBuildEvent(FinishBuildEventImpl::class, "subtask 3", null, "Subtask 3 finished"),

        TestableBuildEvent(FinishBuildEventImpl::class, "task", null, "Finished!"),
      )
    )
  }

  @Test
  fun `should start multiple processes and finish them`() {
    val buildProcessListener = MockBuildProgressListener()
    val basePath = "/project/"

    // when
    val taskConsole = TaskConsole(buildProcessListener, basePath)

    taskConsole.startTask("task-1", "Task 1", "Processing...")
    taskConsole.startTask("task-2", "Task 2", "Processing...")
    taskConsole.startTask("task-3", "Task 3", "Processing...")
    taskConsole.finishTask("task-2", "Task 2 done!", SuccessResultImpl())
    taskConsole.finishTask("task-3", "Task 3 done!", SuccessResultImpl())
    taskConsole.finishTask("task-1", "Task 1 done!", SuccessResultImpl())

    // then
    buildProcessListener.events shouldContainExactly mapOf(
      "task-1" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "task-1", null, "Processing..."),
        TestableBuildEvent(FinishBuildEventImpl::class, "task-1", null, "Task 1 done!"),
      ),
      "task-2" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "task-2", null, "Processing..."),
        TestableBuildEvent(FinishBuildEventImpl::class, "task-2", null, "Task 2 done!"),
      ),
      "task-3" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "task-3", null, "Processing..."),
        TestableBuildEvent(FinishBuildEventImpl::class, "task-3", null, "Task 3 done!"),
      ),
    )
  }

  @Test
  fun `should ignore invalid Task events`() {
    val buildProcessListener = MockBuildProgressListener()
    val basePath = "/project/"

    // when
    val taskConsole = TaskConsole(buildProcessListener, basePath)

    taskConsole.startTask("task-1", "Task 1", "Processing...")
    taskConsole.startTask("task-1", "Task 1", "This event should be ignored")
    taskConsole.finishTask("task-77", "This event should be ignored", SuccessResultImpl())
    taskConsole.finishTask("task-1", "Task 1 done!", SuccessResultImpl())
    taskConsole.finishTask("task-1", "This event should be ignored", SuccessResultImpl())

    // then
    buildProcessListener.events shouldContainExactly mapOf(
      "task-1" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "task-1", null, "Processing..."),
        TestableBuildEvent(FinishBuildEventImpl::class, "task-1", null, "Task 1 done!"),
      )
    )
  }
  
  @Test
  fun `should display messages correctly`() {
    val buildProcessListener = MockBuildProgressListener()
    val basePath = "/project/"

    // when
    val taskConsole = TaskConsole(buildProcessListener, basePath)

    taskConsole.addMessage("task", "Message 0")  // should be omitted - task not yet started
    
    taskConsole.startTask("task", "Task 1", "Task started")
    taskConsole.startSubtask("task", "subtask", "Subtask started")

    taskConsole.addMessage("task", "Message 1\n")
    taskConsole.addMessage("task", "Message 2")  // should add new line at the end
    taskConsole.addMessage("subtask", "Message 3")  // should send a copy the message to the subtask's parent
    taskConsole.addMessage("nonexistent-task", "Message 4")  // should be omitted - no such task
    taskConsole.addMessage("task", "")  // should be omitted - empty message
    taskConsole.addMessage("task", "   \n  \t  ")  // should be omitted - blank message

    taskConsole.finishSubtask("subtask", "Subtask finished")
    taskConsole.finishTask("task", "Task finished")

    taskConsole.addMessage("task", "Message 7")  // should be omitted - task already finished

    // then
    buildProcessListener.events shouldContainExactly mapOf(
      "task" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "task", null, "Task started"),
        TestableBuildEvent(ProgressBuildEventImpl::class, "subtask", "task", "Subtask started"),

        TestableBuildEvent(OutputBuildEventImpl::class, null, null, "Message 1\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, null, "Message 2\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask", "Message 3\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, null, "Message 3\n"),

        TestableBuildEvent(FinishBuildEventImpl::class, "subtask", null, "Subtask finished"),
        TestableBuildEvent(FinishBuildEventImpl::class, "task", null, "Task finished"),
      )
    )
  }

  @Test
  fun `should display diagnostic messages correctly`() {
    val basePath = "/project/"
    val fileURI = "file:///home/directory/project/src/test/Start.kt"

    data class SanitizedDiagnosticEvent(
      val originId: Any?,
      val message: String,
      val severity: Kind,
      val filePositionPath: String,
      val line: Int,
      val column: Int
    )

    class DiagnosticListener : BuildProgressListener {
      val events = mutableMapOf<Any, List<SanitizedDiagnosticEvent?>>()

      override fun onEvent(buildId: Any, event: BuildEvent) {
        val sanitizedEvent = (event as? FileMessageEventImpl)?.let {
          SanitizedDiagnosticEvent(
            it.parentId,
            it.message,
            it.kind,
            it.filePosition.file.absolutePath,
            it.filePosition.startLine,
            it.filePosition.startColumn
          )
        }
        addEvent(buildId, sanitizedEvent)
      }

      private fun addEvent(buildId: Any, event: SanitizedDiagnosticEvent?) {
        events.merge(buildId, listOf(event)) { acc, x -> acc + x }
      }
    }

    val diagnosticListener = DiagnosticListener()
    val taskConsole = TaskConsole(diagnosticListener, basePath)

    // when
    taskConsole.startTask("origin", "Test", "started")

    taskConsole.addDiagnosticMessage("origin", fileURI, 10, 20, "Diagnostic 1", DiagnosticSeverity.ERROR)
    taskConsole.addDiagnosticMessage("origin", fileURI, 10, 20, "Diagnostic 2", DiagnosticSeverity.WARNING)
    taskConsole.addDiagnosticMessage("origin", fileURI, 10, 20, "Diagnostic 3", DiagnosticSeverity.INFORMATION)
    taskConsole.addDiagnosticMessage("origin", fileURI, 10, 20, "Diagnostic 4", DiagnosticSeverity.HINT)

    // blank message, should be omitted
    taskConsole.addDiagnosticMessage("origin", fileURI, 10, 20, "\t    \n   ", DiagnosticSeverity.ERROR)

    // non-existent originId, should be omitted
    taskConsole.addDiagnosticMessage("wrong", fileURI, 10, 20, "Diagnostic 6", DiagnosticSeverity.ERROR)

    // null as severity, should be sent correctly
    taskConsole.addDiagnosticMessage("origin", fileURI, 10, 20, "Diagnostic 7", null)

    // negative line and column numbers, should be sent nevertheless
    taskConsole.addDiagnosticMessage("origin", fileURI, -4, -8, "Diagnostic 8", DiagnosticSeverity.ERROR)

    // fileURI without `file://`, should be sent correctly
    taskConsole.addDiagnosticMessage("origin", "/home/directory/project/src/test/Start.kt", 10, 20, "Diagnostic 9", DiagnosticSeverity.WARNING)

    taskConsole.finishTask("origin", "finished", SuccessResultImpl())

    // then
    diagnosticListener.events shouldContainExactly mapOf(
      "origin" to listOf(
        null,  // starting the task
        SanitizedDiagnosticEvent(originId="origin", message="Diagnostic 1\n", severity=Kind.ERROR, filePositionPath="/home/directory/project/src/test/Start.kt", 10, 20),
        SanitizedDiagnosticEvent(originId="origin", message="Diagnostic 2\n", severity=Kind.WARNING, filePositionPath="/home/directory/project/src/test/Start.kt", 10, 20),
        SanitizedDiagnosticEvent(originId="origin", message="Diagnostic 3\n", severity=Kind.INFO, filePositionPath="/home/directory/project/src/test/Start.kt", 10, 20),
        SanitizedDiagnosticEvent(originId="origin", message="Diagnostic 4\n", severity=Kind.INFO, filePositionPath="/home/directory/project/src/test/Start.kt", 10, 20),
        SanitizedDiagnosticEvent(originId="origin", message="Diagnostic 7\n", severity=Kind.SIMPLE, filePositionPath="/home/directory/project/src/test/Start.kt", 10, 20),
        SanitizedDiagnosticEvent(originId="origin", message="Diagnostic 8\n", severity=Kind.ERROR, filePositionPath="/home/directory/project/src/test/Start.kt", -4, -8),
        SanitizedDiagnosticEvent(originId="origin", message="Diagnostic 9\n", severity=Kind.WARNING, filePositionPath="/home/directory/project/src/test/Start.kt", 10, 20),
        null  // finishing the task
      )
    )
  }
}

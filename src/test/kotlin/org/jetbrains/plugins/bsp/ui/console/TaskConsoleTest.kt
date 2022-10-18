package org.jetbrains.plugins.bsp.ui.console

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.impl.FinishBuildEventImpl
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.events.impl.ProgressBuildEventImpl
import com.intellij.build.events.impl.StartBuildEventImpl
import com.intellij.build.events.impl.SuccessResultImpl
import io.kotest.matchers.maps.shouldContainExactly
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

private data class TestableBuildEvent(
  val eventType: KClass<*>,
  val id: Any?,
  val parentId: Any?,
  val message: String,
)

private class BuildProgressListenerTestMock : BuildProgressListener {

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
  fun `should start the import, start 3 subtask, put 2 messages and for each subtask and finish the import (the happy path)`() {
    // given
    val buildProcessListener = BuildProgressListenerTestMock()
    val basePath = "/project/"
    // when
    val taskConsole = TaskConsole(buildProcessListener, basePath)

    taskConsole.addMessage("task before start", "message before start - should be omitted")

    taskConsole.startTask("import", "Import", "Importing...")

    taskConsole.startSubtask("import", "subtask 1", "Starting subtask 1")
    taskConsole.addMessage("subtask 1", "message 1\n")
    taskConsole.addMessage("subtask 1", "message 2") // should add new line at the end
    taskConsole.finishSubtask("subtask 1", "Subtask 1 finished")

    taskConsole.startSubtask("import", "subtask 2", "Starting subtask 2")
    taskConsole.addMessage("subtask 2", "message 3\n")
    taskConsole.addMessage("subtask 2", "") // should be omitted - empty string

    taskConsole.startSubtask("import", "subtask 3", "Starting subtask 3")
    taskConsole.addMessage("subtask 3", "message 4")
    taskConsole.addMessage("subtask 3", "      ") // should be omitted - blank string

    taskConsole.addMessage(null, "message 5")

    taskConsole.addMessage("subtask 2", "message 6\n")
    taskConsole.addMessage("subtask 3", "message 7\n")

    taskConsole.finishSubtask("subtask 2", "Subtask 2 finished")
    taskConsole.finishSubtask("subtask 3", "Subtask 3 finished")

    taskConsole.finishTask("import", "finitio!", SuccessResultImpl())

    taskConsole.addMessage("task after finish", "message after finish - should be omitted")

    // then
    buildProcessListener.events shouldContainExactly mapOf(
      "import" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "import", null, "Importing..."),

        TestableBuildEvent(ProgressBuildEventImpl::class, "subtask 1", "import", "Starting subtask 1"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 1", "message 1\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 1", "message 2\n"),
        TestableBuildEvent(FinishBuildEventImpl::class, "subtask 1", null, "Subtask 1 finished"),

        TestableBuildEvent(ProgressBuildEventImpl::class, "subtask 2", "import", "Starting subtask 2"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 2", "message 3\n"),

        TestableBuildEvent(ProgressBuildEventImpl::class, "subtask 3", "import", "Starting subtask 3"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 3", "message 4\n"),

        TestableBuildEvent(OutputBuildEventImpl::class, null, null, "message 5\n"),

        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 2", "message 6\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 3", "message 7\n"),

        TestableBuildEvent(FinishBuildEventImpl::class, "subtask 2", null, "Subtask 2 finished"),
        TestableBuildEvent(FinishBuildEventImpl::class, "subtask 3", null, "Subtask 3 finished"),

        TestableBuildEvent(FinishBuildEventImpl::class, "import", null, "finitio!"),
      )
    )
  }

  @Test
  fun `should start multiple processes and finish them`() {
    val buildProcessListener = BuildProgressListenerTestMock()
    val basePath = "/project/"

    // when
    val taskConsole = TaskConsole(buildProcessListener, basePath)

    taskConsole.startTask("process-1", "Process 1", "Processing...")
    taskConsole.startTask("process-2", "Process 2", "Processing...")
    taskConsole.startTask("process-3", "Process 3", "Processing...")
    taskConsole.finishTask("process-2", "Process 2 done!", SuccessResultImpl())
    taskConsole.finishTask("process-3", "Process 3 done!", SuccessResultImpl())
    taskConsole.finishTask("process-1", "Process 1 done!", SuccessResultImpl())

    // then
    buildProcessListener.events shouldContainExactly mapOf(
      "process-1" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "process-1", null, "Processing..."),
        TestableBuildEvent(FinishBuildEventImpl::class, "process-1", null, "Process 1 done!"),
      ),
      "process-2" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "process-2", null, "Processing..."),
        TestableBuildEvent(FinishBuildEventImpl::class, "process-2", null, "Process 2 done!"),
      ),
      "process-3" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "process-3", null, "Processing..."),
        TestableBuildEvent(FinishBuildEventImpl::class, "process-3", null, "Process 3 done!"),
      ),
    )
  }

  @Test
  fun `should ignore invalid process events`() {
    val buildProcessListener = BuildProgressListenerTestMock()
    val basePath = "/project/"

    // when
    val taskConsole = TaskConsole(buildProcessListener, basePath)

    taskConsole.startTask("process-1", "Process 1", "Processing...")
    taskConsole.startTask("process-1", "Process 1", "This event should be ignored")
    taskConsole.finishTask("process-77", "This event should be ignored", SuccessResultImpl())
    taskConsole.finishTask("process-1", "Process 1 done!", SuccessResultImpl())
    taskConsole.finishTask("process-1", "This event should be ignored", SuccessResultImpl())

    // then
    buildProcessListener.events shouldContainExactly mapOf(
      "process-1" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "process-1", null, "Processing..."),
        TestableBuildEvent(FinishBuildEventImpl::class, "process-1", null, "Process 1 done!"),
      )
    )
  }
}

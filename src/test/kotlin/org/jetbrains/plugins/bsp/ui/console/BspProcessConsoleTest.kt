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

class BspProcessConsoleTest {
  @Test
  fun `should start the import, start 3 subtask, put 2 messages and for each subtask and finish the import (the happy path)`() {
    // given
    val buildProcessListener = BuildProgressListenerTestMock()
    val basePath = "/project/"
    // when
    val bspProcessConsole = BspProcessConsole(buildProcessListener, basePath)

    bspProcessConsole.addMessage("task before start", "message before start - should be omitted")

    bspProcessConsole.startProcess("Import", "Importing...", "import")

    bspProcessConsole.startSubtask("subtask 1", "Starting subtask 1")
    bspProcessConsole.addMessage("subtask 1", "message 1\n")
    bspProcessConsole.addMessage("subtask 1", "message 2") // should add new line at the end
    bspProcessConsole.finishSubtask("subtask 1", "Subtask 1 finished")

    bspProcessConsole.startSubtask("subtask 2", "Starting subtask 2")
    bspProcessConsole.addMessage("subtask 2", "message 3\n")
    bspProcessConsole.addMessage("subtask 2", "") // should be omitted - empty string

    bspProcessConsole.startSubtask("subtask 3", "Starting subtask 3")
    bspProcessConsole.addMessage("subtask 3", "message 4")
    bspProcessConsole.addMessage("subtask 3", "      ") // should be omitted - blank string

    bspProcessConsole.addMessage(null, "message 5")

    bspProcessConsole.addMessage("subtask 2", "message 6\n")
    bspProcessConsole.addMessage("subtask 3", "message 7\n")

    bspProcessConsole.finishSubtask("subtask 2", "Subtask 2 finished")
    bspProcessConsole.finishSubtask("subtask 3", "Subtask 3 finished")

    bspProcessConsole.finishProcess("finitio!", SuccessResultImpl())

    bspProcessConsole.addMessage("task after finish", "message after finish - should be omitted")

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
    val bspProcessConsole = BspProcessConsole(buildProcessListener, basePath)

    bspProcessConsole.startProcess("Process 1", "Processing...", "process-1")
    bspProcessConsole.startProcess("Process 2", "Processing...", "process-2")
    bspProcessConsole.startProcess("Process 3", "Processing...", "process-3")
    bspProcessConsole.finishProcess("Process 2 done!", SuccessResultImpl(), "process-2")
    bspProcessConsole.finishProcess("Process 3 done!", SuccessResultImpl())
    bspProcessConsole.finishProcess("Process 1 done!", SuccessResultImpl())

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
    val bspProcessConsole = BspProcessConsole(buildProcessListener, basePath)

    bspProcessConsole.startProcess("Process 1", "Processing...", "process-1")
    bspProcessConsole.startProcess("Process 1", "This event should be ignored", "process-1")
    bspProcessConsole.finishProcess("This event should be ignored", SuccessResultImpl(), "process-77")
    bspProcessConsole.finishProcess("Process 1 done!", SuccessResultImpl(), "process-1")
    bspProcessConsole.finishProcess("This event should be ignored", SuccessResultImpl(), "process-1")
    bspProcessConsole.finishProcess("This event should be ignored", SuccessResultImpl())

    // then
    buildProcessListener.events shouldContainExactly mapOf(
      "process-1" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "process-1", null, "Processing..."),
        TestableBuildEvent(FinishBuildEventImpl::class, "process-1", null, "Process 1 done!"),
      )
    )
  }
}

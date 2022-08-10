package org.jetbrains.plugins.bsp.ui.console

import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.impl.FinishBuildEventImpl
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.events.impl.ProgressBuildEventImpl
import com.intellij.build.events.impl.StartBuildEventImpl
import com.intellij.build.events.impl.SuccessResultImpl

public class BspBuildConsole(private val buildView: BuildProgressListener, private val basePath: String) {

  private var buildId: Any = "needToStartId"
  private var inProgress: Boolean = false

  @Synchronized
  public fun startBuild(buildId: Any, title: String, message: String): Unit = doUnlessBuildInProcess {
    this.buildId = buildId
    this.inProgress = true

    doStartBuild(buildId, title, message)
  }

  private fun doStartBuild(buildId: Any, title: String, message: String) {
    val buildDescriptor = DefaultBuildDescriptor(buildId, title, basePath, System.currentTimeMillis())
    // TODO one day
    //  .withRestartActions(restartAction)

    val startEvent = StartBuildEventImpl(buildDescriptor, message)
    buildView.onEvent(buildId, startEvent)
  }

  @Synchronized
  public fun finishBuild(message: String): Unit = doIfBuildInProcess {
    this.inProgress = false

    doFinishBuild(message)
  }

  private fun doFinishBuild(message: String) {
    val event = FinishBuildEventImpl(buildId, null, System.currentTimeMillis(), message, SuccessResultImpl())
    buildView.onEvent(buildId, event)
  }

  @Synchronized
  public fun startSubtask(id: Any, message: String): Unit = doIfBuildInProcess {
    val event = ProgressBuildEventImpl(id, buildId, System.currentTimeMillis(), message, -1, -1, "unit")
    buildView.onEvent(buildId, event)
  }

  @Synchronized
  public fun finishSubtask(id: Any, message: String): Unit = doIfBuildInProcess {
    val event = FinishBuildEventImpl(id, null, System.currentTimeMillis(), message, SuccessResultImpl())
    buildView.onEvent(buildId, event)
  }

  @Synchronized
  public fun addMessage(id: Any?, message: String): Unit = doIfBuildInProcess {
    if (message.isNotBlank()) {
      val messageToSend = prepareTextToPrint(message)

      doAddMessage(id, messageToSend)
    }
  }

  private fun doAddMessage(id: Any?, message: String) {
    val event = OutputBuildEventImpl(id, message, true)

    buildView.onEvent(buildId, event)
  }

  @Synchronized
  public fun addWarning() {
    // TODO
  }

  private inline fun doUnlessBuildInProcess(action: () -> Unit) {
    if (!inProgress) {
      action()
    }
  }

  private inline fun doIfBuildInProcess(action: () -> Unit) {
    if (inProgress) {
      action()
    }
  }

  private fun prepareTextToPrint(text: String): String =
    if (text.endsWith("\n")) text else text + "\n"
}

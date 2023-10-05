package org.jetbrains.plugins.bsp.neo

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.services.TaskId

public class BspRunStateBase(
  environment: ExecutionEnvironment,
  private val runConfiguration: BspRunConfiguration,
  private val originId: OriginId,
) : CommandLineState(environment) {
  override fun startProcess(): BspProcessHandler {
    val handler = BspProcessHandler()
    val ansiEscapeDecoder = AnsiEscapeDecoder()
    val runListener = object : BspTaskListener {
      override fun onOutputStream(taskId: TaskId?, text: String) {
        ansiEscapeDecoder.escapeText(text, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
          handler.notifyTextAvailable(s, key)
        }
      }

      override fun onErrorStream(taskId: TaskId?, text: String) {
        ansiEscapeDecoder.escapeText(text, ProcessOutputType.STDERR) { s: String, key: Key<Any> ->
          handler.notifyTextAvailable(s, key)
        }
      }

      // For compatibility with older BSP servers
      // TODO: Log messages in the correct place
      override fun onLogMessage(message: String) {
        ansiEscapeDecoder.escapeText(message, ProcessOutputType.SYSTEM) { s: String, key: Key<Any> ->
          handler.notifyTextAvailable(s, key)
        }
      }
    }

    runConfiguration.project.service<BspTaskEventsService>().addListener(originId, runListener)
    handler.startNotify()
    return handler
  }
}
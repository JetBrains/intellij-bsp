package org.jetbrains.plugins.bsp.ui.configuration

import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import java.io.OutputStream
import java.util.concurrent.CompletableFuture

public class BspProcessHandler<T>(private val requestFuture: CompletableFuture<T>) : ProcessHandler() {
//  BspConsolePrinter,

  override fun destroyProcessImpl() {
    requestFuture.cancel(true)
    super.notifyProcessTerminated(1)
  }

  override fun detachProcessImpl() {
    notifyProcessDetached()
  }

  override fun detachIsDefault(): Boolean = false

  override fun getProcessInput(): OutputStream? = null
}

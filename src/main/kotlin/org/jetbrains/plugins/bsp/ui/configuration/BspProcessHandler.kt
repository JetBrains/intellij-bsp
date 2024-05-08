package org.jetbrains.plugins.bsp.ui.configuration

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import java.io.OutputStream
import java.util.concurrent.CompletableFuture

public class BspProcessHandler<T>(private val requestFuture: CompletableFuture<T>) : ProcessHandler() {
  override fun startNotify() {
    super.startNotify()
    var thrownError: Throwable? = null
    requestFuture.handle { _, error ->
      if (error != null) {
        notifyTextAvailable(error.toString(), ProcessOutputType.STDERR)
        notifyProcessTerminated(1)
        thrownError = error
      } else {
        notifyProcessTerminated(0)
      }
    }
    // Hnadles the case when the future is already completed (because, for example, checkRunCapabilities failed)
    if (thrownError != null) {
      throw thrownError as Throwable
    }
  }

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

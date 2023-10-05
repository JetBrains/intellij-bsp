package org.jetbrains.plugins.bsp.neo

import com.intellij.execution.process.ProcessHandler
import java.io.OutputStream

public class BspProcessHandler : ProcessHandler() {


  override fun destroyProcessImpl() {
    // check if the process is running

    // ask through bsp to stop the process

    notifyProcessTerminated(1)
    TODO("Not yet implemented")
  }

  override fun detachProcessImpl() {
    // do nothing probably?
    TODO("Not yet implemented")
  }

  override fun detachIsDefault(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getProcessInput(): OutputStream? {
    return null
  }
}
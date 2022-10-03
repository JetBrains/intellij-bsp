package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.ui.run.configuration.BspConsolePrinter

public class BspConsoleService() {

  private val consoleListeners = mutableSetOf<BspConsolePrinter>()

  public fun registerPrinter(printer : BspConsolePrinter) {
    consoleListeners.add(printer)
  }

  public fun removePrinter(printer : BspConsolePrinter) {
    consoleListeners.remove(printer)
  }

  public fun print(text: String) {
    consoleListeners.forEach { it.printRunOutput(text) }
  }

  public companion object {
    public fun getInstance(project: Project): BspConsoleService =
      project.getService(BspConsoleService::class.java)
  }
}
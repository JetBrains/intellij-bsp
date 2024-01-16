package org.jetbrains.plugins.bsp.ui.configuration

public class BspConsolePrinter {
  public fun printOutput(text: String) {
    println("printOutput: $text")
  }

  public fun startTest(suite: Boolean, displayName: String) {
    println("startTest: $suite, $displayName")
  }

  public fun failTest(displayName: String, message: String) {
    println("failTest: $displayName, $message")
  }

  public fun passTest(suite: Boolean, displayName: String) {
    println("passTest: $suite, $displayName")
  }

  public fun ignoreTest(displayName: String) {
    println("ignoreTest: $displayName")
  }
}
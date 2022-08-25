package org.jetbrains.plugins.bsp.ui.test.configuration

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import java.util.regex.Pattern

public data class OutputCollector(val lines: MutableList<String>,
                                  val stringBuilder: StringBuilder)
public data class BazelProcessOutput(val stdoutCollector: OutputCollector,
                                     val stderrCollector: OutputCollector,
                                     val exitCode: Int)

public class BspTestConsoleService(private val processHandler: ProcessHandler, private val testResult: BazelProcessOutput) {

  public fun processTestOutputWithJUnit() {

    var startedSuite = ""
    var startedClass = ""

    notifyProcess(ServiceMessageBuilder("enteredTheMatrix").toString())

    testResult.stdoutCollector.lines.forEach {
      val firstLetterPattern = Pattern.compile("\\p{L}")

      val matcherSuite = Pattern.compile("^(?!.*(Junit|Jupiter)).*✔\$").matcher(it).find()
      val matcherSuccessTest = Pattern.compile("\\(\\)\\s✔\$").matcher(it).find()
      val matcherFailTest = Pattern.compile("\\(\\)\\s✘.*").matcher(it).find()

      val result = it.trim().substringAfter("─").substringBefore('✔').substringBefore('✘')
      if (matcherSuite  && !matcherSuccessTest && !matcherFailTest) {
        val firstLetterMatcher = firstLetterPattern.matcher(it)
        if (firstLetterMatcher.find()) {
          executeCommand("testSuiteFinished","name" to startedSuite)
          startedSuite = ""

          when(firstLetterMatcher.start()) {
            6 -> { // new test class
              executeCommand("testSuiteFinished","name" to startedClass)
              executeCommand("testSuiteStarted","name" to result)
              startedClass = result
            }
            9 -> { // new test suite (e.g. some inner class)
              executeCommand("testSuiteStarted","name" to result)
              startedSuite = result
            }
          }
        }
      }
      else if (matcherSuccessTest) {
        executeCommand("testStarted", "name" to result)
        executeCommand("testFinished", "name" to result)
      }
      else if (matcherFailTest) {
        executeCommand("testStarted", "name" to result)
        executeCommand("testFailed",
          "name" to result,
                 "error" to "true",
                 "message" to it.substringAfter('✘'))
      }
    }

    executeCommand("testSuiteFinished","name" to startedSuite)
    executeCommand("testSuiteFinished","name" to startedClass)

    notifyProcess("Finish\n")
    processHandler.destroyProcess()
  }

  private fun executeCommand(command: String, vararg pairs: Pair<String, String>) {
    val testSuiteStarted = ServiceMessageBuilder(command)
    pairs.iterator().forEach { testSuiteStarted.addAttribute(it.first, it.second) }
    notifyProcess(testSuiteStarted.toString() + "\n")
  }

  private fun notifyProcess(message: String) {
    processHandler.notifyTextAvailable(message, ProcessOutputTypes.STDOUT);
  }
}
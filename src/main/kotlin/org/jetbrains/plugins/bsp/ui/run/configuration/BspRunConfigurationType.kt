package org.jetbrains.plugins.bsp.ui.run.configuration

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.services.*
import org.jetbrains.plugins.bsp.ui.test.configuration.BspConfigurationType
import java.io.OutputStream
import javax.swing.Icon

internal class BspRunConfigurationType : ConfigurationType {

  override fun getDisplayName(): String = "BSP RUN"

  override fun getConfigurationTypeDescription(): String = "BSP RUN"

  override fun getIcon(): Icon = BspPluginIcons.bsp

  override fun getId(): String = ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    return arrayOf(BspRunFactory(this))
  }

  companion object {
    const val ID: String = "BSP_RUN_CONFIGURATION"
  }
}

public class BspRunFactory(t: ConfigurationType) : ConfigurationFactory(t) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    return BspRunConfiguration(project, this, "BSP RUN")
  }

  override fun getId(): String {
    return BspConfigurationType.ID
  }
}

public interface BspConsolePrinter {
  public fun printRunOutput(text: String)
}


public class BspRunProcessHandler : ProcessHandler(), BspConsolePrinter {

  override fun destroyProcessImpl() {}

  override fun detachProcessImpl() {}

  override fun detachIsDefault(): Boolean = false

  override fun getProcessInput(): OutputStream? = null

  override fun printRunOutput(text: String) {
    val output = prepareTextToPrint(text)
    notifyTextAvailable(output, ProcessOutputType.STDOUT)
  }

  private fun prepareTextToPrint(text: String): String =
    if (text.endsWith("\n")) text else text + "\n"

  public fun shutdown() {
    super.notifyProcessTerminated(0)
  }
}

public class BspRunConfiguration(project: Project, configurationFactory: ConfigurationFactory, name: String) :
  RunConfigurationBase<String>(project, configurationFactory, name) {

  internal class BspCommandLineState(val project: Project, environment: ExecutionEnvironment) :
    CommandLineState(environment) {
    override fun startProcess(): BspRunProcessHandler = BspRunProcessHandler()

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
      val processHandler = startProcess()
      val console = createConsole(executor)?.apply {
        attachToProcess(processHandler)
        processHandler.startNotify()
      }
      val bspConnectionService = project.getService(BspConnectionService::class.java)
      val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)
      val bspBuildConsoleService = BspBuildConsoleService.getInstance(project)
      val bspConsoleService = BspConsoleService.getInstance(project)
      environment.getUserData(BspUtilService.targetIdKey)
      val bspResolver = VeryTemporaryBspResolver(
        project.stateStore.projectBasePath,
        bspConnectionService.server!!,
        bspSyncConsoleService.bspSyncConsole,
        bspBuildConsoleService.bspBuildConsole,
      )
      bspConsoleService.registerPrinter(processHandler)

      environment.getUserData(BspUtilService.targetIdKey)?.let {
        bspResolver.runTarget(BuildTargetIdentifier(it.uri), processHandler)
      }
      bspConsoleService.removePrinter(processHandler)
      processHandler.shutdown()

      return DefaultExecutionResult(console, processHandler)
    }

  }


  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    BspCommandLineState(project, environment)

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    TODO("Not yet implemented")
  }
}


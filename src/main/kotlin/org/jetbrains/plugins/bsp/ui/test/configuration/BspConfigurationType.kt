package org.jetbrains.plugins.bsp.ui.test.configuration

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import java.io.OutputStream
import javax.swing.Icon
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.services.BspBuildConsoleService
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.plugins.bsp.services.BspSyncConsoleService
import org.jetbrains.plugins.bsp.services.BspUtilService
import org.jetbrains.plugins.bsp.services.VeryTemporaryBspResolver


public class BspConfigurationType : ConfigurationType {

  override fun getDisplayName(): String = "BSP TEST"

  override fun getConfigurationTypeDescription(): String = "BSP TEST"

  override fun getIcon(): Icon = BspPluginIcons.bsp

  override fun getId(): String = ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    return arrayOf(TestRunFactory(this))
  }
  public companion object {
    public const val ID: String = "BSP_TEST_RUN_CONFIGURATION"
  }
}

public class TestRunFactory(t: ConfigurationType) : ConfigurationFactory(t) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    return TestRunConfiguration(project, this, "BSP TEST")
  }

  override fun getId(): String {
    return BspConfigurationType.ID
  }
}

public class BspProcHandler : ProcessHandler() {
  override fun destroyProcessImpl() {}

  override fun detachProcessImpl() {}

  override fun detachIsDefault(): Boolean = false

  override fun getProcessInput(): OutputStream? = null

  public fun shutdown() {
    super.notifyProcessTerminated(0)
  }
}

public class TestRunConfiguration(project: Project, configurationFactory: ConfigurationFactory, name: String)
  : RunConfigurationBase<String>(project, configurationFactory, name) {


  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    return RunProfileState { executor, _ ->

      val userdata = environment.getUserData(BspUtilService.targetIdKey)
      val bspConnectionService = project.getService(BspConnectionService::class.java)
      val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)
      val bspBuildConsoleService = BspBuildConsoleService.getInstance(project)

      val bspResolver = VeryTemporaryBspResolver(
        project.stateStore.projectBasePath,
        bspConnectionService.server!!,
        bspSyncConsoleService.bspSyncConsole,
        bspBuildConsoleService.bspBuildConsole
      )

      val testResult = bspResolver.testTarget(userdata!!)
      val gson = Gson()
      val bazelTestProcessOutput = gson.fromJson(testResult.data as JsonObject, BazelProcessOutput::class.java)

      val procHandler = BspProcHandler()
      procHandler.startNotify()

      val console = SMTestRunnerConnectionUtil.createAndAttachConsole("BSP", procHandler, SMTRunnerConsoleProperties(this, "BSP", executor))
      // TODO do something when tests are not from JUnit
      if(bazelTestProcessOutput.stdoutCollector.stringBuilder.toString().contains("JUnit Jupiter")) {
        BspTestConsoleService(procHandler, bazelTestProcessOutput).processTestOutputWithJUnit()
      }

      DefaultExecutionResult(console, procHandler)
    }
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    TODO("Not yet implemented")
  }

}
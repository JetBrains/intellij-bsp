package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import org.jdom.Element
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import org.jetbrains.plugins.bsp.ui.configuration.BspTestConfiguration
import java.awt.TextField
import java.util.*
import javax.swing.JComponent
import javax.swing.JTextField

public class GenericBspRunHandler(private val configuration: BspRunConfigurationBase) : BspRunHandler {
  override val settings: BspRunConfigurationSettings = object : BspRunConfigurationSettings {
    override fun readExternal(element: Element) {

    }

    override fun writeExternal(element: Element) {
    }

    override fun getEditor(project: Project): SettingsEditor<BspRunConfigurationSettings> = object :
      SettingsEditor<BspRunConfigurationSettings>() {
        var count = 0
        val tf = JTextField()

      override fun resetEditorFrom(s: BspRunConfigurationSettings) {
        count++
        tf.text = count.toString()
      }

      override fun applyEditorTo(s: BspRunConfigurationSettings) {
      }

      override fun createEditor(): JComponent = tf
    }

  }

  override val name: String = "Generic BSP Run Handler"

  override fun getRunProfileState(
    executor: Executor,
    environment: ExecutionEnvironment,
  ): RunProfileState =
    when (configuration) {
      is BspTestConfiguration -> {
        thisLogger().warn("Using generic test handler for ${configuration.name}")
        BspTestCommandLineState(environment, configuration, UUID.randomUUID().toString())
      }

      is BspRunConfiguration -> {
        thisLogger().warn("Using generic run handler for ${configuration.name}")
        BspRunCommandLineState( environment, configuration, UUID.randomUUID().toString())
      }

      else -> {
        throw IllegalArgumentException("GenericBspRunHandler can only handle BspRunConfiguration")
      }
    }
  }

package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import org.jetbrains.plugins.bsp.ui.configuration.BspTestConfiguration
import java.util.UUID
import javax.swing.JComponent
import javax.swing.JTextField

public class GenericState : BspRunConfigurationState {
  public var count: Int = 0

  public class BullshitEditor(project: Project) : BspRunConfigurationStateEditor {
    private val tf = JTextField()

    init {
      thisLogger().warn("Initializing BullshitEditor")
    }



    override fun resetEditorFrom(s: BspRunConfigurationState) {
      val s = s as GenericState
      s.count++
      tf.text = s.count.toString()
      thisLogger().warn("Resetting BullshitEditor")
    }

    override fun applyEditorTo(state: BspRunConfigurationState) {
      thisLogger().warn("Applying BullshitEditor")
    }

    override fun createComponent(): JComponent {
      thisLogger().warn("Creating BullshitEditor")
      return tf
    }
  }

  override fun readExternal(element: Element) {
    element.getAttributeValue("count")?.let {
      count = it.toInt()
    }
  }

  override fun writeExternal(element: Element) {
    element.setAttribute("count", count.toString())
  }

  override fun getEditor(project: Project): BspRunConfigurationStateEditor = BullshitEditor(project)
}

public class GenericBspRunHandler(private val configuration: BspRunConfigurationBase) : BspRunHandler {
  override val settings: BspRunConfigurationState = GenericState()

  override val name: String = "Generic BSP Run Handler"

  override fun getRunProfileState(
    executor: Executor,
    environment: ExecutionEnvironment,
  ): RunProfileState = when (configuration) {
    is BspTestConfiguration -> {
      thisLogger().warn("Using generic test handler for ${configuration.name}")
      BspTestCommandLineState(environment, configuration, UUID.randomUUID().toString())
    }

    is BspRunConfiguration -> {
      thisLogger().warn("Using generic run handler for ${configuration.name}")
      BspRunCommandLineState(environment, configuration, UUID.randomUUID().toString())
    }

    else -> {
      throw IllegalArgumentException("GenericBspRunHandler can only handle BspRunConfiguration")
    }
  }
}

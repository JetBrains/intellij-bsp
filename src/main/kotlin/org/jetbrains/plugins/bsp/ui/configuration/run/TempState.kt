package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.configurationStore.deserializeInto
import com.intellij.configurationStore.serializeObjectInto
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.FragmentedSettings
import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.externalSystem.service.ui.util.SettingsFragmentInfo
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import org.jdom.Element
import java.awt.Component
import javax.swing.JCheckBox
import javax.swing.JSpinner
import javax.swing.JTextField

public class OtherState : BaseState(), FragmentedSettings, BspRunConfigurationState {

  @com.intellij.configurationStore.Property(description = "Env")
  @get:Attribute("env")
  public var env: EnvironmentVariablesData by property(EnvironmentVariablesData.DEFAULT) { it == EnvironmentVariablesData.DEFAULT }

  @get:XCollection(propertyElementName = "selectedOptions")
  override var selectedOptions: MutableList<FragmentedSettings.Option> by list()
  override fun readExternal(element: Element) {
    element.deserializeInto(this)
  }

  override fun writeExternal(element: Element) {
    serializeObjectInto(this, element)
  }

  override fun getEditor(): BspRunConfigurationStateEditor {
    return OtherEditor(this)
  }
}

public class OtherEditor(private val settings: OtherState) : FragmentedSettingsEditor<OtherState>(settings),
  BspRunConfigurationStateEditor {
  override fun createFragments(): Collection<SettingsEditorFragment<OtherState, *>> =
    SettingsEditorFragmentContainer.fragments {
      addEnvironmentFragment(object : LabeledSettingsFragmentInfo {
        override val editorLabel: String = "Environment Variables"
        override val settingsActionHint: String = "Environment Variables Action Hint"
        override val settingsGroup: String = "Environment Variables Group"
        override val settingsHint: String = "Environment Variables Hint"
        override val settingsId: String = "Environment Variables ID"
        override val settingsName: String = "Environment Variables Name"
        override val settingsPriority: Int = 0
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      },
        { settings.env.envs },
        { settings.env = settings.env.with(it) },
        { settings.env.isPassParentEnvs },
        { settings.env = settings.env.with(it) },
        false
      )
    }

  override fun resetEditorFrom(state: BspRunConfigurationState) {
    val otherState = state as? OtherState ?: return
    resetFrom(otherState)
  }

  override fun applyEditorTo(state: BspRunConfigurationState) {
    val otherState = state as? OtherState ?: return
    applyTo(otherState)
  }

  override fun getEditorComponent(): Component {
    return component
  }
}


public class SomeState : BaseState(), FragmentedSettings, BspRunConfigurationState {

  @com.intellij.configurationStore.Property(description = "Show console when a message is printed to standard error stream")
  @get:Attribute("someName")
  public var someName: String? by string()

  @com.intellij.configurationStore.Property(description = "Show console when a message is printed to standard error stream")
  @get:Attribute("counter")
  public var counter: Int by property(0)

  @Tag("output_file")
  public class OutputFileOptions : BaseState() {
    @get:Attribute("path")
    public var fileOutputPath: String? by string()

    @get:Attribute("is_save")
    public var isSaveOutput: Boolean by property(false)
  }

  @get:Property
  public var outputFileOptions: OutputFileOptions by property(OutputFileOptions())

  @get:XCollection(propertyElementName = "selectedOptions")
  override var selectedOptions: MutableList<FragmentedSettings.Option> by list()

  override fun readExternal(element: Element) {
    element.deserializeInto(this)
  }

  override fun writeExternal(element: Element) {
    serializeObjectInto(this, element)
  }

  override fun getEditor(): BspRunConfigurationStateEditor {
    return SomeEditor(this)
  }
}

public class SomeEditor(private val settings: SomeState) : FragmentedSettingsEditor<SomeState>(settings),
  BspRunConfigurationStateEditor {
  override fun createFragments(): Collection<SettingsEditorFragment<SomeState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createHeader("WITAJ W EDYTORZE KONFIGURACJI"))
      addSettingsEditorFragment(object : SettingsFragmentInfo {

        override val settingsActionHint: String = "Some Action Hint"
        override val settingsGroup: String = "Some Group"
        override val settingsHint: String = "Some Hint"
        override val settingsId: String = "Some ID"
        override val settingsName: String = "Some Name"
        override val settingsPriority: Int = 0
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      },
        { JTextField() },
        { settings, component -> component.text = settings.someName ?: "" },
        { settings, component -> settings.someName = component.text })

      addSettingsEditorFragment(object : LabeledSettingsFragmentInfo {
        override val editorLabel: String = "Counter"
        override val settingsActionHint: String = "Counter Action Hint"
        override val settingsGroup: String = "Counter Group"
        override val settingsHint: String = "Counter Hint"
        override val settingsId: String = "Counter ID"
        override val settingsName: String = "Counter Name"
        override val settingsPriority: Int = 0
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      },
        { JSpinner() },
        { settings, component -> component.value = settings.counter },
        { settings, component -> settings.counter = component.value as Int })

      // output file
      addSettingsEditorFragment(object : SettingsFragmentInfo {
        override val settingsActionHint: String = "Output File Action Hint"
        override val settingsGroup: String = "Output File Group"
        override val settingsHint: String = "Output File Hint"
        override val settingsId: String = "Output File ID"
        override val settingsName: String = "Output File Name"
        override val settingsPriority: Int = 0
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      },
        { JTextField() },
        { settings, component -> component.text = settings.outputFileOptions.fileOutputPath ?: "" },
        { settings, component -> settings.outputFileOptions.fileOutputPath = component.text })

      // save output
      addSettingsEditorFragment(object : SettingsFragmentInfo {
        override val settingsActionHint: String = "Save Output Action Hint"
        override val settingsGroup: String = "Save Output Group"
        override val settingsHint: String = "Save Output Hint"
        override val settingsId: String = "Save Output ID"
        override val settingsName: String = "Save Output Name"
        override val settingsPriority: Int = 0
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      },
        { JCheckBox() },
        { settings, component -> component.isSelected = settings.outputFileOptions.isSaveOutput },
        { settings, component -> settings.outputFileOptions.isSaveOutput = component.isSelected })
    }

  override fun resetEditorFrom(state: BspRunConfigurationState) {
    val someState = state as? SomeState ?: return
    resetFrom(someState)
  }

  override fun applyEditorTo(state: BspRunConfigurationState) {
    val someState = state as? SomeState ?: return
    applyTo(someState)
  }

  override fun getEditorComponent(): Component {
    return component
  }
}

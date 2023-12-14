package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.compiler.options.CompileStepBeforeRun
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.execution.ui.*
import com.intellij.openapi.externalSystem.service.execution.configuration.addBeforeRunFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addLabeledSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.DropDownLink


public class BspRunConfigurationEditor(public val runConfiguration: BspRunConfiguration) : RunConfigurationFragmentedEditor<BspRunConfiguration>(
  runConfiguration,
  BspRunConfigurationExtensionManager.getInstance()
) {
  override fun createRunFragments(): List<SettingsEditorFragment<BspRunConfiguration, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createRunHeader())
      addBeforeRunFragment(CompileStepBeforeRun.ID)
      addAll(BeforeRunFragment.createGroup())
      add(CommonTags.parallelRun())
      addBspTargetFragment()
      addBspEnvironmentFragment()
      addBspDebuggerTypeFragment()
    }

  private fun SettingsEditorFragmentContainer<BspRunConfiguration>.addBspDebuggerTypeFragment() {
    this.addLabeledSettingsEditorFragment(
      object : LabeledSettingsFragmentInfo { // TODO: Use bundle
        override val editorLabel: String = "Debugger type"
        override val settingsId: String = "bsp.debugger.type.fragment"
        override val settingsName: String = "Debugger type"
        override val settingsGroup: String = "BSP"
        override val settingsHint: String = "Debugger type"
        override val settingsActionHint: String = "Debugger type"
     },
      { DropDownLink<BspDebugType?>(null, BspDebugType.entries.toList() + null) },
      { it, c ->
        c.selectedItem = it.debugType
      },
      { it, c ->
        it.debugType = c.selectedItem
      },
      { true }
    )
  }

  private fun SettingsEditorFragmentContainer<BspRunConfiguration>.addBspEnvironmentFragment() {
    this.addEnvironmentFragment(
      object : LabeledSettingsFragmentInfo {
        override val editorLabel: String = ExecutionBundle.message("environment.variables.component.title")
        override val settingsId: String = "external.system.environment.variables.fragment" // TODO: does it matter?
        override val settingsName: String = ExecutionBundle.message("environment.variables.fragment.name")
        override val settingsGroup: String = ExecutionBundle.message("group.operating.system")
        override val settingsHint: String = ExecutionBundle.message("environment.variables.fragment.hint")
        override val settingsActionHint: String =
          ExecutionBundle.message("set.custom.environment.variables.for.the.process")
      },
      { runConfiguration.env.envs },
      { runConfiguration.env.with(it) },
      { runConfiguration.env.isPassParentEnvs },
      { runConfiguration.env.with(it) },
      false
    )
  }

  private fun SettingsEditorFragmentContainer<BspRunConfiguration>.addBspTargetFragment() {
    this.addLabeledSettingsEditorFragment(
      object : LabeledSettingsFragmentInfo { // TODO: Use bundle
        override val editorLabel: String = "Build target"
        override val settingsId: String = "bsp.target.fragment"
        override val settingsName: String = "Build target"
        override val settingsGroup: String = "BSP"
        override val settingsHint: String = "Build target"
        override val settingsActionHint: String = "Build target"
     },
      { BspTargetBrowserComponent() },
      { it, c ->
        c.text = it.targetUri ?: ""
      },
      { it, c ->
        it.targetUri = c.text
      },
      { true }
    )
  }
}

public class BspTargetBrowserComponent : TextFieldWithBrowseButton() {
  // TODO: implement a browser
}
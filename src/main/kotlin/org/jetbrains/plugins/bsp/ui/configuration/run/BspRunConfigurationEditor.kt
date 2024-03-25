package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.compiler.options.CompileStepBeforeRun
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.ui.*
import com.intellij.openapi.externalSystem.service.execution.configuration.addBeforeRunFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addLabeledSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import javax.swing.Box
import javax.swing.JComponent


public class HandlerSpecificSettingsEditorFragment(private val runConfiguration: BspRunConfigurationBase):
  SettingsEditorFragment<BspRunConfigurationBase, JComponent>(null, null, null, Box.createVerticalBox(), 0,
    { configuration, component -> runConfiguration.settingsEditor.resetFrom(configuration.handler.settings) },
    { configuration, component -> runConfiguration.settingsEditor.applyTo(configuration.handler.settings) },
    { true }) {

  init {
    runConfiguration.handlerChangeListeners.add(BspRunConfigurationBase.HandlerChangeListener {
      component.removeAll()
      component.add(runConfiguration.settingsEditor.component)
    })
  }
}

/**
 * The base editor for a BSP run configuration.
 * Takes care of targets, the common settings and sets up the handler-specific settings editor.
 */
public class BspRunConfigurationEditor(public val runConfiguration: BspRunConfigurationBase) : RunConfigurationFragmentedEditor<BspRunConfigurationBase>(
  runConfiguration,
  BspRunConfigurationExtensionManager.getInstance()
) {

  override fun createRunFragments(): List<SettingsEditorFragment<BspRunConfigurationBase, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createRunHeader())
      addBeforeRunFragment(CompileStepBeforeRun.ID)
      addAll(BeforeRunFragment.createGroup())
      add(CommonTags.parallelRun())
      addBspTargetFragment()
      add(HandlerSpecificSettingsEditorFragment(runConfiguration))
    }

//  public fun programArguments(): SettingsEditorFragment<BspRunConfigurationBase, RawCommandLineEditor> {
//    val programArguments = RawCommandLineEditor()
//    CommandLinePanel.setMinimumWidth(programArguments, 400)
//    val message = ExecutionBundle.message("run.configuration.program.parameters.placeholder")
//    programArguments.editorField.emptyText.setText(message)
//    programArguments.editorField.accessibleContext.accessibleName = message
//    TextComponentEmptyText.setupPlaceholderVisibility(programArguments.editorField)
//    CommonParameterFragments.setMonospaced(programArguments.textField)
//
//    val parameters: SettingsEditorFragment<BspRunConfigurationBase, RawCommandLineEditor> =
//      SettingsEditorFragment<BspRunConfigurationBase, RawCommandLineEditor>(
//        "commandLineParameters",
//        ExecutionBundle.message("run.configuration.program.parameters.name"),
//        null,
//        programArguments,
//        100,
//        { settings: BspRunConfigurationBase, component: RawCommandLineEditor ->
//          component.text = settings.getProgramParameters()
//        },
//        { component: RawCommandLineEditor ->
//          settings.setProgramParameters(
//            component.text
//          )
//        },
//        { true }
//      )
//    parameters.isRemovable = false
//    parameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }
//    parameters.setHint(ExecutionBundle.message("run.configuration.program.parameters.hint"))
//
//    return parameters
//  }

//  private fun SettingsEditorFragmentContainer<BspRunConfigurationBase>.addBspEnvironmentFragment() {
//    this.addEnvironmentFragment(
//      object : LabeledSettingsFragmentInfo {
//        override val editorLabel: String = ExecutionBundle.message("environment.variables.component.title")
//        override val settingsId: String = "external.system.environment.variables.fragment" // TODO: does it matter?
//        override val settingsName: String = ExecutionBundle.message("environment.variables.fragment.name")
//        override val settingsGroup: String = ExecutionBundle.message("group.operating.system")
//        override val settingsHint: String = ExecutionBundle.message("environment.variables.fragment.hint")
//        override val settingsActionHint: String =
//          ExecutionBundle.message("set.custom.environment.variables.for.the.process")
//      },
//      { runConfiguration.env.envs },
//      { runConfiguration.env.with(it) },
//      { runConfiguration.env.isPassParentEnvs },
//      { runConfiguration.env.with(it) },
//      false
//    )
//  }

  private fun SettingsEditorFragmentContainer<BspRunConfigurationBase>.addBspTargetFragment() {
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
        c.text = it.targets.singleOrNull() ?: "(multiple targets)"
      },
      { it, c ->
        it.targets = listOf(c.text)
      },
      { true }
    )
  }
}

public class BspTargetBrowserComponent : TextFieldWithBrowseButton() {
  // TODO: implement a browser
}
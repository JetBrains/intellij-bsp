package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.ComboBoxFieldPanel
import com.intellij.ui.TextFieldWithAutoCompletionWithBrowseButton
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent


public class BspRunConfigurationEditor(public val project: Project) : SettingsEditor<BspRunConfiguration>() {
  private val buildTargetField = TextFieldWithAutoCompletionWithBrowseButton(project)
  private val debugTypeSelector = ComboBoxFieldPanel().also {
    it.comboBox.addItem(BspDebugType.JDWP)
    it.comboBox.addItem(null)
  }


  private val panel = FormBuilder.createFormBuilder()
    .addLabeledComponent("Debug type", debugTypeSelector)
    .addLabeledComponent("Build target", buildTargetField)
    .panel

  override fun resetEditorFrom(s: BspRunConfiguration) {
    TODO("Not yet implemented")
  }

  override fun applyEditorTo(s: BspRunConfiguration) {
    TODO("Not yet implemented")
  }

  override fun createEditor(): JComponent {
    return panel
  }
}
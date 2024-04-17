package org.jetbrains.plugins.bsp.ui.configuration.run

import javax.swing.JComponent

/** Provides support for editing {@link RunConfigurationState}s. */
public interface BspRunConfigurationStateEditor {
  /** Reset the editor based on the given state. */
  public fun resetEditorFrom(state: BspRunConfigurationState)

  /** Update the given state based on the editor.  */
  public fun applyEditorTo(state: BspRunConfigurationState)

  /** @return A component to display for the editor.
   */
  public fun createComponent(): JComponent
}
package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.impl.RunConfigurationSettingsEditor
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element;
import java.awt.Component


public interface BspRunConfigurationState {
  /** Loads this handler's state from the external data.  */
  @Throws(InvalidDataException::class)
  public fun readExternal(element: Element)

  /** Updates the element with the handler's state.  */
  @Throws(WriteExternalException::class)
  public fun writeExternal(element: Element)

  public fun getEditor(): BspRunConfigurationStateEditor
}

// Neded because of the generics in SettingsEditor<T>
public interface BspRunConfigurationStateEditor {
  public fun resetEditorFrom(state: BspRunConfigurationState)
  public fun applyEditorTo(state: BspRunConfigurationState)
  public fun getEditorComponent(): Component
}

public abstract class BspCompositeRunConfigurationState : BspRunConfigurationState {
  protected val settings: MutableList<BspRunConfigurationState> = mutableListOf()

}
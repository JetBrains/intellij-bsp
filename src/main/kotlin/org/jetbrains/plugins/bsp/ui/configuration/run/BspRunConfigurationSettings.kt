package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.impl.RunConfigurationSettingsEditor
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element;


public interface BspRunConfigurationSettings {
  /** Loads this handler's state from the external data.  */
  @Throws(InvalidDataException::class)
  public fun readExternal(element: Element)

  /** Updates the element with the handler's state.  */
  @Throws(WriteExternalException::class)
  public fun writeExternal(element: Element)

  /**
   * @return A [RunConfigurationSettingsEditor] for this state.
   */
  public fun getEditor(project: Project): SettingsEditor<BspRunConfigurationSettings>
}

public abstract class BspCompositeRunConfigurationSettings : BspRunConfigurationSettings {
  protected val settings: MutableList<BspRunConfigurationSettings> = mutableListOf()

}
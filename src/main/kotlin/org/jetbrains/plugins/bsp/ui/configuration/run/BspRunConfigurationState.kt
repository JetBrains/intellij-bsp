package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.configurationStore.deserializeInto
import com.intellij.configurationStore.serializeObjectInto
import com.intellij.execution.ui.FragmentedSettings
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.util.xmlb.annotations.XCollection
import org.jdom.Element
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase

public abstract class BspRunConfigurationState : BaseState(), FragmentedSettings {
  /** Loads this handler's state from the external data.  */
  @Throws(InvalidDataException::class)
  public fun readExternal(element: Element) {
    element.deserializeInto(this)
  }

  /** Updates the element with the handler's state.  */
  @Throws(WriteExternalException::class)
  public fun writeExternal(element: Element) {
    serializeObjectInto(this, element)
  }

  @get:XCollection(propertyElementName = "selectedOptions")
  override var selectedOptions: MutableList<FragmentedSettings.Option> by list()

  public abstract fun getEditor(configuration: BspRunConfigurationBase): SettingsEditor<BspRunConfigurationBase>
}

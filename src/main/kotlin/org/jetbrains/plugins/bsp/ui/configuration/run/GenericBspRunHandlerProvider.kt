package org.jetbrains.plugins.bsp.ui.configuration.run

import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase

public class GenericBspRunHandlerProvider : BspRunHandlerProvider {
  override val id: String = "GenericBspRunHandlerProvider"

  override fun createRunHandler(configuration: BspRunConfigurationBase): BspRunHandler {
    return GenericBspRunHandler(configuration)
  }

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean = true
}
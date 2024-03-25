package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase

public interface BspRunHandlerProvider {

  /**
   * Returns the unique ID of this {@link BspRunHandlerProvider}. The ID is
   * used to store configuration settings and must not change between plugin versions.
   */
  public val id: String

/**
   * Creates a {@link BspRunHandler} for the given configuration.
   */
  public fun createRunHandler(configuration: BspRunConfigurationBase): BspRunHandler

  /**
   * Returns true if this provider can create a {@link BspRunHandler} for the given targets.
   */
  public fun canRun(targetInfos: List<BuildTargetInfo>): Boolean

  public companion object {
    public val ep: ExtensionPointName<BspRunHandlerProvider> =
      ExtensionPointName.create("org.jetbrains.bsp.bspRunHandlerProvider")

    /** Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets */
    public fun getRunHandlerProvider(targetInfos: List<BuildTargetInfo>): BspRunHandlerProvider {
      return ep.extensionList.firstOrNull { it.canRun(targetInfos) } ?: GenericBspRunHandlerProvider()
    }

    /** Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets */
    public fun getRunHandlerProvider(project: Project, targets: List<String>): BspRunHandlerProvider {
      val targetInfos = targets.mapNotNull { MagicMetaModelService.getInstance(project).value.getBuildTargetInfo(it) }
      return getRunHandlerProvider(targetInfos)
    }

    /** Finds a BspRunHandlerProvider by its unique ID */
    public fun findRunHandlerProvider(id: String): BspRunHandlerProvider? {
      return ep.extensionList.firstOrNull { it.id == id }
    }
  }
}
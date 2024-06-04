package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.target.TemporaryTargetUtils
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
   * Returns true if this provider can create a {@link BspRunHandler} for running the given targets.
   */
  public fun canRun(targetInfos: List<BuildTargetInfo>): Boolean

  /**
   * Returns true if this provider can create a {@link BspRunHandler} for debugging the given targets.
   */
  public fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean

  public companion object {
    public val ep: ExtensionPointName<BspRunHandlerProvider> =
      ExtensionPointName.create("org.jetbrains.bsp.bspRunHandlerProvider")

    /** Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets */
    public fun getRunHandlerProvider(targetInfos: List<BuildTargetInfo>, isDebug: Boolean = false): BspRunHandlerProvider? {
      return ep.extensionList.firstOrNull {
        if (isDebug) {
          it.canDebug(targetInfos)
        } else {
          it.canRun(targetInfos)
        }
      }
    }

    /** Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets.
     *  Needs to query MMM for Build Target Infos. */
    public fun getRunHandlerProvider(project: Project, targets: List<String>): BspRunHandlerProvider {
      val targetInfos = targets.mapNotNull { project.service<TemporaryTargetUtils>().getBuildTargetInfoForId(
        BuildTargetIdentifier(it)
      ) }
      if (targetInfos.size != targets.size) {
        thisLogger().warn("Some targets could not be found: ${targets - targetInfos.map { it.id }.toSet()}")
      }

      // TODO
      return getRunHandlerProvider(targetInfos)!!
    }

    /** Finds a BspRunHandlerProvider by its unique ID */
    public fun findRunHandlerProvider(id: String): BspRunHandlerProvider? {
      return ep.extensionList.firstOrNull { it.id == id }
    }
  }
}
package org.jetbrains.plugins.bsp.assets

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.plugins.bsp.extension.points.WithBuildToolId
import javax.swing.Icon

public interface BuildToolAssetsExtension : WithBuildToolId {
  public val presentableName: String

  public val icon: Icon

  // In Swing toolwindow implementation instead of `AssetIcon`, `javax.swing.Icon` is used
  public val loadedTargetIcon: AssetIcon
  public val unloadedTargetIcon: AssetIcon

  public val invalidTargetIcon: AssetIcon

  public companion object {
    internal val ep =
      ExtensionPointName.create<BuildToolAssetsExtension>("org.jetbrains.bsp.buildToolAssetsExtension")
  }
}

public data class AssetIcon(
  public val path: String,
  public val clazz: Class<out Any>,
)

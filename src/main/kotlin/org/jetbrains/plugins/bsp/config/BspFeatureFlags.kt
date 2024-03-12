package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.util.registry.Registry

private const val PYTHON_SUPPORT = "bsp.python.support"
private const val SCALA_SUPPORT = "bsp.scala.support"
private const val SBT_SUPPORT = "bsp.sbt.support"
private const val ANDROID_SUPPORT = "bsp.android.support"
private const val BUILD_PROJECT_ON_SYNC = "bsp.build.project.on.sync"

public object BspFeatureFlags {
  public val isPythonSupportEnabled: Boolean
    get() = Registry.`is`(PYTHON_SUPPORT)

  public val isScalaSupportEnabled: Boolean
    get() = Registry.`is`(SCALA_SUPPORT)

  public val isSbtSupportEnabled: Boolean
    get() = Registry.`is`(SBT_SUPPORT)

  public val isAndroidSupportEnabled: Boolean
    get() = Registry.`is`(ANDROID_SUPPORT)

  public val isBuildProjectOnSyncEnabled: Boolean
    get() = Registry.`is`(BUILD_PROJECT_ON_SYNC)
}

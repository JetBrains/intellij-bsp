package org.jetbrains.plugins.bsp.services

import com.intellij.build.BuildViewManager
import com.intellij.build.SyncViewManager
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.ui.console.BspProcessConsole

public class BspProcessConsoleService(project: Project) {

  public val bspBuildConsole: BspProcessConsole =
    BspProcessConsole(project.getService(BuildViewManager::class.java), project.basePath!!)

  public val bspSyncConsole: BspProcessConsole =
    BspProcessConsole(project.getService(SyncViewManager::class.java), project.basePath!!)

  public companion object {
    public fun getInstance(project: Project): BspProcessConsoleService =
      project.getService(BspProcessConsoleService::class.java)
  }
}

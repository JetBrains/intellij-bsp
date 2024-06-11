package org.jetbrains.plugins.bsp.projectStructure.workspaceModel

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.plugins.bsp.android.androidSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.plugins.bsp.projectStructure.BuildTargetInfo
import org.jetbrains.plugins.bsp.projectStructure.ProjectStructureUpdater

internal class TestUpdater : ProjectStructureUpdater<WorkspaceModelProjectStructureDiff> {
  override val diffClass: Class<WorkspaceModelProjectStructureDiff> = WorkspaceModelProjectStructureDiff::class.java

  override fun isSupported(buildTarget: BuildTarget): Boolean = true

  override fun addTarget(project: Project, targetInfo: BuildTargetInfo, diff: WorkspaceModelProjectStructureDiff) {
    println("AAAA")
    val workspaceModel = WorkspaceModel.getInstance(project)
    val virtualFileUrlManager = workspaceModel.getVirtualFileUrlManager()
    val projectBasePath = project.rootDir.toNioPath()
    val workspaceModelUpdater = WorkspaceModelUpdater.create(
      diff.diff,
      virtualFileUrlManager,
      projectBasePath,
      project,
      BspFeatureFlags.isPythonSupportEnabled,
      BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
    )
    workspaceModelUpdater.loadModule(targetInfo.moduleInfo!!)
  }
}
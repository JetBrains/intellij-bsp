package org.jetbrains.plugins.bsp.projectStructure.workspaceModel

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.projectStructure.BuildTargetInfo
import org.jetbrains.plugins.bsp.projectStructure.ProjectStructureUpdater
import org.jetbrains.plugins.bsp.server.tasks.BspTargetInfo
import org.jetbrains.plugins.bsp.server.tasks.PythonBspInfo

private const val pythonLanguageId = "python"

internal class PythonProjectStructureUpdater : ProjectStructureUpdater<WorkspaceModelProjectStructureDiff, PythonBspInfo> {
  override val diffClass: Class<WorkspaceModelProjectStructureDiff> = WorkspaceModelProjectStructureDiff::class.java
  override val additionalTargetInfoClass: Class<PythonBspInfo> = PythonBspInfo::class.java

  override fun isSupported(buildTarget: BuildTarget): Boolean =
    pythonLanguageId in buildTarget.languageIds

  override fun addTarget(project: Project, targetInfo: BspTargetInfo<PythonBspInfo>, diff: WorkspaceModelProjectStructureDiff) {
    TODO("Not yet implemented")
  }
}

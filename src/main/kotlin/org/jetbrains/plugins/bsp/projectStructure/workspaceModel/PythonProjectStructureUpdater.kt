package org.jetbrains.plugins.bsp.projectStructure.workspaceModel

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.projectStructure.BuildTargetInfo
import org.jetbrains.plugins.bsp.projectStructure.ProjectStructureUpdater

private const val pythonLanguageId = "python"

internal class PythonProjectStructureUpdater : ProjectStructureUpdater<WorkspaceModelProjectStructureDiff> {
  override val diffClass: Class<WorkspaceModelProjectStructureDiff> = WorkspaceModelProjectStructureDiff::class.java

  override fun isSupported(buildTarget: BuildTarget): Boolean =
    pythonLanguageId in buildTarget.languageIds

  override fun addTarget(project: Project, targetInfo: BuildTargetInfo, diff: WorkspaceModelProjectStructureDiff) {
    TODO("Not yet implemented")
  }
}
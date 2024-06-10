package org.jetbrains.plugins.bsp.projectStructure.workspaceModel

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.impl.internal
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.plugins.bsp.magicmetamodel.impl.PerformanceLogger.logPerformance
import org.jetbrains.plugins.bsp.projectStructure.ProjectStructureDiff
import org.jetbrains.plugins.bsp.projectStructure.ProjectStructureProvider

public class WorkspaceModelProjectStructureDiff(public val diff: MutableEntityStorage) : ProjectStructureDiff {
  override suspend fun apply(project: Project) {
    println("AAAAA")
//    val workspaceModel = WorkspaceModel.getInstance(project)
//    val snapshot = workspaceModel.internal.getBuilderSnapshot()
//    logPerformance("replaceBySource-in-apply-on-workspace-model") {
//      snapshot.builder.replaceBySource({ true }, diff)
//    }
//    val storageReplacement = snapshot.getStorageReplacement()
//    writeAction {
//      val workspaceModelUpdated = logPerformance("replaceProjectModel-in-apply-on-workspace-model") {
//        workspaceModel.internal.replaceProjectModel(storageReplacement)
//      }
//      if (!workspaceModelUpdated) {
//        error("Project model is not updated successfully. Try `reload` action to recalculate the project model.")
//      }
//    }
  }
}

internal class WorkspaceModelProjectStructureProvider: ProjectStructureProvider<WorkspaceModelProjectStructureDiff, Any> {
  override fun newDiff(project: Project): WorkspaceModelProjectStructureDiff =
    WorkspaceModelProjectStructureDiff(MutableEntityStorage.create())

  override fun current(project: Project): Any {
    TODO("Not yet implemented")
  }
}

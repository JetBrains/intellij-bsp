package org.jetbrains.plugins.bsp.projectStructure.workspaceModel

private const val pythonLanguageId = "python"

//internal class PythonProjectStructureUpdater : ProjectStructureUpdater<WorkspaceModelProjectStructureDiff, PythonBspInfo> {
//  override val diffClass: Class<WorkspaceModelProjectStructureDiff> = WorkspaceModelProjectStructureDiff::class.java
//  override val additionalTargetInfoClass: Class<PythonBspInfo> = PythonBspInfo::class.java
//
//  override fun isSupported(buildTarget: BuildTarget): Boolean =
//    pythonLanguageId in buildTarget.languageIds
//
//  override fun addTarget(project: Project, targetInfo: BspTargetInfo<PythonBspInfo>, diff: WorkspaceModelProjectStructureDiff) {
//    TODO("Not yet implemented")
//  }
//}

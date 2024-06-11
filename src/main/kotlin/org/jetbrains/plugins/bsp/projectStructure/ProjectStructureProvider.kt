package org.jetbrains.plugins.bsp.projectStructure

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

public interface ProjectStructureDiff {
  public suspend fun apply(project: Project)
}

internal class AllProjectStructuresDiff(
  diffs: List<ProjectStructureDiff>,
  private val updaters: List<ProjectStructureUpdater<*>>,
  private val project: Project
) {
  private val diffs = diffs.associateBy { it::class.java }

  internal fun addTarget(buildTargetInfo: BuildTargetInfo) {
    updaters.forEach { buildTargetInfo.addIfSupported(it, it.diffClass) }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <TDiff: ProjectStructureDiff> BuildTargetInfo.addIfSupported(updater: ProjectStructureUpdater<*>, diffClazz: Class<TDiff>) {
    val typedUpdater = updater as ProjectStructureUpdater<ProjectStructureDiff>
    val diff = getDiff(diffClazz)
    addIfSupported(typedUpdater, diff)
  }

  private fun <TDiff: ProjectStructureDiff> BuildTargetInfo.addIfSupported(updater: ProjectStructureUpdater<TDiff>, diff: TDiff) {
    if (updater.isSupported(target)) {
      updater.addTarget(project, this, diff)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <TDiff: ProjectStructureDiff>getDiff(diffClazz: Class<TDiff>): TDiff =
    diffs[diffClazz] as? TDiff ?: error("Cannot find a ProjectStructureDiff of type: ${diffClazz.simpleName}")

  internal suspend fun applyAll() {
    diffs.values.forEach { it.apply(project) }
  }
}

public interface ProjectStructureProvider<TDiff : ProjectStructureDiff, TCurrent> {
  public fun newDiff(project: Project): TDiff
  public fun current(project: Project): TCurrent

  public companion object {
    internal val ep = ExtensionPointName.create<ProjectStructureProvider<*, *>>("org.jetbrains.bsp.projectStructureProvider")
  }
}

internal class AllProjectStructuresProvider(private val project: Project) {
  fun newDiff(): AllProjectStructuresDiff {
    val providers = ProjectStructureProvider.ep.extensionList
    val diffs = providers.map { it.newDiff(project) }
    val updaters = ProjectStructureUpdater.ep.extensionList

    return AllProjectStructuresDiff(diffs, updaters, project)
  }
}

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.jetbrains.bsp.bsp4kt.BuildTarget
import com.jetbrains.bsp.bsp4kt.BuildTargetIdentifier
import com.jetbrains.bsp.bsp4kt.DependencySourcesItem
import com.jetbrains.bsp.bsp4kt.JavacOptionsItem
import com.jetbrains.bsp.bsp4kt.PythonOptionsItem
import com.jetbrains.bsp.bsp4kt.ResourcesItem
import com.jetbrains.bsp.bsp4kt.SourcesItem
import org.jetbrains.magicmetamodel.LibraryItem
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

internal class ProjectDetailsToModuleDetailsTransformer(
  private val projectBasePath: Path,
  private val projectDetails: ProjectDetails,
) {
  private val targetsIndex = projectDetails.targets.associateBy { it.id }
  private val librariesIndex = projectDetails.libraries?.associateBy { it.id }

  fun moduleDetailsForTargetId(targetId: BuildTargetIdentifier): ModuleDetails {
    val target = calculateTarget(projectDetails, targetId)
    val allDependencies = allDependencies(target, librariesIndex)
    val sources = calculateSources(projectDetails, targetId)
    val resources = calculateResources(projectDetails, targetId)
    val isRoot = target.isRoot(sources, resources)
    return ModuleDetails(
      target = target,
      libraryDependencies = librariesIndex?.keys?.intersect(allDependencies)?.map { it.uri },
      moduleDependencies = targetsIndex.keys.intersect(allDependencies).map { it.uri },
      sources = if (isRoot) emptyList() else sources,
      resources = if (isRoot) emptyList() else resources,
      dependenciesSources = calculateDependenciesSources(projectDetails, targetId),
      javacOptions = calculateJavacOptions(projectDetails, targetId),
      pythonOptions = calculatePythonOptions(projectDetails, targetId),
      outputPathUris = if (isRoot) calculateAllOutputPaths(projectDetails) else emptyList(),
    )
  }

  private fun allDependencies(
    target: BuildTarget,
    librariesIndex: Map<BuildTargetIdentifier, LibraryItem>?,
  ): Set<BuildTargetIdentifier> {
    var librariesToVisit = target.dependencies
    var visited = emptySet<BuildTargetIdentifier>()
    while (librariesToVisit.isNotEmpty()) {
      val currentLib = librariesToVisit.first()
      librariesToVisit = librariesToVisit - currentLib
      visited = visited + currentLib
      librariesToVisit = librariesToVisit + (findLibrary(currentLib, librariesIndex) - visited)
    }
    return visited
  }

  private fun calculateAllOutputPaths(projectDetails: ProjectDetails): List<String> =
    projectDetails.outputPathUris

  private fun BuildTarget.isRoot(
    targetSources: List<SourcesItem>,
    targetResources: List<ResourcesItem>,
  ): Boolean =
    targetSources.all { it.sources.isEmpty() } &&
      (baseDirectory?.let { URI.create(it).toPath() } == projectBasePath ||
        projectBasePath.existsInResources(targetResources))

  private fun Path.existsInResources(resources: List<ResourcesItem>) = resources.any { resourcesItem ->
    resourcesItem.resources.any { URI.create(it).toPath() == this }
  }

  private fun findLibrary(
    currentLib: BuildTargetIdentifier,
    librariesIndex: Map<BuildTargetIdentifier, LibraryItem>?,
  ) = librariesIndex?.get(currentLib)?.dependencies.orEmpty()

  private fun calculateTarget(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): BuildTarget =
    projectDetails.targets.first { it.id == targetId }

  private fun calculateSources(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): List<SourcesItem> =
    projectDetails.sources.filter { it.target == targetId }

  private fun calculateResources(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): List<ResourcesItem> =
    projectDetails.resources.filter { it.target == targetId }

  private fun calculateDependenciesSources(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier,
  ): List<DependencySourcesItem> =
    projectDetails.dependenciesSources.filter { it.target == targetId }

  private fun calculateJavacOptions(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier,
  ): JavacOptionsItem? =
    projectDetails.javacOptions.firstOrNull { it.target == targetId }

  private fun calculatePythonOptions(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier,
  ): PythonOptionsItem? =
    projectDetails.pythonOptions.firstOrNull { it.target == targetId }
}

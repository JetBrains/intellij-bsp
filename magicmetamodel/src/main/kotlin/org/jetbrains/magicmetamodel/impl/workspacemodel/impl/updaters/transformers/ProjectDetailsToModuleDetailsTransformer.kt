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
  private val projectDetails: ProjectDetails,
  private val projectBasePath: Path,
) {

  private val targetsIndex = projectDetails.targets.associateBy { it.id }
  private val librariesIndex = projectDetails.libraries?.associateBy { it.id }

  fun moduleDetailsForTargetId(targetId: BuildTargetIdentifier): ModuleDetails {
    val target = calculateTarget(projectDetails, targetId)
    return if (target.isRoot(projectBasePath)) {
      toRootModuleDetails(projectDetails, target)
    } else {
      val allDependencies = allDependencies(target, librariesIndex)
      ModuleDetails(
        target = target,
        libraryDependencies = librariesIndex?.keys?.intersect(allDependencies)?.map { it.uri },
        moduleDependencies = targetsIndex.keys.intersect(allDependencies).map { it.uri },
        sources = calculateSources(projectDetails, targetId),
        resources = calculateResources(projectDetails, targetId),
        dependenciesSources = calculateDependenciesSources(projectDetails, targetId),
        javacOptions = calculateJavacOptions(projectDetails, targetId),
        pythonOptions = calculatePythonOptions(projectDetails, targetId),
        outputPathUris = emptyList(),
      )
    }
  }

  private fun allDependencies(
    target: BuildTarget,
    librariesIndex: Map<BuildTargetIdentifier, LibraryItem>?
  ): Set<BuildTargetIdentifier> {
    var librariesToVisit = target.dependencies
    var visited = emptySet<BuildTargetIdentifier>();
    while (librariesToVisit.isNotEmpty()) {
      val currentLib = librariesToVisit.first()
      librariesToVisit = librariesToVisit - currentLib
      visited = visited + currentLib
      librariesToVisit = librariesToVisit + (findLibrary(currentLib, librariesIndex) - visited)
    }
    return visited
  }

  private fun findLibrary(
    currentLib: BuildTargetIdentifier,
    librariesIndex: Map<BuildTargetIdentifier, LibraryItem>?,
  ) = librariesIndex?.get(currentLib)?.dependencies.orEmpty()

  private fun toRootModuleDetails(
    projectDetails: ProjectDetails,
    target: BuildTarget,
  ): ModuleDetails =
    ModuleDetails(
      target = target,
      sources = emptyList(),
      resources = emptyList(),
      dependenciesSources = calculateDependenciesSources(projectDetails, target.id),
      javacOptions = calculateJavacOptions(projectDetails, target.id),
      pythonOptions = calculatePythonOptions(projectDetails, target.id),
      outputPathUris = calculateAllOutputPaths(projectDetails),
      libraryDependencies = emptyList(),
      moduleDependencies = emptyList(),
    )

  private fun calculateTarget(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): BuildTarget =
    projectDetails.targets.first { it.id == targetId }

  private fun calculateSources(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): List<SourcesItem> =
    projectDetails.sources.filter { it.target == targetId }

  private fun calculateResources(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): List<ResourcesItem> =
    projectDetails.resources.filter { it.target == targetId }

  private fun calculateDependenciesSources(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier
  ): List<DependencySourcesItem> =
    projectDetails.dependenciesSources.filter { it.target == targetId }

  private fun calculateJavacOptions(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier
  ): JavacOptionsItem? =
    projectDetails.javacOptions.firstOrNull { it.target == targetId }

  private fun calculateAllOutputPaths(projectDetails: ProjectDetails): List<String> =
    projectDetails.outputPathUris

  private fun BuildTarget.isRoot(projectBasePath: Path): Boolean =
    this.baseDirectory?.let { URI.create(it).toPath() } == projectBasePath

  private fun calculatePythonOptions(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier
  ): PythonOptionsItem? =
    projectDetails.pythonOptions.firstOrNull { it.target == targetId }
}

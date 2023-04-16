package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.*

internal data class BspModuleDetails(
  val target: BuildTarget,
  val allTargetsIds: List<BuildTargetIdentifier>,
  val dependencySources: List<DependencySourcesItem>,
  val javacOptions: JavacOptionsItem?,
  val pythonOptions: PythonOptionsItem?,
  val type: String
)

internal class BspModuleDetailsToModuleTransformer(private val moduleNameProvider: ModuleNameProvider) :
  WorkspaceModelEntityTransformer<BspModuleDetails, Module> {

  override fun transform(inputEntity: BspModuleDetails): Module {
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(inputEntity.allTargetsIds, moduleNameProvider)

    return Module(
      name = moduleNameProvider(inputEntity.target.id),
      type = inputEntity.type,
      modulesDependencies = buildTargetToModuleDependencyTransformer.transform(inputEntity.target),
      librariesDependencies = calculateLibrariesDependencies(inputEntity),
      capabilities = inputEntity.target.capabilities.let {
        ModuleCapabilities(it.canRun, it.canTest, it.canCompile, it.canDebug)
      },
      languageIds = inputEntity.target.languageIds
    )
  }

  private fun calculateLibrariesDependencies(inputEntity: BspModuleDetails): List<LibraryDependency> {
    return if (inputEntity.target.languageIds.contains("java"))
      JavaDependencySourcesItemToLibraryDependencyTransformer
        .transform(inputEntity.dependencySources.map {
          DependencySourcesAndJavacOptions(it, inputEntity.javacOptions)
        })
    else if (inputEntity.target.languageIds.contains("python"))
      PythonDependencySourcesItemToLibraryDependencyTransformer.transform(inputEntity.dependencySources)
    else
      emptyList()
  }
}

internal object JavaDependencySourcesItemToLibraryDependencyTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesAndJavacOptions, LibraryDependency> {

  override fun transform(inputEntity: DependencySourcesAndJavacOptions): List<LibraryDependency> =
    DependencySourcesItemToLibraryTransformer.transform(inputEntity)
      .map(this::toLibraryDependency)

  private fun toLibraryDependency(library: Library): LibraryDependency =
    LibraryDependency(
      libraryName = library.displayName,
    )
}

internal object PythonDependencySourcesItemToLibraryDependencyTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesItem, LibraryDependency> {

  override fun transform(inputEntity: DependencySourcesItem): List<LibraryDependency> =
    DependencySourcesItemToPythonLibraryTransformer.transform(inputEntity)
      .map(this::toLibraryDependency)

  private fun toLibraryDependency(library: PythonLibrary): LibraryDependency =
    LibraryDependency(
      libraryName = library.displayName,
    )
}

internal class BuildTargetToModuleDependencyTransformer(
  private val allTargetsIds: List<BuildTargetIdentifier>,
  private val moduleNameProvider: ModuleNameProvider,
) : WorkspaceModelEntityPartitionTransformer<BuildTarget, ModuleDependency> {

  override fun transform(inputEntity: BuildTarget): List<ModuleDependency> =
    inputEntity.dependencies
      .filter { allTargetsIds.contains(it) }
      .map(this::toModuleDependency)

  private fun toModuleDependency(targetId: BuildTargetIdentifier): ModuleDependency =
    ModuleDependency(
      moduleName = moduleNameProvider(targetId),
    )
}

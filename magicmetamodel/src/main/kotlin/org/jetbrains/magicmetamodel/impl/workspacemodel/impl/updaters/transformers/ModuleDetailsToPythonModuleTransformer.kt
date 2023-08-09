package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.jetbrains.bsp.bsp4kt.BuildTarget
import com.jetbrains.bsp.bsp4kt.BuildTargetDataKind
import com.jetbrains.bsp.bsp4kt.PythonBuildTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonSdkInfo
import java.nio.file.Path

internal class ModuleDetailsToPythonModuleTransformer(
  moduleNameProvider: ModuleNameProvider,
  projectBasePath: Path,
) : ModuleDetailsToModuleTransformer<PythonModule>(moduleNameProvider) {
  override val type = "PYTHON_MODULE"

  private val sourcesItemToPythonSourceRootTransformer = SourcesItemToPythonSourceRootTransformer(projectBasePath)
  private val resourcesItemToPythonResourceRootTransformer =
    ResourcesItemToPythonResourceRootTransformer(projectBasePath)

  override fun transform(inputEntity: ModuleDetails): PythonModule =
    PythonModule(
      module = toGenericModuleInfo(inputEntity),
      sourceRoots = sourcesItemToPythonSourceRootTransformer.transform(inputEntity.sources.map {
        BuildTargetAndSourceItem(
          inputEntity.target,
          it,
        )
      }),
      resourceRoots = resourcesItemToPythonResourceRootTransformer.transform(inputEntity.resources),
      libraries = DependencySourcesItemToPythonLibraryTransformer.transform(inputEntity.dependenciesSources),
      sdkInfo = toSdkInfo(inputEntity),
    )

  override fun toGenericModuleInfo(inputEntity: ModuleDetails): GenericModuleInfo {
    val bspModuleDetails = BspModuleDetails(
      target = inputEntity.target,
      dependencySources = inputEntity.dependenciesSources,
      type = type,
      javacOptions = null,
      pythonOptions = inputEntity.pythonOptions,
      libraryDependencies = inputEntity.libraryDependencies,
      moduleDependencies = inputEntity.moduleDependencies,
    )

    return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails)
  }

  private fun toSdkInfo(inputEntity: ModuleDetails): PythonSdkInfo? {
    val pythonBuildTarget = extractPythonBuildTarget(inputEntity.target)
    return if (pythonBuildTarget?.version != null && pythonBuildTarget.interpreter != null)
      PythonSdkInfo(
        version = pythonBuildTarget.version!!,
        originalName = inputEntity.target.id.uri,
      )
    else null
  }
}

public fun extractPythonBuildTarget(target: BuildTarget): PythonBuildTarget? =
  if (target.dataKind == BuildTargetDataKind.Python)
    target.data?.let {
      Json.decodeFromJsonElement<PythonBuildTarget>(it)
    }
  else null

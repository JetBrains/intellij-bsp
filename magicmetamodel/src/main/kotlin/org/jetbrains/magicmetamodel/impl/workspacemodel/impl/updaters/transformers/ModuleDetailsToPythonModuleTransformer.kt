package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.PythonBuildTarget
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonSdkInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.BspModuleDetailsToModuleTransformer
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

internal class ModuleDetailsToPythonModuleTransformer(
  moduleNameProvider: ((BuildTargetIdentifier) -> String)?,
  private val projectBasePath: Path,
) : WorkspaceModelEntityTransformer<ModuleDetails, PythonModule> {

  private val bspModuleDetailsToModuleTransformer = BspModuleDetailsToModuleTransformer(moduleNameProvider)
  private val sourcesItemToPythonSourceRootTransformer = SourcesItemToPythonSourceRootTransformer(projectBasePath)
  private val resourcesItemToPythonResourceRootTransformer =
    ResourcesItemToPythonResourceRootTransformer(projectBasePath)

  override fun transform(inputEntity: ModuleDetails): PythonModule =
    PythonModule(
      module = toModule(inputEntity),
      baseDirContentRoot = toBaseDirContentRoot(inputEntity),
      sourceRoots = sourcesItemToPythonSourceRootTransformer.transform(inputEntity.sources.map {
        BuildTargetAndSourceItem(
          inputEntity.target,
          it,
        )
      }),
      resourceRoots = resourcesItemToPythonResourceRootTransformer.transform(inputEntity.resources),
      libraries = DependencySourcesItemToPythonLibraryTransformer.transform(inputEntity.dependenciesSources),
      sdkInfo = toSdkInfo(inputEntity)
    )

  private fun toModule(inputEntity: ModuleDetails): Module {
    val bspModuleDetails = BspModuleDetails(
      target = inputEntity.target,
      allTargetsIds = inputEntity.allTargetsIds,
      dependencySources = inputEntity.dependenciesSources,
      type = type,
      javacOptions = null,
      pythonOptions = inputEntity.pythonOptions,
    )

    // TODO add similar hack as in java
    return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails)
  }

  private fun toBaseDirContentRoot(inputEntity: ModuleDetails): ContentRoot =
    ContentRoot(
      // TODO what if null?
      url = URI.create(inputEntity.target.baseDirectory ?: "file:///todo").toPath()
    )

  private fun toSdkInfo(inputEntity: ModuleDetails): PythonSdkInfo? {
    val pythonBuildTarget = extractPythonBuildTarget(inputEntity.target)
    return if (pythonBuildTarget != null) PythonSdkInfo(
      version = pythonBuildTarget.version,
      // TODO: check if the URI.create().toPath() is a valid way to do that
      interpreter = URI.create(pythonBuildTarget.interpreter).toPath()
    )
    else null
  }

  companion object {

    private const val type = "PYTHON_MODULE"

  }
}

public fun extractPythonBuildTarget(target: BuildTarget): PythonBuildTarget? =
  if (target.dataKind == BuildTargetDataKind.PYTHON) Gson().fromJson(
    target.data as JsonObject,
    PythonBuildTarget::class.java
  )
  else null
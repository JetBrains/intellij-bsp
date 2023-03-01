package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.PythonBuildTarget
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonSdkInfo
import java.net.URI
import kotlin.io.path.toPath

internal object ModuleDetailsToPythonModuleTransformer : WorkspaceModelEntityTransformer<ModuleDetails, PythonModule> {

  private const val type = "PYTHON_MODULE"

  override fun transform(inputEntity: ModuleDetails): PythonModule =
    PythonModule(
      module = toModule(inputEntity),
      baseDirContentRoot = toBaseDirContentRoot(inputEntity),
      sourceRoots = SourcesItemToPythonSourceRootTransformer.transform(inputEntity.sources.map {
        BuildTargetAndSourceItem(
          inputEntity.target,
          it,
        )
      }),
      resourceRoots = ResourcesItemToPythonResourceRootTransformer.transform(inputEntity.resources),
      // TODO: libraries probably don't work yet - they're the same as in Java version of this file.
      libraries = DependencySourcesItemToLibraryTransformer.transform(inputEntity.dependenciesSources.map {
        DependencySourcesAndJavacOptions(
          it,
          inputEntity.javacOptions
        )
      }),
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

    return BspModuleDetailsToModuleTransformer.transform(bspModuleDetails)
  }

  private fun toBaseDirContentRoot(inputEntity: ModuleDetails): ContentRoot =
    ContentRoot(
      // TODO what if null?
      url = URI.create(inputEntity.target.baseDirectory ?: "file:///todo").toPath()
    )

  private fun toSdkInfo(inputEntity: ModuleDetails): PythonSdkInfo? {
    val pythonBuildTarget = extractPythonBuildTarget(inputEntity.target)
    // TODO: is the URI.create().toPath() valid way to do that?
    return if (pythonBuildTarget != null) PythonSdkInfo(
      version = pythonBuildTarget.version,
      interpreter = URI.create(pythonBuildTarget.interpreter).toPath()
    )
    else null
  }

}

public fun extractPythonBuildTarget(target: BuildTarget): PythonBuildTarget? =
  if (target.dataKind == BuildTargetDataKind.PYTHON) Gson().fromJson(
    target.data as JsonObject,
    PythonBuildTarget::class.java
  )
  else null
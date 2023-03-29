package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.PythonBuildTarget
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonSdkInfo
import java.nio.file.Path
import kotlin.io.path.Path

internal class ModuleDetailsToPythonModuleTransformer(
  moduleNameProvider: ((BuildTargetIdentifier) -> String)?,
  private val projectBasePath: Path,
): ModuleDetailsToModuleTransformer<PythonModule>(moduleNameProvider) {

  override val type = "PYTHON_MODULE"

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
      libraries = emptyList(),
      sdkInfo = toSdkInfo(inputEntity)
    )

  override fun toModule(inputEntity: ModuleDetails): Module {
    val bspModuleDetails = BspModuleDetails(
      target = inputEntity.target,
      allTargetsIds = inputEntity.allTargetsIds,
      dependencySources = inputEntity.dependenciesSources,
      type = type,
      javacOptions = inputEntity.javacOptions,
      pythonOptions = inputEntity.pythonOptions,
    )

    return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails)
  }

  private fun toSdkInfo(inputEntity: ModuleDetails): PythonSdkInfo? {
    val pythonBuildTarget = extractPythonBuildTarget(inputEntity.target)
    return if (pythonBuildTarget != null) PythonSdkInfo(
      version = pythonBuildTarget.version,
      // TODO: check if the Path(...) is a valid way to do that
      interpreter = Path(pythonBuildTarget.interpreter)
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
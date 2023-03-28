package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntity
import java.net.URI
import kotlin.io.path.toPath

internal abstract class ModuleDetailsToModuleTransformer<out T : WorkspaceModelEntity> :
  WorkspaceModelEntityTransformer<ModuleDetails, T> {

  abstract val type: String

  abstract override fun transform(inputEntity: ModuleDetails): T

  protected fun toBaseDirContentRoot(inputEntity: ModuleDetails): ContentRoot =
    ContentRoot(
      // TODO what if null?
      url = URI.create(inputEntity.target.baseDirectory ?: "file:///todo").toPath()
    )

  protected fun toModule(inputEntity: ModuleDetails, includeJavacOpts: Boolean, includePythonOpts: Boolean): Module {
    val bspModuleDetails = BspModuleDetails(
      target = inputEntity.target,
      allTargetsIds = inputEntity.allTargetsIds,
      dependencySources = inputEntity.dependenciesSources,
      type = ModuleDetailsToJavaModuleTransformer.type,
      javacOptions = if (includeJavacOpts) inputEntity.javacOptions else null,
      pythonOptions = if (includePythonOpts) inputEntity.pythonOptions else null,
    )

    return BspModuleDetailsToModuleTransformer.transform(bspModuleDetails)
  }
}
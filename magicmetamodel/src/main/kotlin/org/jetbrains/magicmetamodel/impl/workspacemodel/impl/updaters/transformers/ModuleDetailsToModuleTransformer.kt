package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntity
import java.net.URI
import kotlin.io.path.toPath

internal abstract class ModuleDetailsToModuleTransformer<out T : WorkspaceModelEntity> (
  moduleNameProvider: ModuleNameProvider) :
  WorkspaceModelEntityTransformer<ModuleDetails, T> {

  protected abstract val type: String

  val bspModuleDetailsToModuleTransformer = BspModuleDetailsToModuleTransformer(moduleNameProvider)

  abstract override fun transform(inputEntity: ModuleDetails): T

  protected abstract fun toModule(inputEntity: ModuleDetails): Module

  protected fun toBaseDirContentRoot(inputEntity: ModuleDetails): ContentRoot =
    ContentRoot(
      // TODO what if null?
      url = URI.create(inputEntity.target.baseDirectory ?: "file:///todo").toPath(),
      excludedPaths = inputEntity.outputPathUris.map { URI.create(it).toPath() },
      )
}
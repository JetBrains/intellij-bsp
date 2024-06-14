package org.jetbrains.plugins.bsp.magicmetamodel.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfoOld
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesPython
import java.nio.file.Path

public object TargetIdToModuleEntitiesMap {
  public operator fun invoke(
    projectDetails: ProjectDetails,
    projectBasePath: Path,
    targetsMap: Map<BuildTargetId, BuildTargetInfoOld>,
    moduleNameProvider: TargetNameReformatProvider,
    libraryNameProvider: TargetNameReformatProvider,
    isAndroidSupportEnabled: Boolean,
    transformer: ProjectDetailsToModuleDetailsTransformer,
  ): Map<BuildTargetId, Module> {
    val moduleDetailsToJavaModuleTransformer = ModuleDetailsToJavaModuleTransformer(
      targetsMap,
      moduleNameProvider,
      libraryNameProvider,
      projectBasePath,
      isAndroidSupportEnabled,
    )

    return runBlocking(Dispatchers.Default) {
      projectDetails.targetsId.map {
        async {
          val moduleDetails = transformer.moduleDetailsForTargetId(it)
          if (moduleDetails.target.languageIds.includesPython()) {
            null
          } else {
            it.uri to moduleDetailsToJavaModuleTransformer.transform(moduleDetails)
          }
        }
      }.awaitAll().filterNotNull().toMap()
    }
  }
}

@TestOnly
public fun Collection<String>.toDefaultTargetsMap(): Map<BuildTargetId, BuildTargetInfoOld> =
  associateBy(
    keySelector = { it },
    valueTransform = { BuildTargetInfoOld(id = it) }
  )

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.ResourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonResourceRoot

internal object ResourcesItemToPythonResourceRootTransformer :
  WorkspaceModelEntityPartitionTransformer<ResourcesItem, PythonResourceRoot> {

  override fun transform(inputEntity: ResourcesItem): List<PythonResourceRoot> =
    inputEntity.resources
      .map {
        PythonResourceRoot(
          resourcePath = RawUriToDirectoryPathTransformer.transform(it)
        )
      }
      .distinct()
}

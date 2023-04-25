package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.DependencySourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonLibrary


internal object DependencySourcesItemToPythonLibraryTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesItem, PythonLibrary> {

  override fun transform(inputEntity: DependencySourcesItem): List<PythonLibrary> =
    inputEntity.sources.mapNotNull { PythonLibrary(it) }

}

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.jetbrains.bsp.bsp4kt.DependencySourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonLibrary

internal object DependencySourcesItemToPythonLibraryTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesItem, PythonLibrary> {

  override fun transform(inputEntity: DependencySourcesItem): List<PythonLibrary> =
    inputEntity.sources.mapNotNull { PythonLibrary(listOf(it)) }

}

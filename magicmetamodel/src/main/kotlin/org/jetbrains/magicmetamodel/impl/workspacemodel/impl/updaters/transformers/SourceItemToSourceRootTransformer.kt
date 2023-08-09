package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.jetbrains.bsp.bsp4kt.SourceItem
import com.jetbrains.bsp.bsp4kt.SourceItemKind
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelEntity
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

public data class SourceRoot(
  public val sourcePath: Path,
  public val generated: Boolean,
  public val isFile: Boolean,
) : WorkspaceModelEntity()

internal object SourceItemToSourceRootTransformer :
  WorkspaceModelEntityTransformer<SourceItem, SourceRoot> {
  override fun transform(inputEntity: SourceItem): SourceRoot {
    val sourceURI = URI.create(inputEntity.uri).toPath()

    return SourceRoot(sourceURI, inputEntity.generated, inputEntity.kind == SourceItemKind.File)
  }
}

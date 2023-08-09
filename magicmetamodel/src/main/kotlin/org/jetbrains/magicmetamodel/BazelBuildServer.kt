package org.jetbrains.magicmetamodel

import com.jetbrains.bsp.bsp4kt.BuildTargetIdentifier
import com.jetbrains.jsonrpc4kt.services.JsonRequest
import java.util.concurrent.CompletableFuture

public data class LibraryItem(
  val id: BuildTargetIdentifier,
  val dependencies: List<BuildTargetIdentifier>,
  val jars: List<String>,
)

public data class WorkspaceLibrariesResult(
  val libraries: List<LibraryItem>,
)

public data class LibraryDetails(
  val name: String,
  val roots: List<String>,
)

public interface BazelBuildServer {
  @JsonRequest("workspace/libraries")
  public fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult>
}

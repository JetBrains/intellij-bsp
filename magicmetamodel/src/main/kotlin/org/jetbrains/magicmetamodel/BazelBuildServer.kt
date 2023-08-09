package org.jetbrains.magicmetamodel

import com.jetbrains.bsp.bsp4kt.BuildTargetIdentifier
import com.jetbrains.jsonrpc4kt.services.JsonRequest
import kotlinx.serialization.Serializable
import java.util.concurrent.CompletableFuture

@Serializable
public data class LibraryItem(
  val id: BuildTargetIdentifier,
  val dependencies: List<BuildTargetIdentifier>,
  val jars: List<String>,
)

@Serializable
public data class WorkspaceLibrariesResult(
  val libraries: List<LibraryItem>,
)

@Serializable
public data class LibraryDetails(
  val name: String,
  val roots: List<String>,
)

public interface BazelBuildServer {
  @JsonRequest("workspace/libraries")
  public fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult>
}

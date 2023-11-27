@file:Suppress("MatchingDeclarationName")

package org.jetbrains.bsp.extended

import ch.epfl.scala.bsp4j.BuildTargetIdentifier

public data class WorkspaceInvalidTargetsResult(
  val targets: List<BuildTargetIdentifier>,
)

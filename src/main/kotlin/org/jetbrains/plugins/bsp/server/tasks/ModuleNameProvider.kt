package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier

public data class ModuleNaming(
  val originalTargetId :BuildTargetIdentifier,
  val separator: String,
  val targetPath: List<String>,
  val targetName: String,
)
public class ModuleNameProvider {
}
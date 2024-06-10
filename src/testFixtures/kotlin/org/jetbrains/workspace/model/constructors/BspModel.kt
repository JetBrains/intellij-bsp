package org.jetbrains.workspace.model.constructors

import ch.epfl.scala.bsp4j.SourceItemKind

public class SourceItem(
  uri: String,
  kind: SourceItemKind,
  generated: Boolean = false,
) : ch.epfl.scala.bsp4j.SourceItem(uri, kind, generated)

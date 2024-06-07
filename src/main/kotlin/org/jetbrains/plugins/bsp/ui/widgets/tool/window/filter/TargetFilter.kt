package org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter

import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfoOld
import org.jetbrains.plugins.bsp.target.TemporaryTargetUtils

public class TargetFilter(
  private val onFilterChange: () -> Unit,
) {
  public var currentFilter: FILTER = FILTER.OFF
    set(value) {
      if (field != value) {
        field = value
        onFilterChange()
      }
    }

  public fun isFilterOn(): Boolean = currentFilter != FILTER.OFF

  public fun getMatchingLoadedTargets(xd: TemporaryTargetUtils): List<BuildTargetInfoOld> =
    xd.allTargetIds().mapNotNull { xd.getBuildTargetInfoForId(it) }.filterTargets()

  private fun List<BuildTargetInfoOld>.filterTargets(): List<BuildTargetInfoOld> =
    this.filter(currentFilter.predicate)

  public enum class FILTER(public val predicate: (BuildTargetInfoOld) -> Boolean) {
    OFF({ true }),
    CAN_RUN({ it.capabilities.canRun }),
    CAN_TEST({ it.capabilities.canTest }),
  }
}

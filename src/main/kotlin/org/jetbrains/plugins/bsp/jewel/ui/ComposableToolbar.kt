package org.jetbrains.plugins.bsp.jewel.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.icons.AllIcons
import org.jetbrains.jewel.bridge.LocalComponent
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.modifier.trackComponentActivation
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.theme.iconButtonStyle
import org.jetbrains.jewel.ui.util.thenIf
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons

internal class ComposableToolbar(
  var panelState: ToolbarTab,
  val setPanelState: (ToolbarTab) -> Unit,
) {
  // predefined values to match the design of the toolbar created with Swing
  private val toolbarHeight = 19.dp
  private val componentsDistance = 8.dp
  private val buttonsDistance = 10.dp
  private val dividerThickness = 1.dp
  private val verticalDividerHeight = 32.dp
  private val iconButtonBoxSize = 16.dp

  @Composable
  private fun horizontalDivider() {
    Divider(modifier = Modifier.height(dividerThickness).fillMaxWidth())
  }

  @Composable
  private fun verticalDivider() {
    Divider(modifier = Modifier.height(verticalDividerHeight).width(dividerThickness))
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  private fun wrapWithTooltipInBox(tooltipText: String, content: @Composable () -> Unit) {
    Box(
      modifier = Modifier.size(iconButtonBoxSize),
    ) {
      Tooltip(tooltip = {
        Text(tooltipText)
      }) {
        content()
      }
    }
  }

  @Composable
  private fun actionButtonWithTooltip(tooltipText: String, iconPath: String, onClick: () -> Unit) {
    wrapWithTooltipInBox(tooltipText) {
      IconButton(
        onClick = onClick,
        modifier = Modifier.fillMaxSize()
      ) {
        Icon(
          resource = iconPath,
          iconClass = BspPluginIcons::class.java,
          contentDescription = null,
        )
      }
    }
  }

  @Composable
  private fun RowScope.targetsTabButton(
    tooltipText: String,
    iconPath: String,
    isSelected: () -> Boolean,
    onClick: () -> Unit,
  ) {
    wrapWithTooltipInBox(tooltipText) {
      IconButton(
        onClick = onClick,
        modifier = Modifier.fillMaxSize()
          .thenIf(isSelected()) {
            background(
              color = JewelTheme.iconButtonStyle.colors.backgroundPressed,
              shape = RoundedCornerShape(JewelTheme.iconButtonStyle.metrics.cornerSize),
            ).border(
              width = JewelTheme.iconButtonStyle.metrics.borderWidth,
              color = JewelTheme.iconButtonStyle.colors.backgroundPressed,
              shape = RoundedCornerShape(JewelTheme.iconButtonStyle.metrics.cornerSize),
            )
          }
      ) {
        Icon(
          resource = iconPath,
          iconClass = BspPluginIcons::class.java,
          contentDescription = null,
        )
      }
    }
  }

  @OptIn(ExperimentalJewelApi::class)
  @Composable
  private fun RowScope.buttonsRow(buttons: @Composable RowScope.() -> Unit) {
    Row(
      modifier = Modifier.trackComponentActivation(LocalComponent.current)
        .fillMaxHeight(),
      horizontalArrangement = Arrangement.spacedBy(buttonsDistance),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      buttons()
    }
  }

  @Composable
  private fun RowScope.actionsRow() {
    buttonsRow {
      actionButtonWithTooltip(BspPluginBundle.message("connect.action.text"), "icons/connect.svg") {}
      actionButtonWithTooltip(BspPluginBundle.message("reload.action.text"), "icons/reload.svg") {}
      actionButtonWithTooltip(BspPluginBundle.message("build.and.resync.action.text"), "icons/buildAndResync.svg") {}
      actionButtonWithTooltip(BspPluginBundle.message("disconnect.action.text"), "icons/disconnect.svg") {}
    }
  }

  @Composable
  private fun RowScope.targetsTabsRow() {
    buttonsRow {
      targetsTabButton(
        tooltipText = BspPluginBundle.message("widget.not.loaded.targets.tab.name"),
        iconPath = "icons/notLoaded.svg",
        isSelected = { panelState == ToolbarTab.NOT_LOADED_TARGETS },
        onClick = { setPanelState(ToolbarTab.NOT_LOADED_TARGETS) }
      )
      targetsTabButton(
        tooltipText = BspPluginBundle.message("widget.loaded.targets.tab.name"),
        iconPath = "icons/loaded.svg",
        isSelected = { panelState == ToolbarTab.LOADED_TARGETS },
        onClick = { setPanelState(ToolbarTab.LOADED_TARGETS) }
      )
    }
  }

  @OptIn(ExperimentalJewelApi::class)
  @Composable
  internal fun render() {
    horizontalDivider()
    Row(
      modifier = Modifier.trackComponentActivation(LocalComponent.current)
        .height(toolbarHeight)
        .padding(start = componentsDistance, end = componentsDistance),
      horizontalArrangement = Arrangement.spacedBy(componentsDistance),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      actionsRow()
      verticalDivider()
      targetsTabsRow()
      verticalDivider()
      Icon(
        "general/filter.svg",
        iconClass = AllIcons::class.java,
        contentDescription = "An icon",
      )
    }
    horizontalDivider()
  }
}

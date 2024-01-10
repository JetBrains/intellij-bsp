package org.jetbrains.plugins.bsp.jewel.ui.tree

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.TextTransferable
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.items
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleCapabilities
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.tasks.runBuildTargetTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService

internal class TargetActionPopupMenu(
  private val project: Project,
  // for which tree tab the popup menu is rendered
  private val generatedTreeTabName: TreeTabName,
  private var state: PopupMenuState,
  private val setState: (PopupMenuState) -> Unit,
) {
  private companion object {
    private val log = logger<TargetActionPopupMenu>()
  }

  private fun getActionsListForTarget(
    treeTabName: TreeTabName,
    targetCapabilities: ModuleCapabilities,
  ): List<TargetAction> {
    val actionList = mutableListOf<TargetAction>()
    actionList.add(CopyTargetIdAction())
    if (treeTabName == TreeTabName.LOADED_TARGETS) {
      updateActionSetWithTargetCapabilities(targetCapabilities, actionList)
    } else {
      actionList.add(LoadTargetAction())
    }
    return actionList
  }

  private fun updateActionSetWithTargetCapabilities(
    targetCapabilities: ModuleCapabilities,
    actionList: MutableList<TargetAction>,
  ) {
    if (targetCapabilities.canCompile) {
      actionList.add(BuildTargetAction())
    }
    if (targetCapabilities.canRun) {
      actionList.add(RunTargetAction())
    }
    if (targetCapabilities.canTest) {
      actionList.add(TestTargetAction())
    }
  }

  @Composable
  private fun renderActionPopupMenu(target: BuildTargetInfo) {
    TargetAction.initProject(project)
    val actions = remember { getActionsListForTarget(generatedTreeTabName, target.capabilities) }

    if (state == PopupMenuState.VISIBLE) {
      PopupMenu(
        onDismissRequest = { setState(PopupMenuState.HIDDEN); true },
        horizontalAlignment = Alignment.Start,
        content = {
          items(
            actions,
            isSelected = { false },
            onItemClick = {
              it.execute(target)
            },
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              it.display()
            }
          }
        },
      )
    }
  }

  @Composable
  fun attach(target: BuildTargetInfo?) {
    if (target != null) {
      renderActionPopupMenu(target)
    } else {
      // todo: execute valid actions for invalid targets
      log.warn("Can't render action popup menu for null target")
    }
  }
}

internal enum class PopupMenuState {
  HIDDEN,
  VISIBLE,
}

internal fun PopupMenuState.toggle(): PopupMenuState = when (this) {
  PopupMenuState.HIDDEN -> PopupMenuState.VISIBLE
  PopupMenuState.VISIBLE -> PopupMenuState.HIDDEN
}

private interface TargetAction {
  val displayName: String
  val iconPath: String

  companion object {
    val log = logger<TargetAction>()
    var project: Project? = null
      private set

    fun initProject(newProjectVal: Project) = if (project == null) project = newProjectVal else Unit
  }

  @Composable
  fun display() {
    Icon(
      resource = iconPath,
      iconClass = AllIcons::class.java,
      contentDescription = null,
    )
    Text(displayName)
  }

  fun execute(target: BuildTargetInfo)
}

private class CopyTargetIdAction : TargetAction {
  override val displayName: String = BspPluginBundle.message("widget.copy.target.id")
  override val iconPath: String = "actions/copy.svg"

  @Composable
  override fun display() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row {
        super.display()
        Spacer(Modifier.width(24.dp))
        // Currently copying target id does not work with the use of Ctrl+c
        Text(text = "Ctrl+C", style = TextStyle(textDecoration = TextDecoration.LineThrough))
      }
      // Divider is currently a part of Row of Menu item, meaning it is highlighted when hovered
      // in order to change it, one would have to modify JewelTheme MenuStyle,
      // current MenuStyle is default for PopupMenu function
      Divider(Modifier.height(1.dp).fillMaxWidth())
    }
  }

  override fun execute(target: BuildTargetInfo) {
    val clipboard = CopyPasteManager.getInstance()
    val transferable = TextTransferable(target.id as CharSequence)
    clipboard.setContents(transferable)
  }
}

private class BuildTargetAction : TargetAction {
  override val displayName: String = BspPluginBundle.message("widget.build.target.popup.message")
  override val iconPath: String = "toolwindows/toolWindowBuild.svg"

  override fun execute(target: BuildTargetInfo) {
    val project = TargetAction.project!!
    BspCoroutineService.getInstance(project).start {
      runBuildTargetTask(listOf(target.id.toBsp4JTargetIdentifier()), project, TargetAction.log)
    }
  }
}

private class RunTargetAction : TargetAction {
  override val displayName: String = BspPluginBundle.message("widget.run.target.popup.message")
  override val iconPath: String = "runConfigurations/testState/run.svg"

  override fun execute(target: BuildTargetInfo) {
    println("Run action is yet not implemented. Triggered for target: $target ")
  }
}

private class TestTargetAction : TargetAction {
  override val displayName: String = BspPluginBundle.message("widget.test.target.popup.message")
  override val iconPath: String = "runConfigurations/testState/run.svg"

  override fun execute(target: BuildTargetInfo) {
    println("Test action is yet not implemented. Triggered for target: $target")
  }
}

private class LoadTargetAction : TargetAction {
  override val displayName: String = BspPluginBundle.message("widget.load.target.popup.message")
  override val iconPath: String = "actions/download.svg"

  override fun execute(target: BuildTargetInfo) {
    val project = TargetAction.project!!
    BspCoroutineService.getInstance(project).start {
      org.jetbrains.plugins.bsp.ui.actions.LoadTargetAction.loadTarget(project, target.id)
    }
  }
}

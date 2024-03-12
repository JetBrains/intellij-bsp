package org.jetbrains.plugins.bsp.sbt

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SbtBuildTarget
import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider

public interface SbtBuildModuleBspExtension {
    public fun enrichBspSbtModule(
        sbtBuildTarget: SbtBuildTarget,
        baseDirectory: String,
        projectSystemId: ProjectSystemId,
        buildTargetIdentifier: BuildTargetIdentifier,
        modelsProvider: IdeModifiableModelsProvider,
    )
}
private val ep =
    ExtensionPointName.create<SbtBuildModuleBspExtension>(
        "org.jetbrains.bsp.sbtBuildModuleBspExtension",
    )

internal fun sbtBuildModuleBspExtension(): SbtBuildModuleBspExtension? =
    ep.extensionList.firstOrNull()

internal fun sbtBuildModuleBspExtensionExists(): Boolean =
    ep.extensionList.isNotEmpty()

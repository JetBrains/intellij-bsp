package org.jetbrains.plugins.bsp.projectStructure

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.project.Project

internal abstract class TestableProjectStructureDiffBase : ProjectStructureDiff {
  val updates: MutableList<BuildTargetIdentifier> = mutableListOf()
  var hasBeenApplied: Boolean = false

  override suspend fun apply(project: Project) {
    hasBeenApplied = true
  }
}

internal class TestableProjectStructureDiff : TestableProjectStructureDiffBase()
internal class AnotherTestableProjectStructureDiff : TestableProjectStructureDiffBase()

internal abstract class TestableProjectStructureUpdaterBase<T: TestableProjectStructureDiffBase>(
  private val unsupportedTargets: List<BuildTargetIdentifier>
) : ProjectStructureUpdater<T> {
  override fun isSupported(buildTarget: BuildTarget): Boolean =
    buildTarget.id !in unsupportedTargets

  override fun addTarget(project: Project, targetInfo: BuildTargetInfo, diff: T) {
    diff.updates.add(targetInfo.target.id)
  }
}

internal class TestableProjectStructureUpdater(
  unsupportedTargets: List<BuildTargetIdentifier>
) : TestableProjectStructureUpdaterBase<TestableProjectStructureDiff>(unsupportedTargets) {
  override val diffClass: Class<TestableProjectStructureDiff> = TestableProjectStructureDiff::class.java
}

internal class AnotherTestableProjectStructureUpdater(
  unsupportedTargets: List<BuildTargetIdentifier>
) : TestableProjectStructureUpdaterBase<AnotherTestableProjectStructureDiff>(unsupportedTargets) {
  override val diffClass: Class<AnotherTestableProjectStructureDiff> = AnotherTestableProjectStructureDiff::class.java
}

internal class TestableProjectStructureProvider<TDiff: TestableProjectStructureDiffBase>(
  private val diff: TDiff
) : ProjectStructureProvider<TDiff, Any> {
  override fun newDiff(project: Project): TDiff = diff

  override fun current(project: Project): Any {
    TODO("Not yet implemented")
  }
}

internal fun BuildTargetIdentifier.toMockBuildTargetInfo(): BuildTargetInfo =
  BuildTargetInfo(
    target = BuildTarget(this, emptyList(), emptyList(), emptyList(), BuildTargetCapabilities()),
  )

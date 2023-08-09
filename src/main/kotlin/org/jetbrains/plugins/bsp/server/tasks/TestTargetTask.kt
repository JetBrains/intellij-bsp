package org.jetbrains.plugins.bsp.server.tasks

import com.intellij.openapi.project.Project
import com.jetbrains.bsp.bsp4kt.BuildServerCapabilities
import com.jetbrains.bsp.bsp4kt.BuildTargetIdentifier
import com.jetbrains.bsp.bsp4kt.TestParams
import com.jetbrains.bsp.bsp4kt.TestResult
import org.jetbrains.plugins.bsp.server.connection.BspServer
import java.util.UUID

public class TestTargetTask(project: Project) : BspServerSingleTargetTask<TestResult>("test target", project) {

  protected override fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier
  ): TestResult {
    val params = createTestParams(targetId)

    return server.buildTargetTest(params).get()
  }

  private fun createTestParams(targetId: BuildTargetIdentifier): TestParams =
    TestParams(
      listOf(targetId),
      // TODO
      originId = "test-" + UUID.randomUUID().toString(),
      arguments = emptyList()
    )
}

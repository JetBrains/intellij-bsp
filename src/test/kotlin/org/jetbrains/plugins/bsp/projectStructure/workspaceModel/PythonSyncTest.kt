package org.jetbrains.plugins.bsp.projectStructure.workspaceModel

import com.intellij.openapi.util.registry.Registry
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.server.tasks.PythonSync
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PythonSync tests")
class PythonSyncTest : MockProjectBaseTest() {
  private lateinit var hook: PythonSync

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    hook = PythonSync()
  }

  @Nested
  @DisplayName("PythonSync.isEnabled tests")
  inner class IsEnabledTest {
    @Test
    fun `should return false if feature flag is disabled`() {
      // given
      Registry.get("bsp.python.support").setValue(false)

      // when & then
      hook.isEnabled(project) shouldBe false
    }

    @Test
    fun `should return true if feature flag is enabled`() {
      // given
      Registry.get("bsp.python.support").setValue(true)

      // when & then
      hook.isEnabled(project) shouldBe true
    }
  }

  @Nested
  @DisplayName("PythonSync.execute tests")
  inner class ExecuteTests {

  }
}
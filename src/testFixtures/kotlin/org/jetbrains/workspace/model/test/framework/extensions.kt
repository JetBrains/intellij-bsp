package org.jetbrains.workspace.model.test.framework

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.ExtensionPointName

// TODO: this should not be needed, but for some reason when building with Bazel the EP is not registered
inline fun <reified T : Any> registerExtensionPoint(extensionPoint: ExtensionPointName<T>) {
  ApplicationManager.getApplication().apply {
    if (!extensionArea.hasExtensionPoint(extensionPoint.name)) {
      extensionArea.registerExtensionPoint(
        extensionPoint.name,
        T::class.java.name,
        ExtensionPoint.Kind.INTERFACE,
        false
      )
    }
  }
}
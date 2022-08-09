package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.protocol.connection.LocatedBspConnectionDetails

public class BspUtilService {

  public var bspConnectionDetails: HashMap<String, LocatedBspConnectionDetails> = hashMapOf()
  public val loadedViaBspFile: MutableSet<String> = mutableSetOf()

  public companion object {

    public const val coursierInstallCommand: String = "cs launch org.jetbrains.bsp:bazel-bsp:2.1.0 -M org.jetbrains.bsp.bazel.install.Install"
    public fun getInstance(): BspUtilService =
      ApplicationManager.getApplication().getService(BspUtilService::class.java)
  }
}

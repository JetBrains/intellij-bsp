package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel

public data class PythonSdkInfo(val version: String, val originalName: String) {
  override fun toString(): String = "$originalName$SEPARATOR$version"

  public companion object {
    public const val PYTHON_SDK_ID: String = "PythonSDK"
    private const val SEPARATOR = '-'
    public fun fromString(value: String): PythonSdkInfo? {
      val parts = value.split(SEPARATOR)
      return parts.takeIf { it.size == 2 }?.let {
        PythonSdkInfo(it[0], it[1])
      }
    }
  }
}


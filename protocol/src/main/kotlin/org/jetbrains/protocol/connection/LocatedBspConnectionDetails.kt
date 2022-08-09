package org.jetbrains.protocol.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

public data class LocatedBspConnectionDetails(
  val bspConnectionDetails: BspConnectionDetails,
  val connectionFileLocation: VirtualFile,
)

// TODO visib??
public object LocatedBspConnectionDetailsParser {

  private val log = logger<LocatedBspConnectionDetailsParser>()

  public fun parseFromFile(file: VirtualFile): LocatedBspConnectionDetails? =
    parseBspConnectionDetails(file)?.let {
      LocatedBspConnectionDetails(
        bspConnectionDetails = it,
        connectionFileLocation = file,
      )
    }

  private fun parseBspConnectionDetails(file: VirtualFile): BspConnectionDetails? =
    try {
      Gson().fromJson(VfsUtil.loadText(file), BspConnectionDetails::class.java)
    } catch (e: Exception) {
      log.info("Parsing file '$file' to BspConnectionDetails failed!", e)
      null
    }
}

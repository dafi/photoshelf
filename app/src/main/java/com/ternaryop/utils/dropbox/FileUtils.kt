package com.ternaryop.utils.dropbox

import com.dropbox.core.v2.files.WriteMode
import java.io.File
import java.io.FileInputStream

private fun dropboxPath(exportFile: File): String = "/${exportFile.name}"

fun DropboxManager.copyFile(exportPath: String) {
    if (isLinked) {
        val exportFile = File(exportPath)
        FileInputStream(exportFile).use { stream ->
            // Autorename = true and Mode = OVERWRITE allow to overwrite
            // the file if it exists or create it if doesn't
            checkNotNull(client)
                .files()
                .uploadBuilder(dropboxPath(exportFile))
                .withAutorename(true)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(stream)
        }
    }
}

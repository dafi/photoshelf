package com.ternaryop.photoshelf.test

import com.ternaryop.photoshelf.parsers.JSONTitleParserConfig
import com.ternaryop.photoshelf.parsers.TitleParser
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.Arrays
import java.util.Locale

/**
 * Created by dave on 08/01/18.
 * This isn't a test but help to save to file the formatted output
 */
class TitleSaveFormattedTest {
    @Test
    fun saveFormattedTitles() {
        val titlesDirectoryPath = "/opt/devel/0dafiprj/git.github/post_title_downloader/titles/"
        val formattedPath = "/opt/devel/0dafiprj/git.github/post_title_downloader/titles/all-formatted.txt"

        val titleList = File(titlesDirectoryPath).listFiles { file -> file.name.toLowerCase(Locale.US).endsWith(".txt") }
        val formattedFile = File(formattedPath)

        if (!containsNewerThan(titleList, formattedFile)) {
            return
        }
        Arrays.sort(titleList)

        try {
            PrintWriter(FileWriter(formattedPath)).use { pw ->
                val configPath = File(File(".").absoluteFile, "app/src/main/assets/titleParser.json").absolutePath
                val instance = TitleParser.instance(JSONTitleParserConfig(configPath))

                for (f in titleList) {
                    appendFormatted(f, pw, instance)
                }
                pw.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            assertFalse(e.message, true)
        }
    }

    private fun containsNewerThan(files: Array<File>, newerFile: File): Boolean {
        if (!newerFile.exists()) {
            return true
        }
        val newerLastModified = newerFile.lastModified()
        return files.any { it.lastModified() > newerLastModified }
    }

    @Throws(Exception::class)
    private fun appendFormatted(file: File, appender: PrintWriter, titleParser: TitleParser) {
        val sb = StringBuilder()
        file.forEachLine {
            val titleData = titleParser.parseTitle(it, false, false)
            sb.setLength(0)
            titleData.formatWho("", "", "", "", sb)
            appender.println(sb)
        }
    }
}

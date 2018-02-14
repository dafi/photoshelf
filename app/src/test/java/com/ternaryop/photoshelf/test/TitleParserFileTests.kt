package com.ternaryop.photoshelf.test

import com.ternaryop.photoshelf.parsers.JSONTitleParserConfig
import com.ternaryop.photoshelf.parsers.TitleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.Calendar

// To run this test be sure the Build Variants window is open then select Unit Tests
// see more http://developer.android.com/training/testing/unit-testing/local-unit-tests.html#run
@RunWith(Parameterized::class)
class TitleParserFileTests(private var inputTitle: String, private var expectedTitle: String, private var lineNumber: Int) {
    @Test
    fun testTitle() {
        try {
            val configPath = File(File(".").absoluteFile, "app/src/main/assets/titleParser.json").absolutePath
            val titleData = TitleParser.instance(JSONTitleParserConfig(configPath)).parseTitle(inputTitle, false, false)

            val formattedInput = titleData.format("<strong>", "</strong>", "<em>", "</em>")
            if (expectedTitle != formattedInput) {
                println("[$lineNumber] \"$inputTitle\",")
                println("//expected $expectedTitle")
                println("//found    $formattedInput")
                println()
            }
            assertEquals(expectedTitle, formattedInput)
        } catch (e: Exception) {
            e.printStackTrace()
            assertFalse(e.message, true)
        }
    }

    companion object {
        private const val inputPath = "/opt/devel/0dafiprj/git.github/post_title_downloader/photoshelf_tests/titleParser-test-input.txt"
        private const val resultsPath = "/opt/devel/0dafiprj/git.github/post_title_downloader/photoshelf_tests/titleParser-test-results.txt"

        @JvmStatic
        @Parameterized.Parameters
        @Throws(IOException::class)
        fun data(): Collection<Array<Any>> {
            var inputReader: BufferedReader? = null
            var resultReader: BufferedReader? = null
            val objects = ArrayList<Array<Any>>()

            try {
                inputReader = BufferedReader(InputStreamReader(FileInputStream(inputPath), "UTF-8"))
                resultReader = BufferedReader(InputStreamReader(FileInputStream(resultsPath), "UTF-8"))
                val year = Calendar.getInstance().get(Calendar.YEAR).toString()

                var inputLine: String
                var resultLine: String
                var lineNumber = 1

                while (true) {
                    inputLine = inputReader.readLine() ?: break
                    resultLine = resultReader.readLine() ?: break

                    resultLine = resultLine.replace("%CURRENT_YEAR%", year)
                    objects.add(arrayOf(inputLine, resultLine, lineNumber++))
                }
            } finally {
                if (inputReader != null) try {
                    inputReader.close()
                } catch (ignored: IOException) {
                }

                if (resultReader != null) try {
                    resultReader.close()
                } catch (ignored: IOException) {
                }
            }
            return objects
        }
    }
}

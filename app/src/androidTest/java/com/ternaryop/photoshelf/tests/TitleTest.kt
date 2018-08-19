package com.ternaryop.photoshelf.tests

import androidx.test.InstrumentationRegistry.getTargetContext
import androidx.test.runner.AndroidJUnit4
import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig
import com.ternaryop.photoshelf.parsers.TitleParser
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by dave on 21/09/14.
 * Ready to run test to used inside the emulator
 */
@RunWith(AndroidJUnit4::class)
class TitleTest {
    @Test
    fun testTitle() {
        // fill the array with titles
        val arr = arrayOf(
                ""
        )
        try {
            val config = AndroidTitleParserConfig(getTargetContext())
            val sb = StringBuilder()
            for (i in arr) {
                val titleData = TitleParser.instance(config).parseTitle(i, false)

                val formattedInput = titleData.format("<strong>", "</strong>", "<em>", "</em>")
                // i: input
                // s: simple without format
                // e: expected formatted
                sb.append("i:").append(i).append("\n")
                        .append("s:").append(titleData.format("", "", "", "")).append("\n")
                        .append("e:").append(formattedInput)
                        .append("\n\n")
                println("testTitle $formattedInput")
            }
            val s = sb.toString()
            // setting the breakpoint to the println allows to copy to clipboard the whole string
            // this is necessary because the android log cuts the result
            println("whole string\n$s")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

package com.rcmiku.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YrcParserTest {

    @Test
    fun parseYrc_standardWordByWordLine_parsesTimesAndCharBounds() {
        val raw = """
            [12340,2500](12340,400,0)猜(12740,500,0)不(13240,600,0)透(13840,300,0) 你(14140,700,0)的心
        """.trimIndent()

        val lines = raw.parseYrc()
        assertEquals(1, lines.size)

        val line = lines.first()
        assertEquals(12340L, line.time)
        assertEquals(2500L, line.durationMs)
        assertEquals("猜不透 你的心", line.text)
        assertEquals(5, line.words.size)

        assertEquals(YrcWord(12340L, 400L, 0, 1), line.words[0])
        assertEquals(YrcWord(12740L, 500L, 1, 2), line.words[1])
        assertEquals(YrcWord(13240L, 600L, 2, 3), line.words[2])
        assertEquals(YrcWord(13840L, 300L, 3, 5), line.words[3])
        assertEquals(YrcWord(14140L, 700L, 5, 7), line.words[4])
    }

    @Test
    fun parseYrc_withJsonMetadataHeader_parsesMetadataAndSortsByTime() {
        val raw = """
            {"t":0,"c":[{"tx":"作词: "},{"tx":"黄婷"}]}
            [15000,1800](15000,600,0)第二(15600,600,0)句
            [10000,1500](10000,500,0)第一(10500,500,0)句
        """.trimIndent()

        val lines = raw.parseYrc()
        assertEquals(3, lines.size)

        assertEquals(0L, lines[0].time)
        assertEquals("作词: 黄婷", lines[0].text)
        assertTrue(lines[0].words.isEmpty())

        assertEquals(10000L, lines[1].time)
        assertEquals("第一句", lines[1].text)
        assertEquals(2, lines[1].words.size)

        assertEquals(15000L, lines[2].time)
        assertEquals("第二句", lines[2].text)
    }

    @Test
    fun parseYrc_blankOrMalformedLines_handledSafely() {
        assertTrue("".parseYrc().isEmpty())
        assertTrue("   \nnot a yrc line\n{\"t\":0,\"c\":[]}\n[invalid]".parseYrc().isEmpty())
    }

    @Test
    fun parseLrc_regression_parsesStandardLines() {
        val raw = """
            [00:12.34]第一句歌词
            [01:05.678]第二句歌词
            invalid line
        """.trimIndent()

        val lines = raw.parseLrc()
        assertEquals(2, lines.size)
        assertEquals(12340L, lines[0].time)
        assertEquals("第一句歌词", lines[0].text)
        assertTrue(lines[0].words.isEmpty())
        assertEquals(65678L, lines[1].time)
        assertEquals("第二句歌词", lines[1].text)
    }
}

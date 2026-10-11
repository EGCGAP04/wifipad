package io.github.egcgap04.wifipad.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParseArgsTest {
    private fun parse(vararg args: String) = parseArgs(arrayOf(*args))

    @Test
    fun `defaults to the protocol port`() {
        assertEquals(Options(port = 7654, help = false), parse())
    }

    @Test
    fun `parses the port`() {
        assertEquals(9000, parse("--port", "9000").port)
        assertEquals(1, parse("--port", "1").port)
        assertEquals(65535, parse("--port", "65535").port)
    }

    @Test
    fun `parses help`() {
        assertEquals(true, parse("--help").help)
        assertEquals(true, parse("-h").help)
    }

    @Test
    fun `rejects invalid arguments`() {
        for (args in listOf(
            arrayOf("--port"),
            arrayOf("--port", "abc"),
            arrayOf("--port", "0"),
            arrayOf("--port", "65536"),
            arrayOf("--port=7654"),
            arrayOf("7654"),
        )) {
            assertFailsWith<IllegalArgumentException>(args.joinToString(" ")) { parseArgs(args) }
        }
    }
}

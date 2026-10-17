package io.github.egcgap04.wifipad.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConnectionSettingsTest {
    @Test
    fun `parses IPv4 addresses`() {
        assertEquals("192.168.0.10", ConnectionSettings.parseHost("192.168.0.10"))
        assertEquals("10.0.2.2", ConnectionSettings.parseHost(" 10.0.2.2 "))
        assertEquals("0.0.0.0", ConnectionSettings.parseHost("0.0.0.0"))
        assertEquals("255.255.255.255", ConnectionSettings.parseHost("255.255.255.255"))
    }

    @Test
    fun `rejects anything else as host`() {
        for (text in listOf("", "192.168.0", "192.168.0.10.1", "192.168.0.256", "192.168..10", "1234.0.0.1", "a.b.c.d", "-1.0.0.1", "+1.0.0.1", "192.168.0.10:7654")) {
            assertNull(ConnectionSettings.parseHost(text), text)
        }
    }

    @Test
    fun `parses ports`() {
        assertEquals(7654, ConnectionSettings.parsePort("7654"))
        assertEquals(1, ConnectionSettings.parsePort("1"))
        assertEquals(65535, ConnectionSettings.parsePort(" 65535 "))
    }

    @Test
    fun `rejects invalid ports`() {
        for (text in listOf("", "0", "65536", "-1", "abc", "76 54")) {
            assertNull(ConnectionSettings.parsePort(text), text)
        }
    }
}

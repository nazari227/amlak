package com.example.network

import okhttp3.Dns
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.net.UnknownHostException

class NetworkSafetyTest {

    @Test
    fun safeDnsConvertsSecurityExceptionIntoNetworkFailure() {
        val dns = SafeDns(Dns { throw SecurityException("permission denied") })

        try {
            dns.lookup("my.ashianmelk.ir")
            fail("Expected UnknownHostException")
        } catch (error: UnknownHostException) {
            assertTrue(error.cause is SecurityException)
        }
    }

    @Test
    fun manifestDeclaresRealAndroidNetworkPermissions() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertTrue(manifest.contains("android.permission.VIBRATE"))

        assertFalse(manifest.contains("android.intent.permission.INTERNET"))
        assertFalse(manifest.contains("android.intent.permission.ACCESS_NETWORK_STATE"))
        assertFalse(manifest.contains("android.intent.permission.VIBRATE"))
    }
}

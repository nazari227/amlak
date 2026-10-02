package com.example.network

import okhttp3.Dns
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class NetworkSafetyTest {

    @Test
    fun safeDnsConvertsSecurityExceptionIntoNetworkFailure() {
        val blockedDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                throw SecurityException("permission denied")
            }
        }
        val dns = SafeDns(blockedDns)

        try {
            dns.lookup("my.ashianmelk.ir")
            fail("Expected UnknownHostException")
        } catch (error: UnknownHostException) {
            assertTrue(error.cause is SecurityException)
        }
    }
}

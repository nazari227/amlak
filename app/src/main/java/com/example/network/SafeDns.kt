package com.example.network

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * OkHttp's system DNS can surface a SecurityException on devices when the
 * application lacks network permission. Convert that runtime exception into
 * an IOException subtype so Retrofit/OkHttp delivers it through onFailure
 * instead of terminating the process.
 */
internal class SafeDns(
    private val delegate: Dns = Dns.SYSTEM
) : Dns {

    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            delegate.lookup(hostname)
        } catch (error: SecurityException) {
            throw UnknownHostException("DNS lookup blocked for $hostname").also {
                it.initCause(error)
            }
        }
    }
}

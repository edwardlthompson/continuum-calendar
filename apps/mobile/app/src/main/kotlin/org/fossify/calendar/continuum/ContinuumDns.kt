package org.fossify.calendar.continuum

import android.net.Network
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ThreadLocalRandom
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

/**
 * Resolve hostnames when OEM DNS/UDP is broken for the app process.
 * Order: system → UDP → DoH (TCP/TLS to 8.8.8.8) → known Google frontend IPs.
 */
object ContinuumDns {
    /** Any Google anycast IP works with SNI host oauth2.googleapis.com. */
    private val OAUTH2_FALLBACK_IPS = listOf(
        "142.251.107.95",
        "142.250.190.14",
        "172.217.164.106",
        "216.58.214.174",
        "142.250.72.206",
        "74.125.138.95",
    )

    fun lookup(network: Network?, host: String): List<InetAddress> {
        try {
            val sys = if (network != null) {
                network.getAllByName(host).toList()
            } else {
                InetAddress.getAllByName(host).toList()
            }
            if (sys.isNotEmpty()) return sys
        } catch (e: Exception) {
            ContinuumDiagnostics.w("System DNS failed for $host", e)
        }

        for (server in listOf("8.8.8.8", "1.1.1.1")) {
            try {
                val v4 = queryUdp(network, host, server)
                if (v4.isNotEmpty()) return v4
            } catch (e: Exception) {
                ContinuumDiagnostics.w("UDP DNS via $server failed", e)
            }
        }

        try {
            val doh = queryDoh(network, host)
            if (doh.isNotEmpty()) return doh
        } catch (e: Exception) {
            ContinuumDiagnostics.w("DoH DNS failed for $host", e)
        }

        if (host == "oauth2.googleapis.com" || host.endsWith(".googleapis.com")) {
            ContinuumDiagnostics.w("Using hardcoded Google frontend IPs for $host")
            return OAUTH2_FALLBACK_IPS.map { literalIp(it) }
        }

        throw java.net.UnknownHostException("No DNS answer for $host")
    }

    private fun queryUdp(network: Network?, host: String, dnsServer: String): List<InetAddress> {
        val id = ThreadLocalRandom.current().nextInt(0, 65535)
        val q = buildQuery(id, host, type = 1)
        // Prefer network socket factory — DatagramSocket() is ECONNREFUSED on some OEMs when process-bound.
        val socket = try {
            if (network != null) {
                // Android Network has no datagram factory; create unbound then bindSocket.
                DatagramSocket().also { network.bindSocket(it) }
            } else {
                DatagramSocket()
            }
        } catch (e: Exception) {
            // Last resort: plain datagram without bind.
            DatagramSocket()
        }
        try {
            socket.soTimeout = 4_000
            val dest = literalIp(dnsServer)
            socket.send(DatagramPacket(q, q.size, dest, 53))
            val buf = ByteArray(512)
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)
            return parseAnswers(buf, resp.length, id)
        } finally {
            socket.close()
        }
    }

    /** DNS-over-HTTPS to 8.8.8.8 (TCP works when UDP socket() is refused). */
    private fun queryDoh(network: Network?, host: String): List<InetAddress> {
        val path = "/resolve?name=${java.net.URLEncoder.encode(host, "UTF-8")}&type=A"
        val raw: Socket = if (network != null) {
            network.socketFactory.createSocket()
        } else {
            Socket()
        }
        raw.connect(InetSocketAddress(literalIp("8.8.8.8"), 443), 10_000)
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, null, null)
        val ssl = ctx.socketFactory.createSocket(raw, "dns.google", 443, true) as SSLSocket
        try {
            val params = ssl.sslParameters
            params.serverNames = listOf(SNIHostName("dns.google"))
            ssl.sslParameters = params
            ssl.soTimeout = 12_000
            ssl.startHandshake()
            val req = buildString {
                append("GET $path HTTP/1.1\r\n")
                append("Host: dns.google\r\n")
                append("Accept: application/dns-json\r\n")
                append("Connection: close\r\n\r\n")
            }
            ssl.outputStream.write(req.toByteArray(Charsets.US_ASCII))
            ssl.outputStream.flush()
            val text = ssl.inputStream.bufferedReader().readText()
            val body = text.substringAfter("\r\n\r\n").ifBlank {
                text.substringAfter("\n\n")
            }
            val json = JSONObject(body)
            val answers = json.optJSONArray("Answer") ?: return emptyList()
            val out = ArrayList<InetAddress>()
            for (i in 0 until answers.length()) {
                val a = answers.optJSONObject(i) ?: continue
                if (a.optInt("type") != 1) continue
                val data = a.optString("data")
                if (data.matches(Regex("""\d+\.\d+\.\d+\.\d+"""))) {
                    out.add(literalIp(data))
                }
            }
            return out
        } finally {
            try {
                ssl.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun buildQuery(id: Int, host: String, type: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(id shr 8)
        out.write(id and 0xff)
        out.write(0x01)
        out.write(0x00)
        out.write(0x00)
        out.write(0x01)
        out.write(0x00)
        out.write(0x00)
        out.write(0x00)
        out.write(0x00)
        out.write(0x00)
        out.write(0x00)
        for (label in host.split('.')) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write(0)
        out.write(0x00)
        out.write(type)
        out.write(0x00)
        out.write(0x01)
        return out.toByteArray()
    }

    private fun parseAnswers(data: ByteArray, len: Int, id: Int): List<InetAddress> {
        if (len < 12) return emptyList()
        val buf = ByteBuffer.wrap(data, 0, len)
        val respId = buf.short.toInt() and 0xffff
        if (respId != id) return emptyList()
        buf.short
        val qd = buf.short.toInt() and 0xffff
        val an = buf.short.toInt() and 0xffff
        buf.short
        buf.short
        repeat(qd) { skipName(buf); buf.short; buf.short }
        val out = ArrayList<InetAddress>()
        repeat(an) {
            skipName(buf)
            val type = buf.short.toInt() and 0xffff
            buf.short
            buf.int
            val rdlen = buf.short.toInt() and 0xffff
            if (type == 1 && rdlen == 4) {
                val addr = ByteArray(4)
                buf.get(addr)
                out.add(InetAddress.getByAddress(addr))
            } else {
                buf.position(buf.position() + rdlen)
            }
        }
        return out
    }

    private fun skipName(buf: ByteBuffer) {
        while (true) {
            val len = buf.get().toInt() and 0xff
            if (len == 0) return
            if (len and 0xc0 == 0xc0) {
                buf.get()
                return
            }
            buf.position(buf.position() + len)
        }
    }

    private fun literalIp(ip: String): InetAddress {
        val parts = ip.split('.')
        require(parts.size == 4)
        val bytes = ByteArray(4) { parts[it].toInt().and(0xff).toByte() }
        return InetAddress.getByAddress(bytes)
    }
}

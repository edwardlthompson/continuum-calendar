package org.fossify.calendar.continuum

import android.content.Context
import android.net.Network
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

/** HTTPS POST that resolves DNS via [ContinuumDns] (works when system resolver is broken). */
object ContinuumHttpsPost {
    data class Response(val code: Int, val body: String)

    fun postForm(context: Context, urlString: String, formBody: String): Response {
        val url = URL(urlString)
        val host = url.host ?: error("Missing host")
        val port = if (url.port != -1) url.port else 443
        val path = buildString {
            append(url.path.ifBlank { "/" })
            if (!url.query.isNullOrBlank()) append('?').append(url.query)
        }
        val network = ContinuumHttp.waitForInternetNetwork(context)
        // Do NOT bindProcessToNetwork before DNS — on this OEM it makes DatagramSocket() ECONNREFUSED.
        val addrs = try {
            ContinuumDns.lookup(network, host)
        } catch (e: Exception) {
            ContinuumDiagnostics.w("DNS lookup failed, retry unbound", e)
            ContinuumDns.lookup(null, host)
        }
        ContinuumDiagnostics.i("Resolved $host -> ${addrs.joinToString { it.hostAddress ?: "?" }}")
        var last: Exception? = null
        for (addr in addrs) {
            val ip = addr.hostAddress ?: continue
            try {
                return postOnce(network, ip, host, port, path, formBody)
            } catch (e: Exception) {
                last = e
                ContinuumDiagnostics.w("HTTPS post via $ip (network) failed", e)
                try {
                    return postOnce(null, ip, host, port, path, formBody)
                } catch (e2: Exception) {
                    last = e2
                    ContinuumDiagnostics.w("HTTPS post via $ip (unbound) failed", e2)
                }
            }
        }
        throw last ?: IllegalStateException("HTTPS post failed for $host")
    }

    private fun postOnce(
        network: Network?,
        ip: String,
        host: String,
        port: Int,
        path: String,
        formBody: String,
    ): Response {
        val raw: Socket = if (network != null) {
            network.socketFactory.createSocket()
        } else {
            Socket()
        }
        raw.connect(InetSocketAddress(ip, port), 15_000)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, null, null)
        val ssl = sslContext.socketFactory.createSocket(raw, host, port, true) as SSLSocket
        try {
            val params = ssl.sslParameters
            params.serverNames = listOf(SNIHostName(host))
            ssl.sslParameters = params
            ssl.soTimeout = 20_000
            ssl.startHandshake()
            val bodyBytes = formBody.toByteArray(Charsets.UTF_8)
            val writer = OutputStreamWriter(ssl.outputStream, Charsets.UTF_8)
            writer.write("POST $path HTTP/1.1\r\n")
            writer.write("Host: $host\r\n")
            writer.write("Content-Type: application/x-www-form-urlencoded\r\n")
            writer.write("Content-Length: ${bodyBytes.size}\r\n")
            writer.write("Connection: close\r\n")
            writer.write("Accept: application/json\r\n")
            writer.write("\r\n")
            writer.flush()
            ssl.outputStream.write(bodyBytes)
            ssl.outputStream.flush()
            val reader = BufferedReader(InputStreamReader(ssl.inputStream, Charsets.UTF_8))
            val status = reader.readLine() ?: error("Empty HTTPS response")
            val code = status.split(' ').getOrNull(1)?.toIntOrNull() ?: 0
            var contentLength = -1
            var chunked = false
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val sep = line.indexOf(':')
                if (sep <= 0) continue
                val name = line.substring(0, sep).trim().lowercase()
                val value = line.substring(sep + 1).trim()
                when (name) {
                    "content-length" -> contentLength = value.toIntOrNull() ?: -1
                    "transfer-encoding" -> chunked = value.lowercase().contains("chunked")
                }
            }
            val body = when {
                chunked -> readChunkedBody(reader)
                contentLength >= 0 -> readFixedBody(reader, contentLength)
                else -> reader.readText()
            }
            return Response(code, body.trim())
        } finally {
            try {
                ssl.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun readFixedBody(reader: BufferedReader, length: Int): String {
        if (length == 0) return ""
        val buf = CharArray(length)
        var off = 0
        while (off < length) {
            val n = reader.read(buf, off, length - off)
            if (n < 0) break
            off += n
        }
        return String(buf, 0, off)
    }

    /** Decode HTTP/1.1 chunked body (Google token endpoint often uses this). */
    private fun readChunkedBody(reader: BufferedReader): String {
        val out = StringBuilder()
        while (true) {
            val sizeLine = reader.readLine() ?: break
            val hex = sizeLine.substringBefore(';').trim()
            val size = hex.toIntOrNull(16) ?: break
            if (size == 0) {
                // Trailer headers
                while (true) {
                    val trailer = reader.readLine() ?: break
                    if (trailer.isEmpty()) break
                }
                break
            }
            val chunk = readFixedBody(reader, size)
            out.append(chunk)
            // Consume chunk CRLF
            reader.readLine()
        }
        return out.toString()
    }
}

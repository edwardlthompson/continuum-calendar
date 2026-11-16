package org.continuumcalendar.data.google

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
data class LoopbackRedirect(
    val code: String,
    val state: String,
    val redirectUri: String,
)

/**
 * One-shot 127.0.0.1 loopback for Desktop OAuth (parity with Tauri oauth.rs / mobile ContinuumLoopbackOAuth).
 */
object GoogleLoopbackOAuth {
    private const val TIMEOUT_MS = 300_000

    fun openServer(): Pair<ServerSocket, String> {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        server.soTimeout = TIMEOUT_MS
        val redirectUri = "http://127.0.0.1:${server.localPort}"
        return server to redirectUri
    }

    /** Block until a successful code redirect (skips favicon). */
    fun awaitCode(server: ServerSocket, redirectUri: String): LoopbackRedirect {
        try {
            while (true) {
                val socket = server.accept()
                val result = handleClient(socket, redirectUri)
                if (result != null) return result
            }
        } finally {
            runCatching { server.close() }
        }
    }

    internal fun parseRequestPath(requestLine: String): String {
        return requestLine.split(' ').getOrNull(1) ?: "/"
    }

    internal fun parseQuery(pathAndQuery: String): Map<String, String> {
        val q = pathAndQuery.substringAfter('?', missingDelimiterValue = "")
        if (q.isEmpty()) return emptyMap()
        return q.split('&').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i < 0) return@mapNotNull null
            val k = decode(part.substring(0, i))
            val v = decode(part.substring(i + 1))
            k to v
        }.toMap()
    }

    private fun handleClient(socket: Socket, redirectUri: String): LoopbackRedirect? {
        socket.soTimeout = 15_000
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            val requestLine = reader.readLine() ?: return null
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
            }
            val path = parseRequestPath(requestLine)
            if (path.startsWith("/favicon")) {
                writeRaw(s, "HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n")
                return null
            }
            val query = parseQuery(path)
            val error = query["error"]
            if (!error.isNullOrBlank()) {
                val desc = query["error_description"] ?: error
                writeHtml(
                    s,
                    "<html><body><h2>Sign-in failed</h2><p>${htmlEscape(desc)}</p>" +
                        "<p>You can close this tab and return to Continuum.</p></body></html>",
                )
                error("OAuth error: $desc")
            }
            val code = query["code"] ?: run {
                writeHtml(s, "<html><body><p>Missing code</p></body></html>")
                return null
            }
            val state = query["state"] ?: run {
                writeHtml(s, "<html><body><p>Missing state</p></body></html>")
                return null
            }
            writeHtml(
                s,
                "<html><body><h2>Continuum signed in</h2>" +
                    "<p>You can close this tab and return to Continuum Calendar.</p></body></html>",
            )
            return LoopbackRedirect(code = code, state = state, redirectUri = redirectUri)
        }
    }

    private fun writeHtml(socket: Socket, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        PrintWriter(socket.getOutputStream(), false).use { out ->
            out.print("HTTP/1.1 200 OK\r\n")
            out.print("Content-Type: text/html; charset=utf-8\r\n")
            out.print("Content-Length: ${bytes.size}\r\n")
            out.print("Connection: close\r\n\r\n")
            out.flush()
        }
        socket.getOutputStream().write(bytes)
        socket.getOutputStream().flush()
    }

    private fun writeRaw(socket: Socket, raw: String) {
        socket.getOutputStream().write(raw.toByteArray(Charsets.US_ASCII))
        socket.getOutputStream().flush()
    }

    private fun htmlEscape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun decode(s: String): String =
        java.net.URLDecoder.decode(s, Charsets.UTF_8)
}

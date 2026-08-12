package org.fossify.calendar.continuum

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.fossify.calendar.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Desktop-client OAuth via loopback (http://127.0.0.1:port/), matching Continuum desktop.
 * Google blocks custom URI schemes on Android; loopback is still valid for Desktop OAuth clients.
 */
object ContinuumLoopbackOAuth {
    private const val TIMEOUT_MS = 180_000

    data class Result(val code: String, val state: String, val redirectUri: String)

    /**
     * Starts a one-shot loopback server, opens the system browser to Google, waits for redirect.
     * Must be called off the main thread.
     */
    fun authorize(context: Context): Result {
        val clientId = BuildConfig.CONTINUUM_GOOGLE_CLIENT_ID.trim()
        if (clientId.isBlank()) error("Missing Continuum Google Client ID")

        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        server.soTimeout = TIMEOUT_MS
        val port = server.localPort
        val redirectUri = "http://127.0.0.1:$port/"
        val auth = ContinuumGoogleAuth(context)
        val authUri = auth.buildAuthorizationUri(clientId, redirectUri)

        val launched = AtomicBoolean(false)
        try {
            // Open browser after the server is listening.
            val open = Intent(Intent.ACTION_VIEW, authUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(open)
            launched.set(true)

            val socket = server.accept()
            return handleClient(socket, redirectUri)
        } finally {
            try {
                server.close()
            } catch (_: Exception) {
            }
            if (!launched.get()) {
                ContinuumDiagnostics.w("Loopback OAuth browser was not launched")
            }
        }
    }

    private fun handleClient(socket: Socket, redirectUri: String): Result {
        socket.soTimeout = 15_000
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val requestLine = reader.readLine() ?: error("Empty OAuth loopback request")
            // GET /?code=...&state=... HTTP/1.1
            val path = requestLine.split(' ').getOrNull(1) ?: "/"
            // Drain headers
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
            }
            val uri = Uri.parse("http://127.0.0.1$path")
            val error = uri.getQueryParameter("error")
            if (!error.isNullOrBlank()) {
                val desc = uri.getQueryParameter("error_description") ?: error
                writeHtml(
                    s,
                    "<html><body><h2>Sign-in failed</h2><p>${htmlEscape(desc)}</p>" +
                        "<p>You can close this tab and return to Continuum.</p></body></html>",
                )
                error("OAuth error: $desc")
            }
            val code = uri.getQueryParameter("code") ?: error("OAuth redirect missing code")
            val state = uri.getQueryParameter("state") ?: error("OAuth redirect missing state")
            writeHtml(
                s,
                "<html><body><h2>Continuum signed in</h2>" +
                    "<p>You can close this tab and return to Continuum Calendar.</p></body></html>",
            )
            return Result(code = code, state = state, redirectUri = redirectUri)
        }
    }

    private fun writeHtml(socket: Socket, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        PrintWriter(socket.getOutputStream(), false).use { out ->
            out.print("HTTP/1.1 200 OK\r\n")
            out.print("Content-Type: text/html; charset=utf-8\r\n")
            out.print("Content-Length: ${bytes.size}\r\n")
            out.print("Connection: close\r\n")
            out.print("\r\n")
            out.flush()
        }
        socket.getOutputStream().write(bytes)
        socket.getOutputStream().flush()
    }

    private fun htmlEscape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

package org.continuumcalendar.data.google

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

data class DriveAppDataFile(
    val id: String,
    val body: String,
    val etag: String?,
)

/** Google Drive App Data folder read/write for Continuum peer envelopes. */
object GoogleDriveAppData {
    private const val DRIVE = "https://www.googleapis.com/drive/v3"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    fun findAndDownload(accessToken: String, fileName: String): DriveAppDataFile? {
        val id = findFileId(accessToken, fileName) ?: return null
        return download(accessToken, id)
    }

    fun findFileId(accessToken: String, fileName: String): String? {
        val q = enc("name = '$fileName' and trashed = false")
        val url = "$DRIVE/files?spaces=appDataFolder&q=$q&fields=files(id)&pageSize=10"
        val body = get(accessToken, url)
        val m = Regex(""""id"\s*:\s*"([^"]+)"""").find(body) ?: return null
        return m.groupValues[1]
    }

    fun download(accessToken: String, fileId: String): DriveAppDataFile {
        val req = HttpRequest.newBuilder()
            .uri(URI.create("$DRIVE/files/${enc(fileId)}?alt=media"))
            .timeout(Duration.ofSeconds(45))
            .header("Authorization", "Bearer $accessToken")
            .GET()
            .build()
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() !in 200..299) error("Drive download HTTP ${res.statusCode()}")
        return DriveAppDataFile(fileId, res.body(), res.headers().firstValue("ETag").orElse(null))
    }

    fun upload(
        accessToken: String,
        fileName: String,
        body: String,
        existingId: String?,
        etag: String?,
    ): DriveAppDataFile {
        if (existingId.isNullOrBlank()) {
            val boundary = "continuum"
            val meta = """{"name":"$fileName","parents":["appDataFolder"]}"""
            val payload = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(meta)
                append("\r\n--$boundary\r\n")
                append("Content-Type: application/json\r\n\r\n")
                append(body)
                append("\r\n--$boundary--")
            }
            val req = HttpRequest.newBuilder()
                .uri(URI.create("$UPLOAD/files?uploadType=multipart&spaces=appDataFolder"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "multipart/related; boundary=$boundary")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build()
            val res = http.send(req, HttpResponse.BodyHandlers.ofString())
            if (res.statusCode() !in 200..299) error("Drive create HTTP ${res.statusCode()}")
            val id = Regex(""""id"\s*:\s*"([^"]+)"""").find(res.body())?.groupValues?.get(1)
                ?: error("Drive create missing id")
            return DriveAppDataFile(id, body, res.headers().firstValue("ETag").orElse(null))
        }
        val builder = HttpRequest.newBuilder()
            .uri(URI.create("$UPLOAD/files/${enc(existingId)}?uploadType=media"))
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
        if (!etag.isNullOrBlank()) builder.header("If-Match", etag)
        val res = http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() == 412) error("CAS_CONFLICT")
        if (res.statusCode() !in 200..299) error("Drive update HTTP ${res.statusCode()}")
        return DriveAppDataFile(existingId, body, res.headers().firstValue("ETag").orElse(null))
    }

    private fun get(accessToken: String, url: String): String {
        val req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer $accessToken")
            .GET()
            .build()
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() !in 200..299) error("Drive list HTTP ${res.statusCode()}")
        return res.body()
    }

    private fun enc(s: String): String = URLEncoder.encode(s, StandardCharsets.UTF_8)
}

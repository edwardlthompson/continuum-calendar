package org.continuumcalendar.data.google

import org.continuumcalendar.domain.model.GoogleOAuthTokens

/** Minimal JSON parse/serialize for Tauri vault shape (camelCase). */
fun parseGoogleTokensJson(raw: String): GoogleOAuthTokens? {
    val access = stringField(raw, "accessToken") ?: return null
    if (access.isBlank()) return null
    val expiresAt = longField(raw, "expiresAt") ?: return null
    return GoogleOAuthTokens(
        accessToken = access,
        refreshToken = stringField(raw, "refreshToken"),
        expiresAt = expiresAt,
        scope = stringField(raw, "scope").orEmpty(),
        tokenType = stringField(raw, "tokenType"),
        clientId = stringField(raw, "clientId"),
    )
}

fun googleTokensToJson(tokens: GoogleOAuthTokens): String {
    val parts = mutableListOf(
        """"accessToken":${jsonString(tokens.accessToken)}""",
        """"expiresAt":${tokens.expiresAt}""",
        """"scope":${jsonString(tokens.scope)}""",
    )
    tokens.refreshToken?.let { parts += """"refreshToken":${jsonString(it)}""" }
    tokens.tokenType?.let { parts += """"tokenType":${jsonString(it)}""" }
    tokens.clientId?.let { parts += """"clientId":${jsonString(it)}""" }
    return "{${parts.joinToString(",")}}"
}

private fun jsonString(s: String): String =
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun stringField(json: String, key: String): String? {
    val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
    val m = re.find(json) ?: return null
    return m.groupValues[1]
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
}

private fun longField(json: String, key: String): Long? {
    val re = Regex(""""$key"\s*:\s*(\d+)""")
    return re.find(json)?.groupValues?.get(1)?.toLongOrNull()
}

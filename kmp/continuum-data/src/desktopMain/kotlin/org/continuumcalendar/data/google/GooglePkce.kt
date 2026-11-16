package org.continuumcalendar.data.google

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class PkceSession(
    val verifier: String,
    val challenge: String,
    val state: String,
)

object GooglePkce {
    fun newSession(): PkceSession {
        val verifier = randomUrlSafe(32)
        return PkceSession(
            verifier = verifier,
            challenge = sha256UrlSafe(verifier),
            state = randomUrlSafe(16),
        )
    }

    fun sha256UrlSafe(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun randomUrlSafe(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

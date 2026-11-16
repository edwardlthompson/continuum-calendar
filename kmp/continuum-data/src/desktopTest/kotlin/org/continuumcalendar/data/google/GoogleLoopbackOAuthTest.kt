package org.continuumcalendar.data.google

import kotlin.test.Test
import kotlin.test.assertEquals

class GoogleLoopbackOAuthTest {
    @Test
    fun parsesCodeAndStateFromPath() {
        val path = GoogleLoopbackOAuth.parseRequestPath(
            "GET /?code=abc%2Fdef&state=xyz HTTP/1.1",
        )
        assertEquals("/?code=abc%2Fdef&state=xyz", path)
        val q = GoogleLoopbackOAuth.parseQuery(path)
        assertEquals("abc/def", q["code"])
        assertEquals("xyz", q["state"])
    }
}

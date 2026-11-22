package com.movtery.zalithlauncher.gdworld

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class GdSessionRestoreTest {
    private val id = "12345678-1234-1234-1234-123456789abc"
    private fun session() = JSONObject().put("playerProfile", JSONObject().put("uuid", id))
        .put("oauth", JSONObject().put("accessToken", "valid-access").put("refreshToken", "refresh"))
    private fun restored(uuid: String = id) = JSONObject().put("userInfo", JSONObject()
        .put("playerProfile", JSONObject().put("uuid", uuid)).put("accessToken", "minecraft-token"))

    @Test fun repeatedRestoresDoNotAuthenticateOrRotateValidTokens() {
        var saved = session()
        val calls = mutableListOf<String>()
        repeat(6) {
            saved = restoreGdSession(saved, { type, body ->
                calls += type
                assertEquals("valid-access", body.getString("accessToken"))
                assertTrue(body.getBoolean("needUserInfo"))
                restored()
            }, { saved = it })
        }
        assertEquals(List(6) { "restore" }, calls)
        assertEquals("minecraft-token", saved.getString("accessToken"))
    }

    @Test fun expiredAccessRefreshesOnceBeforeRestoringTheSameIdentity() {
        val calls = mutableListOf<String>()
        var result: JSONObject? = null
        restoreGdSession(session(), { type, body ->
            calls += type
            when (calls.size) {
                1 -> throw IOException("auth.expiretoken")
                2 -> {
                    assertEquals("refresh", body.getString("refreshToken"))
                    JSONObject().put("oauth", JSONObject().put("accessToken", "new-access").put("refreshToken", "rotated"))
                }
                else -> { assertEquals("new-access", body.getString("accessToken")); restored() }
            }
        }, { result = it })
        assertEquals(listOf("restore", "refreshToken", "restore"), calls)
        assertEquals("rotated", result!!.getJSONObject("oauth").getString("refreshToken"))
    }

    @Test fun invalidCredentialsAndNetworkErrorsAreNotRetriedOrRefreshed() {
        for (error in listOf("auth.invalidtoken", "connection", "auth.trylater")) {
            var calls = 0
            try {
                restoreGdSession(session(), { _, _ -> calls++; throw IOException(error) }, { fail("Unexpected save") })
                fail("Expected error")
            } catch (e: IOException) { assertEquals(error, e.message) }
            assertEquals(1, calls)
        }
    }

    @Test fun aDifferentIdentityCannotReplaceTheStoredPlayer() {
        try {
            restoreGdSession(session(), { _, _ -> restored("00000000-0000-0000-0000-000000000000") }, { fail("Wrong player saved") })
            fail("Expected identity mismatch")
        } catch (_: IllegalStateException) { }
    }

    @Test fun rotatedRefreshSurvivesFailureOfTheFollowingRestore() {
        var calls = 0
        var saved: JSONObject? = null
        try {
            restoreGdSession(session(), { _, _ ->
                when (++calls) {
                    1 -> throw IOException("auth.expiretoken")
                    2 -> JSONObject().put("oauth", JSONObject().put("accessToken", "new-access").put("refreshToken", "rotated"))
                    else -> throw IOException("connection")
                }
            }, { saved = it })
            fail("Expected network failure")
        } catch (_: IOException) { }
        assertEquals("rotated", saved!!.getJSONObject("oauth").getString("refreshToken"))
    }
}

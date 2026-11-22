package com.movtery.zalithlauncher.gdworld

import org.json.JSONObject
import java.io.IOException

/** Restore a verified launcher connection without another password-authentication attempt. */
internal fun restoreGdSession(
    session: JSONObject,
    request: (String, JSONObject) -> JSONObject,
    save: (JSONObject) -> Unit
): JSONObject {
    fun restore(): JSONObject = request("restore", JSONObject()
        .put("authId", "std")
        .put("accessToken", session.getJSONObject("oauth").getString("accessToken"))
        .put("needUserInfo", true))
    val response = try { restore() } catch (e: IOException) {
        if (e.message != "auth.expiretoken") throw e
        val refreshed = request("refreshToken", JSONObject().put("authId", "std")
            .put("refreshToken", session.getJSONObject("oauth").getString("refreshToken")))
        session.put("oauth", refreshed.getJSONObject("oauth"))
        // A rotated refresh token must survive even if the following network call fails.
        save(session)
        restore()
    }
    val info = response.getJSONObject("userInfo")
    val expected = session.getJSONObject("playerProfile").getString("uuid").replace("-", "")
    check(info.getJSONObject("playerProfile").getString("uuid").replace("-", "").equals(expected, true)) { "login" }
    check(info.getString("accessToken").isNotBlank()) { "login" }
    return JSONObject().put("playerProfile", info.getJSONObject("playerProfile"))
        .put("accessToken", info.getString("accessToken"))
        .put("oauth", session.getJSONObject("oauth")).also(save)
}

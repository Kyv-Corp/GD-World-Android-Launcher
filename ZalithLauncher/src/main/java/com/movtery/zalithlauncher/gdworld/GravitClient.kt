package com.movtery.zalithlauncher.gdworld

import okhttp3.*
import org.json.JSONObject
import java.io.Closeable
import java.io.IOException
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** A connection belongs to one operation; credentials never enter OkHttp logging. */
class GravitClient : Closeable {
    private val messages = LinkedBlockingQueue<JSONObject>()
    private val socket = http.newWebSocket(Request.Builder().url("wss://gd-network.ddns.net/api").build(), object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            try { messages.offer(JSONObject(text)) } catch (_: Exception) {
                messages.offer(JSONObject().put("error", "protocol"))
            }
        }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            messages.offer(JSONObject().put("error", "connection"))
        }
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            messages.offer(JSONObject().put("error", "connection"))
        }
    })

    @Synchronized fun request(type: String, body: JSONObject = JSONObject()): JSONObject {
        val id = UUID.randomUUID().toString()
        check(socket.send(body.put("type", type).put("requestUUID", id).toString())) { "connection" }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45)
        while (true) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) throw IOException("timeout")
            val result = messages.poll(remaining, TimeUnit.NANOSECONDS) ?: throw IOException("timeout")
            if (result.optString("requestUUID") == id || !result.has("requestUUID")) {
                if (result.has("error") && !result.isNull("error")) {
                    val code = result.optString("error").takeIf { it.matches(Regex("[a-zA-Z0-9_.-]{1,80}")) } ?: "server"
                    throw IOException(if (code == "auth.trylater") com.movtery.zalithlauncher.context.GlobalContext.getString(com.movtery.zalithlauncher.R.string.gd_auth_wait) else code)
                }
                if (result.optString("type") == type) return result
            }
        }
    }

    private fun registerBuild() {
        request("gdworldAndroid", JSONObject().put("sha256", apkDigest))
    }

    fun login(login: String, password: String, totp: String): JSONObject {
        registerBuild()
        val first = JSONObject().put("type", "plain").put("password", password)
        val credential = if (totp.isBlank()) first else JSONObject().put("type", "2fa")
            .put("firstPassword", first).put("secondPassword", JSONObject().put("type", "totp").put("totp", totp))
        return request("auth", JSONObject().put("login", login).put("password", credential)
            .put("auth_id", "std").put("authType", "CLIENT").put("getSession", false))
    }

    fun restore(): JSONObject {
        registerBuild()
        return GdSession.restore(::request)
    }

    override fun close() { socket.close(1000, null) }
    companion object {
        private val http = OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS).build()
        private val apkDigest: String by lazy {
            val sha = java.security.MessageDigest.getInstance("SHA-256")
            java.io.File(com.movtery.zalithlauncher.context.GlobalContext.applicationInfo.sourceDir).inputStream().use { input ->
                val buffer = ByteArray(65536)
                while (true) { val count = input.read(buffer); if (count < 0) break; sha.update(buffer, 0, count) }
            }
            sha.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

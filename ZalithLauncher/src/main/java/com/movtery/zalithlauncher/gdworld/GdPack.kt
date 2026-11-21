package com.movtery.zalithlauncher.gdworld

import android.content.Context
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.movtery.zalithlauncher.game.path.getVersionsHome
import com.movtery.zalithlauncher.game.version.installed.VersionConfig
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GdPack {
    private val http = OkHttpClient()
    suspend fun prepare(context: Context, progress: (String) -> Unit): Pair<String, String> = withContext(Dispatchers.IO) {
        GravitClient().use { client ->
            val session = client.restore()
            val account = checkNotNull(com.movtery.zalithlauncher.game.account.AccountsManager.currentAccountFlow.value)
            val player = session.getJSONObject("playerProfile")
            check(account.accountType == "GDWorld" && account.profileId == player.getString("uuid")) { "login" }
            account.username = player.getString("username")
            com.movtery.zalithlauncher.game.account.AccountsManager.suspendSaveAccount(account)
            val profiles = client.request("profiles").getJSONArray("profiles")
            val profile = (0 until profiles.length()).map { profiles.getJSONObject(it) }.single { it.getString("title") == "GDWorldTest" }
            client.request("setProfile", JSONObject().put("uuid", profile.getString("uuid")))
            val mc = profile.getString("version")
            val args = profile.getJSONArray("clientArgs")
            val loaderIndex = (0 until args.length()).firstOrNull { args.optString(it) == "--fml.neoForgeVersion" }
                ?: error("loader")
            val loader = args.getString(loaderIndex + 1)
            require(mc.matches(Regex("[a-zA-Z0-9._-]+")) && loader.matches(Regex("[a-zA-Z0-9._-]+")))
            val versionName = "GDWorld-$mc-$loader"
            val versionDir = PackFiles.resolve(File(getVersionsHome()), versionName)
            val installed = PackFiles.resolve(versionDir, ".gdworld-installed")
            if (!installed.exists()) {
                progress("install")
                val installing = File(versionDir.parentFile, ".$versionName.gdworld-installing")
                check(!versionDir.exists() || installing.exists()) { "directory" }
                installing.parentFile!!.mkdirs()
                installing.writeText("GDWorld installation in progress")
                coroutineScope {
                    val installer = GameInstaller(context, GameDownloadInfo(mc, versionName, overwrite = versionDir.exists(), neoforge = NeoForgeVersion(loader, false)), this)
                    suspendCancellableCoroutine { continuation ->
                        installer.installGame(
                            onInstalled = { if (continuation.isActive) continuation.resume(Unit) },
                            onError = { if (continuation.isActive) continuation.resumeWithException(it) },
                            onGameAlreadyInstalled = { if (continuation.isActive) continuation.resumeWithException(IllegalStateException("install")) }
                        )
                    }
                }
                VersionConfig.createIsolation(versionDir).apply { renderer = "a1b2c3d4-e5f6-7890-abcd-ef1234567890" }.saveWithThrowable()
                installed.writeText("$mc/$loader")
                check(installing.delete())
            }
            progress("update")
            sync(client.request("update", JSONObject().put("dirName", profile.getString("dir"))), versionDir, progress)
            val servers = profile.getJSONArray("servers")
            val server = (0 until servers.length()).map { servers.getJSONObject(it) }.firstOrNull { it.optBoolean("isDefault") } ?: servers.getJSONObject(0)
            val address = server.getString("serverAddress") + ":" + server.getInt("serverPort")
            VersionsManager.refresh("GDWorld", trySetVersion = versionName)
            versionName to address
        }
    }

    private suspend fun sync(update: JSONObject, root: File, progress: (String) -> Unit) {
        require(!update.optBoolean("zip")) { "archive" }
        val files = linkedMapOf<String, JSONObject>()
        fun walk(dir: JSONObject, prefix: String = "") {
            val entries = dir.getJSONObject("map")
            for (name in entries.keys()) {
                val path = prefix + name
                PackFiles.resolve(root, path)
                val entry = entries.getJSONObject(name)
                when (entry.getString("type")) {
                    "dir" -> walk(entry, "$path/")
                    "file" -> if (PackFiles.isContent(path)) files[path] = entry
                    else -> error("manifest")
                }
            }
        }
        walk(update.getJSONObject("hdir"))
        val base = update.getString("url").toHttpUrl()
        require(base.isHttps && base.host == "gd-network.ddns.net") { "url" }
        val stage = PackFiles.resolve(root, ".gdworld-stage").apply { PackFiles.deleteTree(this); check(mkdirs()) }
        val manifest = PackFiles.resolve(root, ".gdworld-managed.json")
        val old = if (manifest.exists()) JSONObject(manifest.readText()) else JSONObject()
        for ((index, item) in files.entries.withIndex()) {
            currentCoroutineContext().ensureActive()
            val (path, entry) = item
            progress("${index + 1}/${files.size}")
            val digest = PackFiles.decodeDigest(entry.getString("digest"))
            val size = entry.getLong("size")
            require(digest.size == 20 && size >= 0)
            val target = PackFiles.resolve(root, path)
            if (matches(target, size, digest)) continue
            val url = entry.optString("url").takeIf { it.isNotBlank() }?.toHttpUrl() ?: base.newBuilder().addPathSegments(path).build()
            require(url.isHttps && url.host == base.host) { "url" }
            val pending = PackFiles.resolve(stage, path).apply { parentFile!!.mkdirs() }
            http.newCall(Request.Builder().url(url).build()).execute().use { response ->
                check(response.isSuccessful) { "download" }
                response.body.byteStream().use { source -> pending.outputStream().use { output ->
                    val buffer = ByteArray(65536)
                    var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= size) { "checksum" }
                        output.write(buffer, 0, count)
                    }
                } }
            }
            check(matches(pending, size, digest)) { "checksum" }
        }
        currentCoroutineContext().ensureActive()
        // A failed commit leaves the old ownership manifest and blocks launch until a successful retry.
        withContext(NonCancellable) {
            for (path in files.keys) {
                val pending = PackFiles.resolve(stage, path)
                if (pending.exists()) {
                    val target = PackFiles.resolve(root, path).apply { parentFile!!.mkdirs() }
                    check(pending.renameTo(target)) { "storage" }
                }
            }
            for (path in old.keys()) if (path !in files) {
                require(PackFiles.isContent(path))
                val stale = PackFiles.resolve(root, path)
                check(!stale.exists() || stale.delete()) { "storage" }
            }
            val next = PackFiles.resolve(root, ".gdworld-managed.tmp")
            next.writeText(JSONObject().apply { files.keys.forEach { put(it, true) } }.toString())
            check(next.renameTo(manifest)) { "storage" }
            PackFiles.deleteTree(stage)
        }
    }
    private fun matches(file: File, size: Long, digest: ByteArray): Boolean {
        if (!file.isFile || file.length() != size) return false
        val sha = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) { val n = input.read(buffer); if (n < 0) break; sha.update(buffer, 0, n) }
        }
        return sha.digest().contentEquals(digest)
    }
}

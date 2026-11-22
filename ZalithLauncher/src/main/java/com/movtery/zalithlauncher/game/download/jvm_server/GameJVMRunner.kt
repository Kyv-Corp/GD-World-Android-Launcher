/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.download.jvm_server

import com.movtery.zalithlauncher.components.jre.Jre
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.notification.NoticeProgress
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "GameJVMRunner"
private val installerJvm = Mutex()

/**
 * 运行一个简易的JVM环境，安装ModLoader，同时在jvm退出时，尝试使用其他的Java环境重试
 * @param logId 记录日志的 tag
 * @param start 刚开始启动会调用的回调
 */
suspend fun runJvmRetryRuntimes(
    logId: String,
    jvmArgs: String,
    prefixArgs: (Jre) -> String?,
    jre: Jre,
    userHome: String,
    postSummary: String? = null,
    postProgress: NoticeProgress? = null,
    start: (Jre) -> Unit = {}
): Unit = withContext(Dispatchers.Default) {
    start(jre)

    val finalArgs = prefixArgs(jre)?.let {
        "$it $jvmArgs"
    } ?: jvmArgs

    val exitCode = startJvmServiceAndWaitExit(
        jvmArgs = finalArgs,
        jreName = jre.jreName,
        userHome = userHome,
        postSummary = postSummary,
        postProgress = postProgress
    )

    if (exitCode != 0) {
        val nextJava: Jre? = when (jre) {
            Jre.JRE_8 -> Jre.JRE_17
            Jre.JRE_17 -> Jre.JRE_21
            Jre.JRE_21 -> Jre.JRE_25
            else -> null
        }

        nextJava?.let { jre ->
            Logger.info(TAG, "Retry with jre ${jre.name}...")
            runJvmRetryRuntimes(
                logId = logId,
                jvmArgs = jvmArgs,
                prefixArgs = prefixArgs,
                jre = jre,
                userHome = userHome,
                postSummary = postSummary,
                postProgress = postProgress,
                start = start
            )
        } ?: throw JvmCrashException(exitCode)
    }
}

suspend fun startJvmServiceAndWaitExit(
    jvmArgs: String,
    jreName: String? = null,
    userHome: String? = null,
    postSummary: String? = null,
    postProgress: NoticeProgress? = null,
): Int = installerJvm.withLock {
    try {
        withContext(Dispatchers.IO) {
            stopJvmProcess(GlobalContext)
            withTimeout(10_000) {
                while (isJvmProcessRunning(GlobalContext)) delay(100)
            }
            JvmExitSignal(PROCESS_SERVICE_PORT).use { signal ->
                try {
                    startJvmService(
                        context = GlobalContext,
                        jvmArgs = jvmArgs,
                        userHome = userHome,
                        jreName = jreName,
                        postSummary = postSummary,
                        postProgress = postProgress
                    )
                    val started = System.nanoTime()
                    var seenProcess = false
                    var code: Int? = null
                    withTimeout(20 * 60_000L) {
                        while (code == null) {
                            currentCoroutineContext().ensureActive()
                            code = signal.receive()
                            if (code == null) {
                                val alive = isJvmProcessRunning(GlobalContext)
                                seenProcess = seenProcess || alive
                                if (!alive && (seenProcess || System.nanoTime() - started > 30_000_000_000L)) {
                                    code = -1
                                }
                            }
                        }
                    }
                    checkNotNull(code).also { Logger.info(TAG, "Installer exit code: $it") }
                } finally {
                    stopJvmProcess(GlobalContext)
                }
            }
        }
    } catch (e: TimeoutCancellationException) {
        throw JvmCrashException(-1, "Installer JVM timed out; see latest_process.log", e)
    }
}

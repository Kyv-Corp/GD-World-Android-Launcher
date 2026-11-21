package com.movtery.zalithlauncher.gdworld

import com.movtery.zalithlauncher.R

data class GdProgress(
    val phase: Phase,
    val fraction: Float? = null,
    val detail: String = "",
    val completedBytes: Long? = null,
    val totalBytes: Long? = null
) {
    enum class Phase { CONNECT, INSTALL, CHECK, DOWNLOAD, APPLY }

    val textRes: Int get() = when (phase) {
        Phase.CONNECT -> R.string.gd_connect
        Phase.INSTALL -> R.string.gd_install
        Phase.CHECK -> R.string.gd_check
        Phase.DOWNLOAD -> R.string.gd_update
        Phase.APPLY -> R.string.gd_apply
    }
}

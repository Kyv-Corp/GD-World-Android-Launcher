package com.movtery.zalithlauncher.gdworld

import com.movtery.zalithlauncher.upgrade.GithubReleaseApi
import org.junit.Assert.*
import org.junit.Test

class LauncherReleaseTest {
    private fun release(tag: String, draft: Boolean = false, asset: String = "GDWorld-Android.apk", size: Long = 100) =
        GithubReleaseApi(tag, tag, "", "2026-11-21T00:00:00Z",
            listOf(GithubReleaseApi.Asset(asset, "https://github.com/fixture/$asset", size)), draft)

    @Test fun selectsHighestPublishedBuildWithAnApk() {
        val current = release("v000002")
        assertEquals(current, GithubReleaseApi.newestLauncherRelease(listOf(
            release("v000003", draft = true), release("v000004", asset = "SHA256SUMS"),
            current, release("v000001"), release("v000005", size = 0), release("v9.0.0")
        )))
        assertNull(GithubReleaseApi.newestLauncherRelease(listOf(release("0.1.0"))))
    }

    @Test fun parsesOnlySixDigitVersionCodes() {
        assertEquals(123, release("v000123").versionCode)
        for (tag in listOf("v1.2.3", "v1234567", "v000123-extra", "release000123")) {
            assertNull(tag, release(tag).versionCode)
        }
    }
}

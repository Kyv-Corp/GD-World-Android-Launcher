package com.movtery.zalithlauncher.gdworld

import com.movtery.zalithlauncher.components.jre.Jre
import com.movtery.zalithlauncher.game.download.game.forge.installerJavaRuntime
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallerRuntimeTest {
    @Test fun minecraft1211StartsItsProcessorsOnJava21() {
        assertEquals(Jre.JRE_21, installerJavaRuntime("""{"javaVersion":{"component":"java-runtime-delta","majorVersion":21}}"""))
    }

    @Test fun olderAndNewerMetadataSelectCompatibleBundledRuntimes() {
        assertEquals(Jre.JRE_8, installerJavaRuntime("{}"))
        assertEquals(Jre.JRE_17, installerJavaRuntime("""{"javaVersion":{"majorVersion":16}}"""))
        assertEquals(Jre.JRE_25, installerJavaRuntime("""{"javaVersion":{"majorVersion":25}}"""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidMetadataIsNotSilentlyDowngradedToJava8() {
        installerJavaRuntime("""{"javaVersion":{"majorVersion":0}}""")
    }

    @Test(expected = IllegalStateException::class)
    fun unsupportedJavaIsReportedInsteadOfLaunchingAnOlderRuntime() {
        installerJavaRuntime("""{"javaVersion":{"majorVersion":30}}""")
    }
}

package com.movtery.zalithlauncher.gdworld;

import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class PackFilesTest {
    @Test public void downloadProgressCountsActualBytesAndVerifiesContent() throws Exception {
        byte[] content = new byte[150000];
        new java.util.Random(42).nextBytes(content);
        byte[] digest = java.security.MessageDigest.getInstance("SHA-1").digest(content);
        var reports = new java.util.ArrayList<Long>();
        var output = new java.io.ByteArrayOutputStream();
        PackFiles.copyVerified(new java.io.ByteArrayInputStream(content), output, content.length, digest, reports::add);
        assertArrayEquals(content, output.toByteArray());
        assertEquals(java.util.List.of(0L, 65536L, 131072L, 150000L), reports);
        for (int size : new int[]{content.length - 1, content.length + 1}) {
            try {
                PackFiles.copyVerified(new java.io.ByteArrayInputStream(content), new java.io.ByteArrayOutputStream(), size, digest, n -> {});
                fail("incorrect size");
            } catch (IOException expected) { }
        }
        digest[0] ^= 1;
        try {
            PackFiles.copyVerified(new java.io.ByteArrayInputStream(content), new java.io.ByteArrayOutputStream(), content.length, digest, n -> {});
            fail("incorrect checksum");
        } catch (IOException expected) { }
    }
    @Test public void downloadCanBeCancelledBetweenChunksAndHandlesEmptyFiles() throws Exception {
        byte[] content = new byte[150000];
        byte[] digest = java.security.MessageDigest.getInstance("SHA-1").digest(content);
        var output = new java.io.ByteArrayOutputStream();
        try {
            PackFiles.copyVerified(new java.io.ByteArrayInputStream(content), output, content.length, digest, n -> {
                if (n >= 65536) throw new java.util.concurrent.CancellationException();
            });
            fail("cancelled");
        } catch (java.util.concurrent.CancellationException expected) {
            assertEquals(65536, output.size());
        }
        var reports = new java.util.ArrayList<Long>();
        PackFiles.copyVerified(new java.io.ByteArrayInputStream(new byte[0]), new java.io.ByteArrayOutputStream(), 0,
            java.security.MessageDigest.getInstance("SHA-1").digest(new byte[0]), reports::add);
        assertEquals(java.util.List.of(0L), reports);
    }
    @Test public void readsGravitUrlSafeSha1IncludingUnderscores() throws Exception {
        byte[] emptySha1 = java.security.MessageDigest.getInstance("SHA-1").digest(new byte[0]);
        assertArrayEquals(emptySha1, PackFiles.decodeDigest("2jmj7l5rSw0yVb_vlWAYkK_YBwk="));
        assertArrayEquals(emptySha1, PackFiles.decodeDigest("2jmj7l5rSw0yVb_vlWAYkK_YBwk"));
        try { PackFiles.decodeDigest("AA=="); fail("short digest"); } catch (IllegalArgumentException expected) { }
    }
    @Test public void rejectsEscapesAndSymlinks() throws Exception {
        File root = Files.createTempDirectory("gdworld-pack-test").toFile();
        try {
            for (String path : new String[]{"../world", "/tmp/escape", "mods/../../world", "mods\\escape", "mods//file", "mods/./file"}) {
                try { PackFiles.resolve(root, path); fail(path); } catch (IOException expected) { }
            }
            Files.createSymbolicLink(new File(root, "mods").toPath(), root.toPath().getParent());
            try { PackFiles.resolve(root, "mods/escape.jar"); fail("symlink"); } catch (IOException expected) { }
        } finally { Files.deleteIfExists(new File(root, "mods").toPath()); root.delete(); }
    }
    @Test public void stageCleanupDoesNotFollowLinksIntoUserData() throws Exception {
        File stage = Files.createTempDirectory("gdworld-stage").toFile();
        File player = Files.createTempDirectory("gdworld-player").toFile();
        File keep = new File(player, "level.dat");
        try {
            Files.writeString(keep.toPath(), "player world");
            Files.createSymbolicLink(new File(stage, "link").toPath(), player.toPath());
            PackFiles.deleteTree(stage);
            assertFalse(stage.exists());
            assertEquals("player world", Files.readString(keep.toPath()));
        } finally { PackFiles.deleteTree(stage); PackFiles.deleteTree(player); }
    }
    @Test public void acceptsChangingPackContentButPreservesPlatformAndPlayerData() throws Exception {
        assertTrue(PackFiles.isContent("mods/new-loader-compatible-mod.jar"));
        assertTrue(PackFiles.isContent("new-content-folder/custom.json"));
        assertTrue(PackFiles.isContent("config/new-mod/settings.toml"));
        for (String path : new String[]{"libraries/desktop.jar", "natives/lib.so", "saves/world/level.dat", "screenshots/photo.png", "options.txt", ".gdworld-managed.json"}) assertFalse(path, PackFiles.isContent(path));
    }
}

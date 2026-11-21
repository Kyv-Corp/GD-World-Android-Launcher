package com.movtery.zalithlauncher.gdworld;

import java.io.File;
import java.io.IOException;

public final class PackFiles {
    private PackFiles() {}
    public static File resolve(File root, String path) throws IOException {
        if (path.isEmpty() || path.contains("\\") || new File(path).isAbsolute()) throw new IOException("path");
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) throw new IOException("path");
        }
        File target = new File(root, path);
        if (!target.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)) throw new IOException("path");
        return target;
    }
    public static byte[] decodeDigest(String encoded) {
        byte[] digest = java.util.Base64.getUrlDecoder().decode(encoded);
        if (digest.length != 20) throw new IllegalArgumentException("checksum");
        return digest;
    }
    public static void deleteTree(File root) throws IOException {
        if (!java.nio.file.Files.exists(root.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) return;
        java.nio.file.Files.walkFileTree(root.toPath(), new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
            @Override public java.nio.file.FileVisitResult visitFile(java.nio.file.Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                java.nio.file.Files.delete(file);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
            @Override public java.nio.file.FileVisitResult postVisitDirectory(java.nio.file.Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                java.nio.file.Files.delete(dir);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }
    public static boolean isContent(String path) {
        String root = path.split("/", 2)[0];
        if (root.startsWith(".gdworld-")) return false;
        return !java.util.Arrays.asList("libraries", "natives", "assets", "runtime", "runtimes", "java", "versions",
            "saves", "screenshots", "logs", "crash-reports", "options.txt", "optionsof.txt", "usernamecache.json",
            "minecraft.jar", "forge.jar", "liteloader.jar").contains(root)
            && !path.endsWith(".exe") && !path.endsWith(".dll") && !path.endsWith(".so") && !path.endsWith(".dylib");
    }
}

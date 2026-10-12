package com.votexa.offline.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/** Replay protection: a package id can be loaded on this device only once. */
public final class PackageRegistry {
    private final Path file;

    public PackageRegistry(Path dir) throws IOException {
        Files.createDirectories(dir);
        this.file = dir.resolve("loaded-packages.txt");
        if (!Files.exists(file)) Files.createFile(file);
    }

    public boolean seen(String packageId) throws IOException {
        List<String> ids = Files.readAllLines(file);
        return ids.contains(packageId);
    }

    public void add(String packageId) throws IOException {
        Files.writeString(file, packageId + "\n", StandardOpenOption.APPEND);
    }
}

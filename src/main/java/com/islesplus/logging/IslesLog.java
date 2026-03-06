package com.islesplus.logging;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class IslesLog {
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Path LOG_DIR =
        FabricLoader.getInstance().getGameDir().resolve("islespluslogs");
    private static final Path RUNTIME_LOG = LOG_DIR.resolve("runtime.log");
    private static final Path DEBUG_LOG = LOG_DIR.resolve("debug.log");
    private static final long MAX_LOG_BYTES = 10L * 1024L * 1024L;
    private static final int MAX_BACKUPS = 3;

    private IslesLog() {
    }

    public static Path debugLogPath() {
        return DEBUG_LOG;
    }

    public static void runtimeInfo(String message) {
        append(RUNTIME_LOG, "INFO", message, null);
    }

    public static void runtimeWarn(String message) {
        append(RUNTIME_LOG, "WARN", message, null);
    }

    public static void runtimeWarn(String message, Throwable throwable) {
        append(RUNTIME_LOG, "WARN", message, throwable);
    }

    public static void runtimeError(String message, Throwable throwable) {
        append(RUNTIME_LOG, "ERROR", message, throwable);
    }

    public static void debug(String message) {
        append(DEBUG_LOG, "DEBUG", message, null);
    }

    private static synchronized void append(Path path, String level, String message, Throwable throwable) {
        StringBuilder line = new StringBuilder()
            .append('[').append(LocalDateTime.now().format(TIME_FORMAT)).append(']')
            .append(" [").append(level).append("] ")
            .append(message);
        if (throwable != null) {
            line.append(System.lineSeparator()).append(stackTrace(throwable));
        }

        try {
            Files.createDirectories(LOG_DIR);
            rotateIfNeeded(path);
            Files.writeString(path, line + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Logging failures are intentionally swallowed.
        }
    }

    private static void rotateIfNeeded(Path path) throws IOException {
        if (!Files.exists(path) || Files.size(path) < MAX_LOG_BYTES) {
            return;
        }

        for (int i = MAX_BACKUPS; i >= 1; i--) {
            Path from = (i == 1) ? path : backupPath(path, i - 1);
            Path to = backupPath(path, i);
            if (Files.exists(from)) {
                Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static Path backupPath(Path path, int index) {
        return path.resolveSibling(path.getFileName().toString() + "." + index);
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}

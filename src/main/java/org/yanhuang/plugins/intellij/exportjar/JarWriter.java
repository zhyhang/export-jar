package org.yanhuang.plugins.intellij.exportjar;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Writes collected files into a JAR.
 * <p>
 * Pure JAR-writing mechanics, decoupled from logging/notification: callers pass
 * an {@link EntryWrittenListener} to observe progress instead of this class
 * depending on the message UI.
 */
public class JarWriter {

    private static final String MANIFEST_VERSION = "2.5.0";

    /**
     * Notified after each entry has been written, for progress/logging.
     */
    @FunctionalInterface
    public interface EntryWrittenListener {
        void onEntryWritten(Path filePath, String entryName);
    }

    /**
     * Create a new JAR at the given path from parallel filePaths/entryNames lists.
     * A {@code null} file path means a directory entry (no content).
     *
     * @param jarFileFullPath target jar path
     * @param filePaths       source file paths (index-aligned with entryNames); null = directory entry
     * @param entryNames      jar entry names (index-aligned with filePaths)
     * @param listener        optional per-entry callback, may be null
     * @throws ExportJarException if writing fails
     */
    public void write(Path jarFileFullPath, List<Path> filePaths, List<String> entryNames,
                      EntryWrittenListener listener) {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION);
        // "Created-By" removed per https://github.com/zhyhang/export-jar/issues/15
        try (OutputStream os = Files.newOutputStream(jarFileFullPath);
             BufferedOutputStream bos = new BufferedOutputStream(os);
             JarOutputStream jos = new JarOutputStream(bos, manifest)) {
            for (int i = 0; i < entryNames.size(); i++) {
                final String entryName = entryNames.get(i);
                final JarEntry je = new JarEntry(entryName);
                final Path filePath = filePaths.get(i);
                jos.putNextEntry(je);
                if (filePath != null && Files.isRegularFile(filePath)) {
                    // preserve the origin file's last modified time
                    je.setLastModifiedTime(Files.getLastModifiedTime(filePath));
                    jos.write(Files.readAllBytes(filePath));
                }
                jos.closeEntry();
                if (listener != null) {
                    listener.onEntryWritten(filePath, entryName);
                }
            }
        } catch (Exception e) {
            throw new ExportJarException(e);
        }
    }
}

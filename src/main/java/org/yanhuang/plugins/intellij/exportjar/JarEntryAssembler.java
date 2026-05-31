package org.yanhuang.plugins.intellij.exportjar;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Assembles jar entry names and their source file paths. Pure list/string logic with no
 * IntelliJ platform dependency, extracted from {@link ExportPacker} so the entry-name
 * normalization and directory-entry generation can be unit tested without a platform fixture.
 */
class JarEntryAssembler {
    private final List<Path> filePaths = new ArrayList<>();
    private final List<String> entryNames = new ArrayList<>();

    int size() {
        return filePaths.size();
    }

    Path filePathAt(int index) {
        return filePaths.get(index);
    }

    List<Path> getFilePaths() {
        return filePaths;
    }

    List<String> getEntryNames() {
        return entryNames;
    }

    void addEntry(String packagePath, Path filePath) {
        addEntry(packagePath, filePath, null);
    }

    void addEntry(String packagePath, Path filePath, String entryName) {
        filePaths.add(filePath);
        String normalEntryName = entryName == null ? "" : entryName;
        if (entryName == null) {
            String normalPackagePath = "".equals(packagePath) ? "" : packagePath.endsWith("/") ? packagePath : packagePath + "/";
            normalEntryName = normalPackagePath + filePath.getFileName();
        }
        entryNames.add(normalEntryName);
    }

    void addDirectoryEntries() {
        final HashSet<String> added = new HashSet<>();
        final int size = entryNames.size();
        for (int i = 0; i < size; i++) {
            addDirectoryEntry(entryNames.get(i), added);
        }
    }

    private void addDirectoryEntry(String entryName, HashSet<String> added) {
        int spIndex = entryName.indexOf("/");
        while (spIndex > 0) {
            final String dir = entryName.substring(0, spIndex + 1);
            if (added.add(dir)) {
                filePaths.add(null);
                entryNames.add(dir);
            }
            spIndex = entryName.indexOf("/", spIndex + 1);
        }
    }
}

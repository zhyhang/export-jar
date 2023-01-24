package org.yanhuang.plugins.intellij.exportjar;

import org.yanhuang.plugins.intellij.exportjar.model.ExportJarInfo;
import org.yanhuang.plugins.intellij.exportjar.model.ExportOptions;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * operation history data
 */
public class HistoryData {

    private static final int maxHistory = 2022;

    private ExportJarInfo[] savedJarInfo;

    private ExportOptions[] lastExportOptions;

    public ExportJarInfo[] getSavedJarInfo() {
        return savedJarInfo;
    }

    public void setSavedJarInfo(ExportJarInfo[] savedJarInfo) {
        this.savedJarInfo = savedJarInfo;
    }

    public ExportOptions[] getLastExportOptions() {
        return lastExportOptions;
    }

    public void setLastExportOptions(ExportOptions[] lastExportOptions) {
        this.lastExportOptions = lastExportOptions;
    }

    public void addSavedJarInfo(ExportJarInfo savedJarInfo) {
        if (savedJarInfo == null) {
            return;
        }
        Comparator<Long> comparator = Long::compareTo;
        TreeMap<Long, ExportJarInfo> savedTree = new TreeMap<>(comparator.reversed());
        if (this.savedJarInfo != null) {
            Arrays.stream(this.savedJarInfo).forEach(s -> {
                if (savedJarInfo.equals(s)) {
                    savedTree.put(savedJarInfo.getCreation(), savedJarInfo);
                } else {
                    savedTree.put(s.getCreation(), s);
                }
            });
        }
        savedTree.put(savedJarInfo.getCreation(), savedJarInfo);
        this.savedJarInfo = savedTree.values().toArray(new ExportJarInfo[0]);
        if (this.savedJarInfo.length > maxHistory) {
            this.savedJarInfo = Arrays.copyOf(this.savedJarInfo, maxHistory);
        }
    }

}

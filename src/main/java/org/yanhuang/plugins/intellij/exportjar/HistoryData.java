package org.yanhuang.plugins.intellij.exportjar;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.TreeMap;

/**
 * operation history data
 */
public class HistoryData {

    private static final int maxHistory = 100;

    private SavedJarInfo[] savedJarInfo;

    public SavedJarInfo[] getSavedJarInfo() {
        return savedJarInfo;
    }

    public void setSavedJarInfo(SavedJarInfo[] savedJarInfo) {
        this.savedJarInfo = savedJarInfo;
    }

    public void addSavedJarInfo(SavedJarInfo savedJarInfo) {
        if (savedJarInfo == null) {
            return;
        }
        Comparator<Long> comparator = Long::compareTo;
        TreeMap<Long, SavedJarInfo> savedTree = new TreeMap<>(comparator.reversed());
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
        this.savedJarInfo = savedTree.values().toArray(new SavedJarInfo[0]);
        if (this.savedJarInfo.length > maxHistory) {
            this.savedJarInfo = Arrays.copyOf(this.savedJarInfo, maxHistory);
        }
    }

    public static class SavedJarInfo implements Comparable<SavedJarInfo> {
        private String path;
        private long creation;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public long getCreation() {
            return creation;
        }

        public void setCreation(long creation) {
            this.creation = creation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SavedJarInfo that = (SavedJarInfo) o;
            return path.equals(that.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }

        @Override
        public int compareTo(@NotNull SavedJarInfo that) {
            if (this == that) {
                return 0;
            }
            if (getClass() != that.getClass()) return 1;
            return Long.compare(this.getCreation(), that.getCreation());
        }
    }
}

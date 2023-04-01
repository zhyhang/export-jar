package org.yanhuang.plugins.intellij.exportjar.model;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ExportJarInfo implements Comparable<ExportJarInfo> {
    private String path;
    private long createTime;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(final long createTime) {
        this.createTime = createTime;
    }

    // compatible with older version
    public long getCreation() {
        return createTime;
    }

    // compatible with older version
    public void setCreation(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportJarInfo that = (ExportJarInfo) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public int compareTo(@NotNull ExportJarInfo that) {
        if (this == that) {
            return 0;
        }
        if (getClass() != that.getClass()) return 1;
        return Long.compare(that.getCreateTime(), this.getCreateTime());
    }
}

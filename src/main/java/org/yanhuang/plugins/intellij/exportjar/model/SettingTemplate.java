package org.yanhuang.plugins.intellij.exportjar.model;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * export settings saved in template include select files, options, jar file and etc.
 */
public class SettingTemplate implements Comparable<SettingTemplate> {

    public static final ExportOptions[] DEFAULT_OPTIONS = {ExportOptions.export_class, ExportOptions.export_java};

    private String name;
    private long createTime;
    private long updateTime;
    private ExportJarInfo[] exportJar;
    private String selectFilesStore;
    private ExportOptions[] options;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(final long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(final long updateTime) {
        this.updateTime = updateTime;
    }

    public ExportJarInfo[] getExportJar() {
        return exportJar;
    }

    public void setExportJar(final ExportJarInfo[] exportJar) {
        this.exportJar = exportJar;
    }

    public String getSelectFilesStore() {
        return selectFilesStore;
    }

    public void setSelectFilesStore(final String selectFilesStore) {
        this.selectFilesStore = selectFilesStore;
    }

    public ExportOptions[] getOptions() {
        return options;
    }

    public void setOptions(final ExportOptions[] options) {
        this.options = options;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SettingTemplate that = (SettingTemplate) o;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(@NotNull final SettingTemplate that) {
        if (this == that) {
            return 0;
        }
        if (getClass() != that.getClass()) return 1;
        return Long.compare(that.getUpdateTime(), this.getUpdateTime());
    }
}

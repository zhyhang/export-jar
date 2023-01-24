package org.yanhuang.plugins.intellij.exportjar.model;

import java.util.List;
import java.util.Map;

public class SettingHistory {
    private long createTime;
    private long updateTime;
    private UISizes ui;
    private boolean templateEnable;
    private SettingTemplate global;
    private Map<String, List<SettingTemplate>> projects;

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

    public UISizes getUi() {
        return ui;
    }

    public void setUi(final UISizes ui) {
        this.ui = ui;
    }

    public boolean isTemplateEnable() {
        return templateEnable;
    }

    public void setTemplateEnable(final boolean templateEnable) {
        this.templateEnable = templateEnable;
    }

    public SettingTemplate getGlobal() {
        return global;
    }

    public void setGlobal(final SettingTemplate global) {
        this.global = global;
    }

    public Map<String, List<SettingTemplate>> getProjects() {
        return projects;
    }

    public void setProjects(final Map<String, List<SettingTemplate>> projects) {
        this.projects = projects;
    }
}

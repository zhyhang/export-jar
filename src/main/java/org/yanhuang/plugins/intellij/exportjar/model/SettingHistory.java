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

}

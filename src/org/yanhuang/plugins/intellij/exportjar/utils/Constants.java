package org.yanhuang.plugins.intellij.exportjar.utils;

import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.nio.file.Path;

public interface Constants {
    String actionName = "Export Jar";
    String exportCommitActionName = "Export Jar from Local Changes";
    String exportCommitButtonName = "Export Jar...";
    String infoTabName = "Packing Export Jar";
    String msgFileList = "Select files";
    String creator = "yanhuang.org";
    Path cachePath = SystemUtils.getUserHome().toPath().resolve(".IntelliJIdea_export_jar");
    Path historyFilePath = cachePath.resolve("select_history.json");
    Dimension settingDialogSize = new Dimension(660, 420);
    Dimension settingPanelSize = new Dimension(660, 360);
    Dimension defaultSize = new Dimension(-1, -1);

}

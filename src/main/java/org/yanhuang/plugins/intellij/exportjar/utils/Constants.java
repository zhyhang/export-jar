package org.yanhuang.plugins.intellij.exportjar.utils;

import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.nio.file.Path;

public interface Constants {
    int maxTemplateHistoryPerProject=512;
    int maxJarHistoryPerTemplate=256;
    String actionName = "Export Jar";
    String exportCommitActionName = "Export Jar from Local Changes";
    String exportCommitButtonName = "Export Jar...";
    String infoTabName = "Packing Export Jar";
    String titleFileList = "Select files";
    String titleTemplateSetting = "Templates";

    String titleTemplateMessageDialog = "Template";
    String titleJarFileSeparator = "Jar file";
    String titleOptionSeparator = "Options";
    String actionNameExplorer = "Explorer";
    String actionNameCopy = "Copy";
    String creator = "yanhuang.org";
    Path cachePath = SystemUtils.getUserHome().toPath().resolve(".IntelliJIdea_export_jar");
    Path historyFilePath = cachePath.resolve("select_history.json");
    Path historyFilePath2023 = cachePath.resolve("history_v2023.json");
    String historySelectsFilePathPrefix2023 = "history_v2023_";
    String historySelectsFilePathSuffix2023 = "_selects.json";
    Path historyFileOperationLock = cachePath.resolve("history_operation.lck");
    Dimension settingDialogSize = new Dimension(800, 420);
    Dimension fileListSettingSplitPanelSize = new Dimension(780, 410);

    String messageTemplateSaveSuccess = "template %s saved successfully";
    String messageTemplateNameNull = "Please specify template name";
    String messageTemplateStoreSelectsError = "write select files to store %s error\n %s";
    String messageTemplateReadSelectsError = "read select files from store %s error\n %s";

    String[] templateDefaultName = {""};
    String templateGlobalName = "template-global";

    String toolTipEmptyTemplate = "Please enter the template name";
}

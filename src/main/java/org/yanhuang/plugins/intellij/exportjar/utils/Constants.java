package org.yanhuang.plugins.intellij.exportjar.utils;

import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.nio.file.Path;

public interface Constants {
    String flagFileSelectRecursive = "[R]";
    int maxTemplateHistoryPerProject=512;
    int maxJarHistoryPerTemplate=256;
    int maxTemplateExportActionSize = 12*2;
    String actionName = "Export Jar";
    String exportCommitActionName = "Export Jar from Local Changes";
    String exportCommitButtonName = "Export Jar...";
    String actionNameRecursiveSelectByDir = "Recursive Select (files in directory recursively export)";
    String actionNameCancelRecursiveSelectByDir = "Cancel Recursive Select (only select files export)";
    String infoTabName = "Packing Export Jar";
    String titleFileList = "Select Files";
    String titleTemplateSetting = "Templates";

    String titleTemplateMessageDialog = "Template";
    String titleJarFileSeparator = "Jar File";
    String titleOptionSeparator = "Options";
    String titleHistorySaveErr = "Save History Error";

    String actionNameExplorer = "Explorer";
    String actionNameCopy = "Copy";
    String creator = "yanhuang.org";
    Path cachePath = SystemUtils.getUserHome().toPath().resolve(".IntelliJIdea_export_jar");
    Path historyFilePath = cachePath.resolve("select_history.json");
    Path historyFilePath2023 = cachePath.resolve("history_v2023.json");
    String historySelectsFilePathPrefix2023 = "history_v2023_";
    String historySelectsFilePathPrefix2024 = "history_v2024_";
    String historySelectsFilePathSuffix2023 = "_selects.json";
    Dimension settingDialogSize = new Dimension(800, 420);
    Dimension fileListSettingSplitPanelSize = new Dimension(780, 410);

    String messageTemplateSaveSuccess = "template %s save successfully";
    String messageTemplateDelSuccess = "template %s delete successfully";
    String messageTemplateDelError = "template %s delete failure, possible not exists";
    String messageTemplateDelYesNo = "Would you delete the template %s ?";
    String messageTemplateNameNull = "Please specify template name";
    String messageTemplateStoreSelectsError = "write select files to store %s error\n %s";
    String messageTemplateReadSelectsError = "read select files from store %s error\n %s";

    String[] templateDefaultName = {""};
    String templateGlobalName = "template-global";
    String toolTipEmptyTemplate = "Please enter the template name";

    String messageTemplateFileNotFound = " in template [%s] not found, possible removed after last exporting.";

    String messageMigrationHistorySuccess = "Migrate history from old version successfully!";

    String toolTipRecursiveDirectorySelect = flagFileSelectRecursive+": Files in the directory and in it's descendents will export.";

    String notifyRecursiveSelectDirectory = "Recursively select %d directories, %d files.";
    String notifyCancelRecursiveSelectDirectory = "Unselect %d directories.";

}

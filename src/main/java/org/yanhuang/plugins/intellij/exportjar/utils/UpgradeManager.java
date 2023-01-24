package org.yanhuang.plugins.intellij.exportjar.utils;

/**
 * plugin upgrade migration manager
 */
public class UpgradeManager {

    /**
     * migration to v2023 history saved model.
     */
    public static void migrateHistoryToV2023(){
        // if old version not exists, do nothing
        if (!Constants.historyFilePath.toFile().exists()) {
            return;
        }
        // if v2023 history file already exists, migration is done.
        if (Constants.historyFilePath2023.toFile().exists()) {
            return;
        }
        // do migration, no file lock using, because export dialog is modal, rarely concurrent operation occur
        migrateV2023();
    }

    private static void migrateV2023(){

    }

}

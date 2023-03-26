package org.yanhuang.plugins.intellij.exportjar.utils;

import com.intellij.openapi.project.Project;
import org.yanhuang.plugins.intellij.exportjar.HistoryData;
import org.yanhuang.plugins.intellij.exportjar.model.SettingTemplate;
import org.yanhuang.plugins.intellij.exportjar.settings.HistoryDao;

import java.nio.file.Files;

import static org.yanhuang.plugins.intellij.exportjar.utils.Constants.*;
import static org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils.info;
import static org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils.warn;

/**
 * plugin upgrade migration manager
 */
public class UpgradeManager {

	/**
	 * migration to v2023 history saved model.
	 */
	public static void migrateHistoryToV2023(Project project) {
		// if old version not exists, do nothing
		if (!historyFilePath.toFile().exists()) {
			return;
		}
		// if v2023 history file already exists, migration is done.
		if (historyFilePath2023.toFile().exists()) {
			return;
		}
		// do migration, no file lock using, because export dialog is modal, rarely concurrent operation occur
		migrateV2023(project);
	}

	private static void migrateV2023(Project project) {
		final HistoryDao dao = new HistoryDao();
		// save default history
		dao.initV2023();
		// read old history
		final HistoryData oldHistory = readOldVersionHistory(project);
		// write new history
		if (oldHistory != null) {
			final var newGlobalTemplate = new SettingTemplate();
			newGlobalTemplate.setExportJar(oldHistory.getSavedJarInfo());
			newGlobalTemplate.setOptions(oldHistory.getLastExportOptions());
			final long ts = System.currentTimeMillis();
			newGlobalTemplate.setCreateTime(ts);
			newGlobalTemplate.setUpdateTime(ts);
			newGlobalTemplate.setName(templateGlobalName);
			dao.saveGlobal(newGlobalTemplate);
			info(project, messageMigrationHistorySuccess);
		}
	}

	private static HistoryData readOldVersionHistory(Project project) {
		if (Files.exists(historyFilePath)) {
			try {
				final String historyJson = Files.readString(historyFilePath);
				return CommonUtils.fromJson(historyJson, HistoryData.class);
			} catch (Exception e) {
				warn(project, e.getMessage());
				return null;
			}
		} else {
			return null;
		}
	}

}

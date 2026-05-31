package org.yanhuang.plugins.intellij.exportjar.utils;

import com.intellij.openapi.project.Project;
import org.yanhuang.plugins.intellij.exportjar.HistoryData;
import org.yanhuang.plugins.intellij.exportjar.model.SettingTemplate;
import org.yanhuang.plugins.intellij.exportjar.settings.HistoryDao;

import java.nio.file.Files;
import java.nio.file.Path;

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
		migrateHistoryToV2023(project, Constants.cachePath);
	}

	public static void migrateHistoryToV2023(Project project, Path cacheRoot) {
		final Path oldHistoryFile = cacheRoot.resolve(historyFileName);
		final Path v2023File = cacheRoot.resolve(historyFileName2023);
		// if old version not exists, do nothing
		if (!oldHistoryFile.toFile().exists()) {
			return;
		}
		// if v2023 history file already exists, migration is done.
		if (v2023File.toFile().exists()) {
			return;
		}
		// do migration, no file lock using, because export dialog is modal, rarely concurrent operation occur
		migrateV2023(project, cacheRoot, oldHistoryFile);
	}

	private static void migrateV2023(Project project, Path cacheRoot, Path oldHistoryFile) {
		final HistoryDao dao = new HistoryDao(cacheRoot);
		// save default history
		dao.initV2023();
		// read old history
		final HistoryData oldHistory = readOldVersionHistory(project, oldHistoryFile);
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

	private static HistoryData readOldVersionHistory(Project project, Path oldHistoryFile) {
		if (Files.exists(oldHistoryFile)) {
			try {
				final String historyJson = Files.readString(oldHistoryFile);
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

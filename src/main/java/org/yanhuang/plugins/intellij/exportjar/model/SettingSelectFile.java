package org.yanhuang.plugins.intellij.exportjar.model;

import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;
import java.util.*;

/**
 * save select exporting files (include file and directory), used to template saving history.
 * <li>include/exclude detail first (bigger level first)</li>
 * <li>include/exclude direct first (indirect second)</li>
 * <li>exclude first, include second</li>
 * <li>example:</li>
 * <li>    [/a/b/f1:  exclude /a/b/f1] vs [/a/b/f1: include /a/b/f1] => exclude /a/b/f1 </li>
 * <li>    [/a/b/f1:  exclude /a/b] vs [/a/b/f1: include /a] => exclude /a/b/f1 </li>
 * <li>     [/a/b/f1:  include /a/b] vs [/a/b/f1: exclude /a] => include /a/b/f1 </li>
 */
public class SettingSelectFile {
	private Path filePath;
	private boolean recursive;
	private SelectType selectType=SelectType.include;

	private transient Map<Path, VirtualFile> mappingVfs;

	public Path getFilePath() {
		return filePath;
	}

	public void setFilePath(Path filePath) {
		this.filePath = filePath;
	}

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	public SelectType getSelectType() {
		return selectType;
	}

	public void setSelectType(SelectType selectType) {
		this.selectType = selectType;
	}

	public boolean isDirectory() {
		return filePath != null && filePath.toFile().isDirectory();
	}

	public void setMappingVfs(Map<Path, VirtualFile> mappingVfs) {
		this.mappingVfs = mappingVfs;
	}

	public enum SelectType {
		include, exclude
	}

	/**
	 * combine select files to final virtual files (include and exclude according to select type).
	 *
	 * @param selectFiles array of original select files (not directory)
	 * @return final select files by combining include and exclude according to select type.
	 */
	public static Map<Path, VirtualFile> combineFinalVirtualFiles(SettingSelectFile[] selectFiles) {
		if (selectFiles == null || selectFiles.length == 0) {
			return Map.of();
		}
		final var excludeSettingMap = makeExcludeSettingMap(selectFiles);
		final var finalMap = new HashMap<Path, VirtualFile>();
		for (final SettingSelectFile selectFile : selectFiles) {
			combineOneSettingFile(selectFile, excludeSettingMap, finalMap);
		}
		return finalMap;
	}

	private static void combineOneSettingFile(SettingSelectFile selectFile,
	                                          Map<VirtualFile, List<SettingSelectFile>> excludeSettingMap,
	                                          HashMap<Path, VirtualFile> finalMap) {
		if (selectFile == null) {
			return;
		}
		final var selectFileMapping = selectFile.mappingVfs;
		if (selectFileMapping == null || selectFileMapping.isEmpty()) {
			return;
		}
		for (Map.Entry<Path, VirtualFile> virtualFileEntry : selectFileMapping.entrySet()) {
			final VirtualFile virtualFile = virtualFileEntry.getValue();
			if (isInclude(selectFile, virtualFile, excludeSettingMap)) {
				finalMap.put(virtualFileEntry.getKey(), virtualFile);
			}
		}
	}

	private static Map<VirtualFile, List<SettingSelectFile>> makeExcludeSettingMap(SettingSelectFile[] selectFiles) {
		final var excludeSettingMap = new HashMap<VirtualFile, List<SettingSelectFile>>();
		for (final SettingSelectFile selectFile : selectFiles) {
			if (selectFile == null) {
				continue;
			}
			final var selectFileMapping = selectFile.mappingVfs;
			if (selectFile.getSelectType() == SelectType.include
					|| selectFileMapping == null || selectFileMapping.isEmpty()) {
				continue;
			}
			selectFileMapping
					.values()
					.stream()
					.filter(Objects::nonNull)
					.forEach(virtualFile -> excludeSettingMap.computeIfAbsent(virtualFile, k -> new ArrayList<>()).add(selectFile));
		}
		return excludeSettingMap;
	}

	private static boolean isInclude(SettingSelectFile selectFile, VirtualFile selectVirtualFile,
	                                 Map<VirtualFile, List<SettingSelectFile>> excludeSettingMap) {
		if (selectFile == null || selectFile.getSelectType() != SelectType.include || selectFile.filePath == null) {
			return false;
		}
		final List<SettingSelectFile> settingSelectFiles = excludeSettingMap.get(selectVirtualFile);
		if (settingSelectFiles == null || settingSelectFiles.isEmpty()) {
			return true;
		}
		for (SettingSelectFile selectFile1 : settingSelectFiles) {
			if (selectFile1.filePath == null) {
				continue;
			}
			if (selectFile1.filePath.startsWith(selectFile.filePath)) {
				return false;
			}
		}
		return true;
	}
}


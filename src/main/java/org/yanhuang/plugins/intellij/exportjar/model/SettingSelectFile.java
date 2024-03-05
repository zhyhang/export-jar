package org.yanhuang.plugins.intellij.exportjar.model;

import com.intellij.openapi.vfs.VirtualFile;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;

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
public class SettingSelectFile implements Cloneable {
	private String filePath;
	private boolean recursive;
	private SelectType selectType=SelectType.noop ;
	private transient VirtualFile virtualFile;
	private transient Path nioPath;
	private transient Map<Path, Object> mappingVfs = new HashMap<>();

	public void shallowOverrideFrom(SettingSelectFile anotherFile) {
		this.filePath = anotherFile.getFilePath();
		this.recursive = anotherFile.isRecursive();
		this.selectType = anotherFile.getSelectType();
		this.mappingVfs = anotherFile.mappingVfs;
		this.virtualFile = anotherFile.getVirtualFile();
		this.nioPath = anotherFile.getNioPath();
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
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

	public void putMappingVf(Path filePath, Object treeNodeObject) {
		this.mappingVfs.put(filePath, treeNodeObject);
	}

	public VirtualFile getVirtualFile() {
		if (filePath != null && this.virtualFile==null) {
			this.virtualFile = CommonUtils.fromOsFile(filePath);
		}
		return virtualFile;
	}

	public Path getNioPath() {
		if (filePath != null && this.nioPath == null) {
			this.nioPath = Path.of(filePath);
		}
		return nioPath;
	}

	@Override
	public SettingSelectFile clone() {
		try {
			final SettingSelectFile clone = (SettingSelectFile) super.clone();
			clone.mappingVfs = new HashMap<>();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	public enum SelectType {
		include, exclude,noop
	}

	/**
	 * combine select files to final virtual files (include and exclude according to select type).
	 *
	 * @param selectFiles array of original select files (not directory)
	 * @return final select files by combining include and exclude according to select type. list.get(0) includes, list.get(1) excludes.
	 */
	public static List<Map<Path, Object>> combineFinalVirtualFiles(SettingSelectFile[] selectFiles) {
		if (selectFiles == null || selectFiles.length == 0) {
			return List.of(Map.of(),Map.of());
		}
		final var excludeSettingMap = makeExcludeSettingMap(selectFiles);
		final var finalIncludeMap = new HashMap<Path, Object>();
		final var finalExcludeMap = new HashMap<Path, Object>();
		for (final SettingSelectFile selectFile : selectFiles) {
			combineOneSettingFile(selectFile, excludeSettingMap, finalIncludeMap, finalExcludeMap);
		}
		return List.of(finalIncludeMap,finalExcludeMap);
	}

	private static void combineOneSettingFile(SettingSelectFile selectFile,
	                                          Map<Object, List<SettingSelectFile>> excludeSettingMap,
	                                          Map<Path, Object> finalIncludeMap, Map<Path,Object> finalExcludeMap) {
		if (selectFile == null) {
			return;
		}
		final var selectFileMapping = selectFile.mappingVfs;
		if (selectFileMapping == null || selectFileMapping.isEmpty()) {
			return;
		}
		for (Map.Entry<Path, Object> virtualFileEntry : selectFileMapping.entrySet()) {
			final Object virtualFile = virtualFileEntry.getValue();
			if (isInclude(selectFile, virtualFile, excludeSettingMap)) {
				finalIncludeMap.put(virtualFileEntry.getKey(), virtualFile);
			}else{
				finalExcludeMap.put(virtualFileEntry.getKey(), virtualFile);
			}
		}
	}

	private static Map<Object, List<SettingSelectFile>> makeExcludeSettingMap(SettingSelectFile[] selectFiles) {
		final var excludeSettingMap = new HashMap<Object, List<SettingSelectFile>>();
		for (final SettingSelectFile selectFile : selectFiles) {
			if (selectFile == null) {
				continue;
			}
			final var selectFileMapping = selectFile.mappingVfs;
			if (selectFile.getSelectType() != SelectType.exclude
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

	private static boolean isInclude(SettingSelectFile selectFile, Object selectVirtualFile,
	                                 Map<Object, List<SettingSelectFile>> excludeSettingMap) {
		if (selectFile == null || selectFile.getSelectType() == SelectType.exclude || selectFile.filePath == null) {
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
			if (Path.of(selectFile1.filePath).startsWith(Path.of(selectFile.filePath))) {
				return false;
			}
		}
		return true;
	}
}


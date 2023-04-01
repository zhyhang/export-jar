package org.yanhuang.plugins.intellij.exportjar.settings;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.model.ExportJarInfo;
import org.yanhuang.plugins.intellij.exportjar.model.SettingHistory;
import org.yanhuang.plugins.intellij.exportjar.model.SettingTemplate;
import org.yanhuang.plugins.intellij.exportjar.model.UISizes;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;
import org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.yanhuang.plugins.intellij.exportjar.utils.Constants.*;
import static org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils.errorNotify;

public class HistoryDao {

	/**
	 * create v2023 history and save if that not exists.
	 */
	public void initV2023() {
		if (Constants.historyFilePath2023.toFile().exists()) {
			return;
		}
		save(defaultHistory());
	}

	@NotNull
	private SettingHistory defaultHistory() {
		final SettingHistory history = new SettingHistory();
		final long ts = System.currentTimeMillis();
		history.setCreateTime(ts);
		history.setUpdateTime(ts);
		history.setUi(UISizes.DEFAULT_UI);
		history.setGlobal(defaultGlobalTemplate());
		return history;
	}

	private SettingHistory save(SettingHistory history) {
		if (!Constants.cachePath.toFile().exists()) {
			final boolean ignoredMkdirs = Constants.cachePath.toFile().mkdirs();
		}
		try {
			final String json = CommonUtils.toJson(history);
			Files.writeString(Constants.historyFilePath2023, json);
			return history;
		} catch (IOException e) {
			MessagesUtils.errorNotify(Constants.titleHistorySaveErr, e.getMessage());
			return null;
		}
	}

	private SettingTemplate defaultGlobalTemplate() {
		final long ts = System.currentTimeMillis();
		final SettingTemplate globalTemplate = new SettingTemplate();
		globalTemplate.setName(Constants.templateGlobalName);
		globalTemplate.setOptions(SettingTemplate.DEFAULT_OPTIONS);
		globalTemplate.setCreateTime(ts);
		globalTemplate.setUpdateTime(ts);
		globalTemplate.setExportJar(new ExportJarInfo[0]);
		return globalTemplate;
	}

	/**
	 * save global template to history [merge with old global template]
	 * @param globalNewTemplate new global template
	 */
	public void saveGlobal(SettingTemplate globalNewTemplate) {
		final SettingHistory history = readOrDefault();
		final var savedGlobal = Objects.requireNonNullElse(history.getGlobal(), defaultGlobalTemplate());
		mergeTemplate(globalNewTemplate, savedGlobal);
		history.setGlobal(savedGlobal);
		history.setUpdateTime(System.currentTimeMillis());
		save(history);
	}

	/**
	 * save project's setting (template) to history
	 *
	 * @param project project
	 * @param newTemplate new setting template
	 * @return when success, return saved history, or else null.
	 */
	public SettingHistory saveProject(String project, SettingTemplate newTemplate) {
		final SettingHistory history = readOrDefault();
		final Map<String, List<SettingTemplate>> projectTemplateMap =
				new HashMap<>(Optional.ofNullable(history.getProjects()).orElse(Map.of()));
		final List<SettingTemplate> savedTemplates =
				Optional.ofNullable(projectTemplateMap.get(project)).orElse(List.of());
		final List<SettingTemplate> newTemplates = updateOrAddTemplate(savedTemplates, newTemplate);
		projectTemplateMap.put(project, newTemplates);
		history.setProjects(projectTemplateMap);
		history.setUpdateTime(System.currentTimeMillis());
		return save(history);
	}

	private List<SettingTemplate> updateOrAddTemplate(List<SettingTemplate> savedTemplates,
	                                                  SettingTemplate newTemplate) {
		final List<SettingTemplate> newTemplates = new ArrayList<>(savedTemplates);//make list to modified
		return mergeSortCutExceeding(newTemplates, newTemplate, this::mergeTemplate,
				Constants.maxTemplateHistoryPerProject);
	}

	private void mergeTemplate(SettingTemplate src, SettingTemplate dest) {
		dest.setOptions(src.getOptions());
		dest.setSelectFilesStore(src.getSelectFilesStore());
		dest.setUpdateTime(src.getUpdateTime());
		final ExportJarInfo[] destJarArray = Optional.ofNullable(dest.getExportJar()).orElse(new ExportJarInfo[0]);
		if (destJarArray.length == 0) {
			dest.setExportJar(src.getExportJar());
		} else if (src.getExportJar().length > 0) {
			List<ExportJarInfo> destJarList = new ArrayList<>(Arrays.asList(destJarArray));
			for (ExportJarInfo srcJar : src.getExportJar()) {
				destJarList = mergeSortCutExceeding(destJarList, srcJar, this::mergeExportJar,
						Constants.maxJarHistoryPerTemplate);
			}
			dest.setExportJar(destJarList.toArray(new ExportJarInfo[0]));
		}
	}

	private void mergeExportJar(ExportJarInfo src, ExportJarInfo dest) {
		dest.setCreateTime(src.getCreateTime());
	}

	private <T extends Comparable<T>> List<T> mergeSortCutExceeding(List<T> itemList,
	                                                                T item, BiConsumer<T, T> srcDescMerge,
	                                                                int maxItems) {
		final int existsIndex = itemList.indexOf(item);
		if (existsIndex >= 0) {
			final T existsItem = itemList.get(existsIndex);
			srcDescMerge.accept(item, existsItem);
		} else {
			itemList.add(item);
		}
		Collections.sort(itemList);
		if (itemList.size() > maxItems) {
			return itemList.subList(0, maxItems);
		}
		return itemList;
	}

	/**
	 * read history from setting store.
	 * @return saved history, return default history if read error.
	 */
	public SettingHistory readOrDefault() {
		try {
			final String json = Files.readString(Constants.historyFilePath2023);
			return CommonUtils.fromJson(json, SettingHistory.class);
		} catch (IOException e) {
			return defaultHistory();
		}
	}

	/**
	 * save virtual files to store file (separate from main history store file).
	 * @param projectName project name
	 * @param templateName template name
	 * @param selectVFiles virtual files
	 * @return store file path
	 */
	public Path saveSelectFiles(String projectName, final String templateName, final VirtualFile[] selectVFiles) {
		final var selectStore = getSelectFilesStorePath(projectName, templateName);
		final var selectFiles = Arrays.stream(selectVFiles).map(VirtualFile::getPath).collect(Collectors.toList());
		try {
			Files.writeString(selectStore, CommonUtils.toJson(selectFiles));
		} catch (IOException e) {
			final String msg = String.format(messageTemplateStoreSelectsError, selectStore, e.getMessage());
			errorNotify(titleTemplateMessageDialog, msg);
		}
		return selectStore;
	}

	/**
	 * read template's select files to export from store file.
	 *
	 * @param projectName  project name
	 * @param templateName template name
	 * @return os file string mapping VirtualFile, if not exists the os file, the entry value is null. return map
	 * Always not null.
	 */
	public Map<String, VirtualFile> readStoredSelectFiles(String projectName, String templateName) {
		final Path selectStore = getSelectFilesStorePath(projectName, templateName);
		if (selectStore.toFile().exists()) {
			try {
				final String[] storeFiles = CommonUtils.fromJson(Files.readString(selectStore), String[].class);
				return readSelectVirtualFiles(storeFiles);
			} catch (IOException e) {
				final String msg = String.format(messageTemplateReadSelectsError, selectStore, e.getMessage());
				errorNotify(titleTemplateMessageDialog, msg);
				return Map.of();
			}
		} else {
			return Map.of();
		}
	}

	private Map<String, VirtualFile> readSelectVirtualFiles(final String[] storeFiles) {
		if (storeFiles != null && storeFiles.length > 0) {
			final Map<String, VirtualFile> virtualFileMap = new HashMap<>();
			for (String storeFile : storeFiles) {
				final VirtualFile vf = Path.of(storeFile).toFile().exists() ? CommonUtils.fromOsFile(storeFile) : null;
				virtualFileMap.put(storeFile, vf);
			}
			return virtualFileMap;
		} else {
			return Map.of();
		}
	}

	private Path getSelectFilesStorePath(String projectName, String templateName) {
		return cachePath.resolve(historySelectsFilePathPrefix2023
				+ projectName + "_" + templateName + historySelectsFilePathSuffix2023);
	}

	/**
	 * remove template
	 * @param project project of template for
	 * @param template template name
	 * @return updated history, null if not found removing template.
	 */
	public SettingHistory removeTemplate(String project, String template) {
		final SettingHistory history = readOrDefault();
		final var orgTemplates = getProjectTemplates(history, project);
		final var savingTemplates =
				orgTemplates.stream().filter(Predicate.not(t -> t.getName().equalsIgnoreCase(template))).collect(Collectors.toList());
		if (orgTemplates.size() > savingTemplates.size()) {
			final var orgProjectTemplates = history.getProjects();
			final var savingProjectTemplates = new HashMap<>(orgProjectTemplates); //make modified
			savingProjectTemplates.put(project, savingTemplates);
			history.setProjects(savingProjectTemplates);
			history.setUpdateTime(System.currentTimeMillis());
			return save(history);
		} else {
			return null;
		}
	}

	/**
	 * save current ui sizes to history
	 * @param uiSizes ui sizes
	 */
	public void saveUISizes(UISizes uiSizes){
		final SettingHistory history = readOrDefault();
		history.setUi(uiSizes);
		save(history);
	}

	/**
	 * extract project's setting templates from history.
	 * @param history setting history
	 * @param project project name
	 * @return always not null, possible size=0, project's templates.
	 */
	public List<SettingTemplate> getProjectTemplates(SettingHistory history, String project) {
		final List<SettingTemplate> templateList =
				Optional.ofNullable(history.getProjects()).orElse(Map.of()).get(project);
		return templateList != null ? templateList : List.of();
	}

}

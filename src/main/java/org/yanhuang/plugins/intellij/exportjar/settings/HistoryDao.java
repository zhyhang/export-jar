package org.yanhuang.plugins.intellij.exportjar.settings;

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
import java.util.*;
import java.util.function.BiConsumer;

public class HistoryDao {

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
        history.setTemplateEnable(false);
        history.setGlobal(defaultGlobalTemplate());
        return history;
    }

    private void save(SettingHistory history) {
        if (!Constants.cachePath.toFile().exists()) {
            final boolean ignoredMkdirs = Constants.cachePath.toFile().mkdirs();
        }
        final String json = CommonUtils.toJson(history);
        if (json != null) {
            try {
                Files.writeString(Constants.historyFilePath2023, json);
            } catch (IOException e) {
                MessagesUtils.errorNotify("Save History Error", e.getMessage());
            }
        }
    }

    public SettingTemplate defaultGlobalTemplate() {
        final long ts = System.currentTimeMillis();
        final SettingTemplate globalTemplate = new SettingTemplate();
        globalTemplate.setName("global");
        globalTemplate.setOptions(SettingTemplate.DEFAULT_OPTIONS);
        globalTemplate.setCreateTime(ts);
        globalTemplate.setUpdateTime(ts);
        globalTemplate.setExportJar(new ExportJarInfo[0]);
        return globalTemplate;
    }

    public void saveGlobal(SettingTemplate globalNewTemplate) {
        final SettingHistory history = readOrDefault();
        final SettingTemplate savedGlobal = history.getGlobal() != null ? history.getGlobal() : defaultGlobalTemplate();
        mergeTemplate(globalNewTemplate, savedGlobal);
        history.setGlobal(savedGlobal);
        save(history);
    }

    public void saveProject(String project, SettingTemplate newTemplate) {
        final SettingHistory history = readOrDefault();
        final Map<String, List<SettingTemplate>> projectTemplateMap = new HashMap<>(Optional.ofNullable(history.getProjects()).orElse(Map.of()));
        final List<SettingTemplate> savedTemplates = Optional.ofNullable(projectTemplateMap.get(project)).orElse(List.of());
        final List<SettingTemplate> newTemplates = updateOrAddTemplate(savedTemplates, newTemplate);
        projectTemplateMap.put(project, newTemplates);
        history.setProjects(projectTemplateMap);
        history.setUpdateTime(System.currentTimeMillis());
        save(history);
    }

    private List<SettingTemplate> updateOrAddTemplate(List<SettingTemplate> savedTemplates, SettingTemplate newTemplate) {
        final List<SettingTemplate> newTemplates = new ArrayList<>(savedTemplates);//make list to modified
        return mergeSortCutExceeding(newTemplates, newTemplate, this::mergeTemplate, Constants.maxTemplateHistoryPerProject);
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
                destJarList = mergeSortCutExceeding(destJarList, srcJar, this::mergeExportJar, Constants.maxJarHistoryPerTemplate);
            }
            dest.setExportJar(destJarList.toArray(new ExportJarInfo[0]));
        }
    }

    private void mergeExportJar(ExportJarInfo src, ExportJarInfo dest) {
        dest.setCreateTime(src.getCreateTime());
    }

    private <T extends Comparable<T>> List<T> mergeSortCutExceeding(List<T> itemList,
                                                                    T item, BiConsumer<T, T> srcDescMerge, int maxItems) {
        final int existsIndex = itemList.indexOf(item);
        if (existsIndex >= 0) {
            final T existsItem = itemList.get(existsIndex);
            if (existsItem != null) {
                srcDescMerge.accept(item, existsItem);
            } else {
                itemList.add(item);
            }
        } else {
            itemList.add(item);
        }
        Collections.sort(itemList);
        if (itemList.size() > maxItems) {
            return itemList.subList(0, maxItems);
        }
        return itemList;
    }

    public SettingHistory readOrDefault() {
        try {
            final String json = Files.readString(Constants.historyFilePath2023);
            return CommonUtils.fromJson(json, SettingHistory.class);
        } catch (IOException e) {
            return defaultHistory();
        }

    }

}

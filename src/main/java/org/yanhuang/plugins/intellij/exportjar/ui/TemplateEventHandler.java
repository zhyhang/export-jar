package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.vfs.VirtualFile;
import org.yanhuang.plugins.intellij.exportjar.model.ExportJarInfo;
import org.yanhuang.plugins.intellij.exportjar.model.ExportOptions;
import org.yanhuang.plugins.intellij.exportjar.model.SettingHistory;
import org.yanhuang.plugins.intellij.exportjar.model.SettingTemplate;
import org.yanhuang.plugins.intellij.exportjar.settings.HistoryDao;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.openapi.ui.Messages.showInfoMessage;
import static org.yanhuang.plugins.intellij.exportjar.utils.Constants.*;

/**
 * handle template related events
 */
public class TemplateEventHandler {
    private final HistoryDao historyDao = new HistoryDao();
    private final SettingDialog settingDialog;

    public TemplateEventHandler(final SettingDialog settingDialog) {
        this.settingDialog = settingDialog;
    }

    public void initByHistory(SettingHistory history) {
        settingDialog.templateEnableCheckBox.setSelected(history.isTemplateEnable());
        updateUI();
        //TODO setting history uiSize already used to init, do not handle again
    }

    public void templateEnableChanged(ChangeEvent e) {
        updateUI();
    }

    public void templateSelectChanged(ItemEvent e){
        final Object item = e.getItem();
        final String name = Objects.toString(item);
        updateTemplateListTooltip(name);
        final SettingHistory history = historyDao.readOrDefault();
        final List<SettingTemplate> templates = getProjectTemplates(history);
        final Optional<SettingTemplate> templateOptional = getTemplateByName(templates, name);
        templateOptional.ifPresent(this::updateUIOptions);
    }

    private void updateUI() {
        final boolean templateEnable = updateUIState();
        if (templateEnable) {
            updateUIDataEnableTemplate();
        }else{
            updateUIDataDisableTemplate();
        }
    }

    private boolean updateUIState() {
        final boolean templateEnable = settingDialog.templateEnableCheckBox.isSelected();
        settingDialog.templateSelectComBox.setEnabled(templateEnable);
        settingDialog.templateSaveButton.setEnabled(templateEnable);
        settingDialog.templateDelButton.setEnabled(templateEnable);
        return templateEnable;
    }

    private void updateTemplateListTooltip(String tooltip){
        settingDialog.templateSelectComBox.setToolTipText(tooltip);
    }

    private void updateUIDataEnableTemplate() {
        final Object item = settingDialog.templateSelectComBox.getSelectedItem();
        if (item == null || item.toString().isBlank()) {
            final SettingHistory history = historyDao.readOrDefault();
            final List<SettingTemplate> templateList = getProjectTemplates(history);
            if (templateList != null && templateList.size()>0) {
                updateUIDataBy(templateList);
            } else {
                final ComboBoxModel<String> model = new DefaultComboBoxModel<>(templateDefaultName);
                settingDialog.templateSelectComBox.setModel(model);
            }
        }
        updateTemplateListTooltip(Objects.toString(settingDialog.templateSelectComBox.getSelectedItem()));
    }

    private List<SettingTemplate> getProjectTemplates(SettingHistory history) {
        return Optional.ofNullable(history.getProjects()).orElse(Map.of()).get(settingDialog.project.getName());
    }

    private Optional<SettingTemplate> getTemplateByName(List<SettingTemplate> templates, String name) {
        return templates.stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst();
    }

    private void updateUIDataBy(final List<SettingTemplate> templateList) {
        final String[] names = templateList.stream().map(SettingTemplate::getName).toArray(String[]::new);
        final ComboBoxModel<String> model = new DefaultComboBoxModel<>(names);
        settingDialog.templateSelectComBox.setModel(model);
        updateUIOptions(templateList.get(0));
        // TODO update select files panel, setModel fire the change event, use listener to update select files
        // TODO update export jar list
    }

    private void updateUIOptions(SettingTemplate template){
        final Component[] components = settingDialog.optionsPanel.getComponents();
        final Set<String> optionSet = Optional.ofNullable(template.getOptions()).stream().flatMap(Arrays::stream)
                .map(ExportOptions::name)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (optionSet.size() == 0) {
            return;
        }
        //set select state according to last saved
        for (Component c : components) {
            if (c instanceof JCheckBox && optionSet.contains(c.getName().toLowerCase())) {
                ((JCheckBox) c).setSelected(true);
            } else if (c instanceof JCheckBox) {
                ((JCheckBox) c).setSelected(false);
            }
        }
    }

    private void updateUIDataDisableTemplate(){
        final SettingHistory history = historyDao.readOrDefault();
        updateUIOptions(history.getGlobal());
        // TODO update export jar list  from global template and select file list and etc.
    }



    public void saveTemplate(ActionEvent e){
        final Object name = settingDialog.templateSelectComBox.getSelectedItem();
        if (name == null || name.toString().isBlank()) {
            showErrorDialog(messageTemplateNameNull, titleTemplateMessageDialog);
        }else{
            final SettingTemplate curTemplate = new SettingTemplate();
            final String templateName = name.toString();
            final long ts = System.currentTimeMillis();
            curTemplate.setCreateTime(ts);
            curTemplate.setUpdateTime(ts);
            curTemplate.setName(templateName);
            curTemplate.setExportJar(getExportJar());
            curTemplate.setOptions(settingDialog.pickExportOptions());
            final Path selectStore = storeSelectFiles(templateName);
            curTemplate.setSelectFilesStore(selectStore.toString());
            historyDao.saveProject(settingDialog.project.getName(), curTemplate);
            showInfoMessage(String.format(messageTemplateSaveSuccess, templateName), titleTemplateMessageDialog);
        }

    }

    private ExportJarInfo[] getExportJar(){
        final Object jarFile = settingDialog.outPutJarFileComboBox.getSelectedItem();
        if (jarFile == null || jarFile.toString().isBlank()) {
            return new ExportJarInfo[0];
        }else{
            final ExportJarInfo jarInfo = new ExportJarInfo();
            jarInfo.setPath(jarFile.toString());
            jarInfo.setCreateTime(System.currentTimeMillis());
            return new ExportJarInfo[]{jarInfo};
        }
    }

    private Path storeSelectFiles(final String templateName){
        return this.historyDao.saveSelectFiles(settingDialog.project.getName(), templateName,
                settingDialog.getSelectedFiles());
    }

    private VirtualFile[] readStoredSelectFiles(final String templateName) {
        return null;//TODO
    }

}

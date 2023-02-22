package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.vfs.VirtualFile;
import org.yanhuang.plugins.intellij.exportjar.model.ExportJarInfo;
import org.yanhuang.plugins.intellij.exportjar.model.SettingHistory;
import org.yanhuang.plugins.intellij.exportjar.model.SettingTemplate;
import org.yanhuang.plugins.intellij.exportjar.settings.HistoryDao;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    }

    public void templateEnableChanged(ChangeEvent e) {
        updateUI();
    }

    private void updateUI() {
        final boolean templateEnable = updateUIState();
        if (templateEnable) {
            updateUIData();
        }else{
            // TODO updateUIDataDisableTemplate();
            // read options to from global template and etc.
        }
    }

    private boolean updateUIState() {
        final boolean templateEnable = settingDialog.templateEnableCheckBox.isSelected();
        settingDialog.templateSelectComBox.setEnabled(templateEnable);
        settingDialog.templateSaveButton.setEnabled(templateEnable);
        settingDialog.templateDelButton.setEnabled(templateEnable);
        return templateEnable;
    }

    private void updateUIData() {
        final Object item = settingDialog.templateSelectComBox.getSelectedItem();
        if (item == null || item.toString().isBlank()) {
            final SettingHistory history = historyDao.readOrDefault();
            final List<SettingTemplate> templateList = Optional.ofNullable(history.getProjects()).orElse(Map.of()).get(settingDialog.project.getName());
            if (templateList != null && templateList.size()>0) {
                updateUIDataBy(templateList);
            } else {
                final ComboBoxModel<String> model = new DefaultComboBoxModel<>(templateDefaultName);
                settingDialog.templateSelectComBox.setModel(model);
            }
        }
    }

    private void updateUIDataBy(final List<SettingTemplate> templateList) {
        final String[] names = templateList.stream().map(SettingTemplate::getName).toArray(String[]::new);
        final ComboBoxModel<String> model = new DefaultComboBoxModel<>(names);
        settingDialog.templateSelectComBox.setModel(model);
        // TODO update select files panel, setModel fire the change event, use listener to update select files
        // TODO update checkbox(options) panel
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

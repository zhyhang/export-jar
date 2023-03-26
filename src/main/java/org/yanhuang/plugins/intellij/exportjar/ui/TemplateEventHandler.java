package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.vfs.VirtualFile;
import org.yanhuang.plugins.intellij.exportjar.model.ExportJarInfo;
import org.yanhuang.plugins.intellij.exportjar.model.ExportOptions;
import org.yanhuang.plugins.intellij.exportjar.model.SettingHistory;
import org.yanhuang.plugins.intellij.exportjar.model.SettingTemplate;
import org.yanhuang.plugins.intellij.exportjar.settings.HistoryDao;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.openapi.ui.Messages.showInfoMessage;
import static org.yanhuang.plugins.intellij.exportjar.utils.Constants.*;
import static org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils.errorNotify;
import static org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils.warn;

/**
 * handle template related events
 */
public class TemplateEventHandler {
	private final HistoryDao historyDao = new HistoryDao();
	private final SettingDialog settingDialog;
	/**
	 * Transient template for storing no-template-name settings
	 */
	private VirtualFile[] transientTemplateSelectFiles;
	private final SettingTemplate transientTemplate = new SettingTemplate();

	public TemplateEventHandler(final SettingDialog settingDialog) {
		this.settingDialog = settingDialog;
		// do not save transient template in constructor, will lead logic error!!
	}

	public void initUI(SettingHistory history, String curTemplate) {
		updateUI(history, curTemplate);
		//TODO setting history uiSize already used to init, do not handle again
		//TODO save uiSize in save template/export actions
	}

	public void templateEnableChanged(ItemEvent e) {
		updateUI(historyDao.readOrDefault(), null);
	}

	public void templateSelectChanged(ItemEvent e) {
		final String name = Objects.toString(e.getItem());
		if (name.isBlank()) {
			transientTemplateChanged(e);
		} else if (ItemEvent.SELECTED == e.getStateChange()) {
			updateTemplateListTooltip(name);
			final SettingHistory history = historyDao.readOrDefault();
			final List<SettingTemplate> templates = getProjectTemplates(history);
			final Optional<SettingTemplate> template = getTemplateByName(templates, name);
			template.ifPresent(this::updateUIOptions);
			template.ifPresent(this::updateUIExportJar);
			template.ifPresent(this::updateUISelectFiles);
		}
	}

	public void exportJarChanged(ItemEvent e) {
		if (ItemEvent.SELECTED == e.getStateChange()) {
			final String name = Objects.toString(e.getItem());
			settingDialog.outPutJarFileComboBox.setToolTipText(name);
		}
	}

	private void updateUI(SettingHistory history, String curTemplate) {
		final boolean templateEnable = updateUIState();
		if (templateEnable) {
			saveTransientTemplate();
			updateUIDataEnableTemplate(history, curTemplate);
		} else {
			updateUIDataDisableTemplate(history);
		}
	}

	private boolean updateUIState() {
		final boolean templateEnable = settingDialog.templateEnableCheckBox.isSelected();
		settingDialog.templateSelectComBox.setEnabled(templateEnable);
		settingDialog.templateSaveButton.setEnabled(templateEnable);
		settingDialog.templateDelButton.setEnabled(templateEnable);
		return templateEnable;
	}

	private void updateTemplateListTooltip(String tooltip) {
		if (tooltip != null && !tooltip.isBlank()) {
			settingDialog.templateSelectComBox.setToolTipText(tooltip);
		} else {
			settingDialog.templateSelectComBox.setToolTipText(toolTipEmptyTemplate);
		}
	}

	private void updateUIDataEnableTemplate(SettingHistory history, String curTemplate) {
		final Object item = settingDialog.templateSelectComBox.getSelectedItem();
		if (item == null || item.toString().isBlank()) {
			updateUITemplateList(history, curTemplate);
		}
		final String itemName = settingDialog.templateSelectComBox.getSelectedItem().toString();
		updateTemplateListTooltip(itemName);
		final Optional<SettingTemplate> template = getTemplateByName(getProjectTemplates(history), itemName);
		//TODO combine to one method
		template.ifPresent(this::updateUIOptions);
		template.ifPresent(this::updateUIExportJar);
		template.ifPresent(this::updateUISelectFiles);
		// TODO update select files panel, setModel fire the change event, use listener to update select files
	}

	private void updateUITemplateList(SettingHistory history, String selectedTemplateName) {
		final var selectedTemplate = new SettingTemplate();
		selectedTemplate.setName(selectedTemplateName == null ? templateDefaultName[0] : selectedTemplateName);
		final var curTemplateList = new ArrayList<SettingTemplate>();
		curTemplateList.add(selectedTemplate);
		final List<SettingTemplate> savedTemplateList = getProjectTemplates(history);
		if (savedTemplateList != null && savedTemplateList.size() > 0) {
			curTemplateList.addAll(savedTemplateList);
		}
		updateUITemplateList(curTemplateList);
	}

	private void updateUITemplateList(SettingHistory history) {
		final List<SettingTemplate> templateList = getProjectTemplates(history);
		if (templateList != null && templateList.size() > 0) {
			updateUITemplateList(templateList);
		} else {
			final ComboBoxModel<String> model = new DefaultComboBoxModel<>(templateDefaultName);
			settingDialog.templateSelectComBox.setModel(model);
		}
	}

	private List<SettingTemplate> getProjectTemplates(SettingHistory history) {
		return Optional.ofNullable(history.getProjects()).orElse(Map.of()).get(settingDialog.project.getName());
	}

	private Optional<SettingTemplate> getTemplateByName(List<SettingTemplate> templates, String name) {
		return templates != null ? templates.stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst() :
				Optional.empty();
	}

	private void updateUITemplateList(final List<SettingTemplate> templateList) {
		final String[] names = templateList.stream().map(SettingTemplate::getName).toArray(String[]::new);
		final ComboBoxModel<String> model = new DefaultComboBoxModel<>(names);
		settingDialog.templateSelectComBox.setModel(model);
	}

	private void updateUIOptions(SettingTemplate template) {
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

	private void updateUIExportJar(SettingTemplate template) {
		String[] historyFiles;
		if (template != null) {
			historyFiles =
					Arrays.stream(Optional.ofNullable(template.getExportJar()).orElse(new ExportJarInfo[0]))
							.map(ExportJarInfo::getPath).toArray(String[]::new);
		} else {
			historyFiles = new String[0];
		}
		ComboBoxModel<String> model = new DefaultComboBoxModel<>(historyFiles);
		settingDialog.outPutJarFileComboBox.setModel(model);
		if (historyFiles.length > 0) {
			settingDialog.outPutJarFileComboBox.setToolTipText(historyFiles[0]);
		} else {
			settingDialog.outPutJarFileComboBox.setToolTipText("");
		}
	}

	private void updateUISelectFiles(SettingTemplate template) {
		final VirtualFile[] virtualFiles = readStoredSelectFiles(template);
		this.settingDialog.setSelectedFiles(virtualFiles);
	}

	private void updateUIDataDisableTemplate(SettingHistory history) {
		if (this.transientTemplateSelectFiles != null) {
			updateUIByTransientTemplate();
		} else {
			updateUIOptions(history.getGlobal());
			updateUIExportJar(history.getGlobal());
		}
	}

	public void saveTemplate(ActionEvent e) {
		final Object name = settingDialog.templateSelectComBox.getSelectedItem();
		if (name == null || name.toString().isBlank()) {
			showErrorDialog(messageTemplateNameNull, titleTemplateMessageDialog);
		} else {
			final SettingHistory savedHistory = saveCurTemplate();
			if (savedHistory != null) {
				updateUITemplateList(savedHistory);
				saveGlobalTemplate();
				showInfoMessage(String.format(messageTemplateSaveSuccess, name), titleTemplateMessageDialog);
			}
		}
	}

	public SettingHistory saveCurTemplate() {
		final boolean templateEnable = settingDialog.templateEnableCheckBox.isSelected();
		if (!templateEnable) {
			return null;
		}
		final Object name = settingDialog.templateSelectComBox.getSelectedItem();
		if (name != null && !name.toString().isBlank()) {
			final SettingTemplate curTemplate = new SettingTemplate();
			final String templateName = name.toString();
			final long ts = System.currentTimeMillis();
			curTemplate.setCreateTime(ts);
			curTemplate.setUpdateTime(ts);
			curTemplate.setName(templateName);
			curTemplate.setExportJar(buildExportJarArray());
			curTemplate.setOptions(settingDialog.pickExportOptions());
			final Path selectStore = storeSelectFiles(templateName);
			curTemplate.setSelectFilesStore(selectStore.toString());
			return historyDao.saveProject(settingDialog.project.getName(), curTemplate);
		}
		return null;
	}

	public void saveGlobalTemplate() {
		final SettingTemplate globalTemplate = new SettingTemplate();
		final long ts = System.currentTimeMillis();
		globalTemplate.setCreateTime(ts);
		globalTemplate.setUpdateTime(ts);
		globalTemplate.setName(templateGlobalName);
		globalTemplate.setExportJar(buildExportJarArray());
		globalTemplate.setOptions(settingDialog.pickExportOptions());
		this.historyDao.saveGlobal(globalTemplate);
	}

	private ExportJarInfo[] buildExportJarArray() {
		final Object jarFile = settingDialog.outPutJarFileComboBox.getSelectedItem();
		if (jarFile == null || jarFile.toString().isBlank()) {
			return new ExportJarInfo[0];
		} else {
			final ExportJarInfo jarInfo = new ExportJarInfo();
			jarInfo.setPath(jarFile.toString());
			jarInfo.setCreateTime(System.currentTimeMillis());
			return new ExportJarInfo[]{jarInfo};
		}
	}

	private Path storeSelectFiles(final String templateName) {
		return this.historyDao.saveSelectFiles(settingDialog.project.getName(), templateName,
				settingDialog.getSelectedFiles());
	}

	private VirtualFile[] readStoredSelectFiles(final SettingTemplate template) {
		final var templateName = template.getName();
		final var projectName = settingDialog.project.getName();
		final var fileMap = historyDao.readStoredSelectFiles(projectName, templateName);
		final var fs = new ArrayList<VirtualFile>();
		for (Map.Entry<String, VirtualFile> fileEntry : fileMap.entrySet()) {
			if (fileEntry.getValue() != null) {
				fs.add(fileEntry.getValue());
			} else {
				warn(settingDialog.project, fileEntry.getKey() +
						String.format(messageTemplateFileNotFound, templateName));
			}
		}
		return fs.toArray(new VirtualFile[0]);
	}

	private void transientTemplateChanged(ItemEvent e) {
		if (ItemEvent.DESELECTED == e.getStateChange()) {
			saveTransientTemplate();
		} else {
			updateUIByTransientTemplate();
		}
	}

	private void updateUIByTransientTemplate() {
		if (this.transientTemplateSelectFiles != null) {
			updateUIOptions(this.transientTemplate);
			updateUIExportJar(this.transientTemplate);
			this.settingDialog.setSelectedFiles(this.transientTemplateSelectFiles);
		}
	}

	private void saveTransientTemplate() {
		this.transientTemplateSelectFiles = settingDialog.getSelectedFiles();
		this.transientTemplate.setOptions(settingDialog.pickExportOptions());
		this.transientTemplate.setExportJar(buildExportJarArray());
	}

	public void delTemplate(ActionEvent e) {
		final String template = String.valueOf(settingDialog.templateSelectComBox.getSelectedItem());
		final SettingHistory savedHistory = historyDao.removeTemplate(settingDialog.project.getName(), template);
		if (savedHistory != null) {
			updateUITemplateList(savedHistory);
			showInfoMessage(String.format(messageTemplateDelSuccess, template), titleTemplateMessageDialog);
		} else {
			errorNotify(titleTemplateMessageDialog, String.format(messageTemplateDelError, template));
		}
	}

}

package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vfs.VirtualFile;
import org.yanhuang.plugins.intellij.exportjar.model.*;
import org.yanhuang.plugins.intellij.exportjar.settings.HistoryDao;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.openapi.ui.Messages.*;
import static org.yanhuang.plugins.intellij.exportjar.ui.FileListTreeHandler.updateUIFileListTreeGrouping;
import static org.yanhuang.plugins.intellij.exportjar.utils.Constants.*;
import static org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils.errorNotify;

/**
 * handle template related events
 */
public class TemplateEventHandler {
	private final HistoryDao historyDao = new HistoryDao();
	private final SettingDialog settingDialog;
	/**
	 * Transient template for storing no-template-name settings
	 */
	private SettingSelectFile[] transientTemplateSelectFiles;
	private final SettingTemplate transientTemplate = new SettingTemplate();

	public TemplateEventHandler(final SettingDialog settingDialog) {
		this.settingDialog = settingDialog;
		// do not save transient template in constructor, will lead logic error!!
	}

	/**
	 * init ui (template related)
	 *
	 * @param history     setting history saved
	 * @param curTemplate current using template name (null: start from "export jar" action, not null: start from "
	 *                    export jar by template" action)
	 */
	public void initUI(SettingHistory history, String curTemplate) {
		if (curTemplate != null && !curTemplate.isBlank()) {
			settingDialog.templateEnableCheckBox.setSelected(true);
		}
		updateUI(history, curTemplate);
	}

	/**
	 * enable/disable template event process
	 *
	 * @param e change event
	 */
	public void templateEnableChanged(ItemEvent e) {
		updateUI(historyDao.readOrDefault(), null);
	}

	/**
	 * template select change event process,
	 *
	 * @param e event
	 */
	public void templateSelectChanged(ItemEvent e) {
		final String name = Objects.toString(e.getItem());
		if (name.isBlank()) {
			transientTemplateChanged(e);
		} else if (ItemEvent.SELECTED == e.getStateChange()) {
			updateTemplateListTooltip(name);
			final SettingHistory history = historyDao.readOrDefault();
			updateUIBySelectTemplate(history);
		}
	}

	private void updateUIBySelectTemplate(SettingHistory history) {
		final var selectTemplate = String.valueOf(settingDialog.templateSelectComBox.getSelectedItem());
		final var templates = historyDao.getProjectTemplates(history, settingDialog.project.getName());
		final var template = getTemplateByName(templates, selectTemplate);
		template.ifPresent(this::updateUIOptions);
		template.ifPresent(this::updateUIExportJar);
		template.ifPresent(this::updateUISelectFiles);
		template.ifPresent(this::updateUIFileListTree);
	}

	/**
	 * export jar change event handle
	 *
	 * @param e event
	 */
	public void exportJarChanged(ItemEvent e) {
		if (ItemEvent.SELECTED == e.getStateChange()) {
			final String name = Objects.toString(e.getItem());
			settingDialog.outPutJarFileComboBox.setToolTipText(name);
		}
	}

	private void updateUI(SettingHistory history, String curTemplate) {
		final boolean templateEnable = updateUIState();
		if (templateEnable) {
			if (curTemplate == null || curTemplate.isBlank()) {
				// save settings to transient fields status change from disable to enable
				saveTransientTemplate();
			}
			updateUIDataEnableTemplate(history, curTemplate);
		} else {
			// when disable template will, restore global setting (last save transient or last save global)
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
		updateUIBySelectTemplate(history);
	}

	private void updateUITemplateList(SettingHistory history, String selectedTemplateName) {
		final var curTemplateList = new ArrayList<SettingTemplate>();
		final var savedTemplateList = historyDao.getProjectTemplates(history, settingDialog.project.getName());
		final Optional<SettingTemplate> selectedTemplate = getTemplateByName(savedTemplateList, selectedTemplateName);
		if (selectedTemplate.isEmpty()) {// not found name, consider it as new adding
			final var templateNew = new SettingTemplate();
			templateNew.setName(null == selectedTemplateName || selectedTemplateName.isBlank() ?
					templateDefaultName[0] :
					selectedTemplateName);
			curTemplateList.add(templateNew);
		}
		curTemplateList.addAll(savedTemplateList);
		updateUITemplateList(curTemplateList);
		// will not trigger event listener,  because add listener after this method call
		selectedTemplate.ifPresent(t -> settingDialog.templateSelectComBox.setSelectedItem(t.getName()));
	}

	private void updateUITemplateList(SettingHistory history) {
		final var templateList = historyDao.getProjectTemplates(history, settingDialog.project.getName());
		if (!templateList.isEmpty()) {
			updateUITemplateList(templateList);
		} else {
			final ComboBoxModel<String> model = new DefaultComboBoxModel<>(templateDefaultName);
			settingDialog.templateSelectComBox.setModel(model);
		}
	}

	private Optional<SettingTemplate> getTemplateByName(List<SettingTemplate> templates, String name) {
		return templates.stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst();
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
		if (optionSet.isEmpty()) {
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
		final var model = new DefaultComboBoxModel<>(historyFiles);
		settingDialog.outPutJarFileComboBox.setModel(model);
		if (historyFiles.length > 0) {
			settingDialog.outPutJarFileComboBox.setToolTipText(historyFiles[0]);
		} else {
			settingDialog.outPutJarFileComboBox.setToolTipText("");
		}
	}

	private void updateUISelectFiles(SettingTemplate template) {
		final var selectFiles = readStoredSelectFiles(template);
		this.settingDialog.setIncludeExcludeSelections(selectFiles);
	}

	private void updateUIFileListTree(SettingTemplate template) {
		this.settingDialog.fileListDialog.setExpandAllDirectory(template.isFileListTreeExpandDirectory());
		final ChangesTree changesTree = settingDialog.fileListDialog.getFileList();
		if (template.getFileListTreeGroupingKeys() != null) {
			updateUIFileListTreeGrouping(changesTree, template.getFileListTreeGroupingKeys());
		}
		changesTree.rebuildTree();
	}

	private void updateUIDataDisableTemplate(SettingHistory history) {
		final SettingTemplate global = history.getGlobal();
		if (this.transientTemplateSelectFiles != null) {
			updateUIByTransientTemplate();
		} else if (global != null) {
			updateUIOptions(global);
			updateUIExportJar(global);
		}
	}

	/**
	 * save template event process
	 *
	 * @param e save button event
	 */
	public void saveTemplate(ActionEvent e) {
		final Object name = settingDialog.templateSelectComBox.getSelectedItem();
		if (name == null || name.toString().isBlank()) {
			showErrorDialog(messageTemplateNameNull, titleTemplateMessageDialog);
		} else {
			final SettingHistory savedHistory = saveCurTemplate();
			if (savedHistory != null) {
				updateUITemplateList(savedHistory);
				saveGlobalTemplate();
				updateUIBySelectTemplate(savedHistory);
				showInfoMessage(String.format(messageTemplateSaveSuccess, name), titleTemplateMessageDialog);
			}
		}
	}

	/**
	 * save current template to history
	 *
	 * @return not null: success, or else failure
	 */
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
			curTemplate.setFileListTreeExpandDirectory(settingDialog.fileListDialog.isExpandAllDirectory());
			curTemplate.setFileListTreeGroupingKeys(settingDialog.fileListDialog.getGroupingKeys());
			return historyDao.saveProject(settingDialog.project.getName(), curTemplate);
		}
		return null;
	}

	/**
	 * save global template to history
	 */
	public void saveGlobalTemplate() {
		final SettingTemplate globalTemplate = new SettingTemplate();
		final long ts = System.currentTimeMillis();
		globalTemplate.setCreateTime(ts);
		globalTemplate.setUpdateTime(ts);
		globalTemplate.setName(templateGlobalName);
		globalTemplate.setExportJar(buildExportJarArray());
		globalTemplate.setOptions(settingDialog.pickExportOptions());
		this.historyDao.saveGlobal(globalTemplate);
		this.historyDao.saveUISizes(settingDialog.currentUISizes());
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

	/**
	 * store select files to history for template.
	 * consider the include/exclude selection.
	 * if file under certain include or exclude directory, the file will not save to history or else directory will be
	 * saved.
	 * should travel the whole files tree to find include or exclude selections.
	 *
	 * @param templateName template name
	 * @return template history store file, full path
	 */
	private Path storeSelectFiles(final String templateName) {
		return this.historyDao.saveSelectFiles(settingDialog.project.getName(), templateName,
				settingDialog.getIncludeExcludeSelections());
	}

	private SettingSelectFile[] readStoredSelectFiles(final SettingTemplate template) {
		final var templateName = template.getName();
		final var projectName = settingDialog.project.getName();
		final SettingSelectFile[] selectFiles = historyDao.readStoredSelectFiles(projectName, templateName);
		return Arrays.stream(selectFiles).filter(this::filterStoreSelectFile).toArray(SettingSelectFile[]::new);
	}

	private boolean filterStoreSelectFile(SettingSelectFile sf){
		final VirtualFile vf = sf.getVirtualFile();
		// filter out removed files or files not in this project
		return vf != null && settingDialog.fileListDialog.isInModule(vf);
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
			updateUIFileListTree(this.transientTemplate);
			this.settingDialog.setIncludeExcludeSelections(this.transientTemplateSelectFiles);
		}
	}

	private void saveTransientTemplate() {
		this.transientTemplateSelectFiles = settingDialog.fileListDialog.getStoreIncludeExcludeSelections();
		this.transientTemplate.setOptions(settingDialog.pickExportOptions());
		this.transientTemplate.setExportJar(buildExportJarArray());
		this.transientTemplate.setFileListTreeExpandDirectory(settingDialog.fileListDialog.isExpandAllDirectory());
		this.transientTemplate.setFileListTreeGroupingKeys(settingDialog.fileListDialog.getGroupingKeys());
	}

	/**
	 * delete template action process
	 *
	 * @param e delete button event
	 */
	public void delTemplate(ActionEvent e) {
		final String template = String.valueOf(settingDialog.templateSelectComBox.getSelectedItem());
		final Application app = ApplicationManager.getApplication();
		final int[] result = new int[1];
		app.invokeAndWait(() -> result[0] = showYesNoDialog(settingDialog.project,
				String.format(messageTemplateDelYesNo, template), titleTemplateMessageDialog, getWarningIcon()));
		if (result[0] == NO) {
			return;
		}
		final SettingHistory savedHistory = historyDao.removeTemplate(settingDialog.project.getName(), template);
		if (savedHistory != null) {
			updateUITemplateList(savedHistory);
			updateUIBySelectTemplate(savedHistory);
		} else {
			errorNotify(titleTemplateMessageDialog, String.format(messageTemplateDelError, template));
		}
	}

}

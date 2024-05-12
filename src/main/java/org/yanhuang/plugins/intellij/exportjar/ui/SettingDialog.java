package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SeparatorFactory;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.Consumer;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yanhuang.plugins.intellij.exportjar.ExportPacker;
import org.yanhuang.plugins.intellij.exportjar.model.ExportOptions;
import org.yanhuang.plugins.intellij.exportjar.model.SettingHistory;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile;
import org.yanhuang.plugins.intellij.exportjar.model.UISizes;
import org.yanhuang.plugins.intellij.exportjar.settings.HistoryDao;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;
import org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils;
import org.yanhuang.plugins.intellij.exportjar.utils.UpgradeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

import static com.intellij.openapi.ui.Messages.getWarningIcon;
import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.util.ui.JBUI.Panels.simplePanel;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * export jar settings dialog (link to SettingDialog.form)
 * <li><b>In UIDesigner: export options JCheckBox's name is same as HistoryData.ExportOptions</b></li>
 * <li><b>If modal dialog, should extends DialogWrapper, or else throw exception when setVisible(true)</b></li>
 */
public class SettingDialog extends DialogWrapper {
	@Nullable
	protected VirtualFile[] selectedFiles;
	protected final Project project;
	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JCheckBox exportJavaFileCheckBox;
	private JCheckBox exportClassFileCheckBox;
	private JCheckBox exportTestFileCheckBox;
	private JCheckBox exportAddDirectoryCheckBox;
	protected JComboBox<String> outPutJarFileComboBox;
	private JButton selectJarFileButton;
	private JPanel settingPanel;
	protected JPanel fileListPanel;
	private JButton debugButton;
	private JPanel actionPanel;
	protected JPanel optionsPanel;
	private JPanel jarFilePanel;
	private JBSplitter fileListSettingSplitPanel;
	private JPanel templatePanel;
	protected JCheckBox templateEnableCheckBox;
	protected JComboBox<String> templateSelectComBox;
	protected JButton templateSaveButton;
	protected JButton templateDelButton;
	private JPanel templateTitlePanel;
	private JPanel outputJarTitlePanel;
	private JPanel optionTitlePanel;
	protected FileListDialog fileListDialog;
	private BorderLayoutPanel fileListLabel;
	private final HistoryDao historyDao = new HistoryDao();

	private final TemplateEventHandler templateHandler = new TemplateEventHandler(this);

	public SettingDialog(Project project, @Nullable VirtualFile[] selectedFiles, @Nullable String template) {
		super(true);
		MessagesUtils.getMessageView(project);//register message tool window to avoid pack error
		this.project = project;
		this.selectedFiles = selectedFiles;
		this.getPeer().setContentPane(contentPane);
		getRootPane().setDefaultButton(buttonOK);
		this.buttonOK.addActionListener(e -> onOK());
		this.buttonCancel.addActionListener(e -> onCancel());
		this.debugButton.addActionListener(e -> onDebug());

		this.contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		this.selectJarFileButton.addActionListener(this::onSelectJarFileButton);

		migrateSavedHistory();
		historyDao.initV2023();
		createFileListTree();
		updateSettingPanelComponents();
		updateFileListSettingSplitPanel();
//        uiDebug();
		updateComponentState(template);
		updateTemplateUiState();
		this.templateEnableCheckBox.addItemListener(templateHandler::templateEnableChanged);
		this.templateSaveButton.addActionListener(templateHandler::saveTemplate);
		this.templateDelButton.addActionListener(templateHandler::delTemplate);
		this.templateSelectComBox.addItemListener(templateHandler::templateSelectChanged);
		this.outPutJarFileComboBox.addItemListener(templateHandler::exportJarChanged);
	}

	private void updateComponentState(String template) {
		this.setResizable(true);
		final SettingHistory history = historyDao.readOrDefault();
		final Dimension splitPanelSize =
				Optional.ofNullable(history.getUi()).map(UISizes::getFileSettingSplitPanel).orElse(Constants.fileListSettingSplitPanelSize);
		final float splitPanelRatio =
				Optional.ofNullable(history.getUi()).map(UISizes::getFileSettingSplitRatio).orElse(0.5f);
		this.fileListSettingSplitPanel.setPreferredSize(splitPanelSize);
		this.fileListSettingSplitPanel.setProportion(splitPanelRatio);
		final Dimension dialogSize =
				Optional.ofNullable(history.getUi()).map(UISizes::getExportDialog).orElse(Constants.settingDialogSize);
		this.setSize(dialogSize.width, dialogSize.height);
		this.templateHandler.initUI(history, template);
	}

	protected void updateTemplateUiState() {
//		if (category == SettingDialogCategory.LOCAL_CHANGES || category == SettingDialogCategory.LOCAL_CHANGES_REUSE_IDEA) {
//			this.templateEnableCheckBox.setEnabled(false);
//		}
	}

	public JBSplitter getFileListSettingSplitPanel() {
		return fileListSettingSplitPanel;
	}

	protected void createFileListTree() {
		//remove old component
		if (this.fileListLabel != null) {
			fileListPanel.remove(fileListLabel);
		}
		if (this.fileListDialog != null) {
			fileListPanel.remove(this.fileListDialog.getCenterPanel());
			disposeFileListDialog();
		}
		//create new components
		this.fileListDialog = createFilesDialog();
		final JComponent selectTreePanel = fileListDialog.getCenterPanel();
		final JPanel separatorPanel = UIFactory.createTitledSeparatorPanel(Constants.titleFileList, selectTreePanel);
		this.fileListLabel = simplePanel(separatorPanel).withBorder(createEmptyBorder());
		this.fileListPanel.add(fileListLabel, BorderLayout.NORTH);
		this.fileListPanel.add(selectTreePanel, BorderLayout.CENTER);
	}

	@NotNull
	private FileListDialog createFilesDialog() {
		final Set<VirtualFile> allVfs = new HashSet<>();
		for (VirtualFile virtualFile : Optional.ofNullable(this.selectedFiles).orElse(new VirtualFile[0])) {
			CommonUtils.collectExportFilesNest(project, allVfs, virtualFile);
		}
		final FileListDialog dialog = new FileListDialog(this.project, List.copyOf(allVfs), null, null, true, false);
		dialog.addFileTreeChangeListener((d, e) -> {
			final Collection<VirtualFile> files = d.getSelectedFiles();
			this.buttonOK.setEnabled(null != files && !files.isEmpty());
		});
		return dialog;
	}
	private void updateSettingPanelComponents() {
		createTemplatePanelTitledSeparator();
		createJarOutputPanelTiledSeparator();
		createOptionPanelTitledSeparator();
	}

	private void createTemplatePanelTitledSeparator() {
		createTitledSeparatorForPanel(this.templateTitlePanel, Constants.titleTemplateSetting, this.templatePanel);
	}

	private void createJarOutputPanelTiledSeparator() {
		createTitledSeparatorForPanel(this.outputJarTitlePanel, Constants.titleJarFileSeparator, this.jarFilePanel);
	}

	private void createOptionPanelTitledSeparator() {
		createTitledSeparatorForPanel(this.optionTitlePanel, Constants.titleOptionSeparator, this.optionsPanel);
	}

	private void createTitledSeparatorForPanel(JPanel borderContainer, String title, JComponent separatorForComp) {
		final TitledSeparator separator = SeparatorFactory.createSeparator(title, separatorForComp);
		borderContainer.add(separator, BorderLayout.CENTER);
	}

	private void updateFileListSettingSplitPanel() {
		this.fileListSettingSplitPanel.setFirstComponent(this.fileListPanel);
		this.fileListSettingSplitPanel.setSecondComponent(this.settingPanel);
	}


	private void uiDebug() {
		debugButton.setVisible(true);
		this.fileListDialog.getFileList().addSelectionListener(() -> {
			System.out.println("in ui debug");
		});

	}

	private void migrateSavedHistory() {
		UpgradeManager.migrateHistoryToV2023(this.project);
	}

	public ExportOptions[] pickExportOptions() {
		final Component[] components = optionsPanel.getComponents();
		return Arrays.stream(components).filter(c -> c instanceof JCheckBox)
				.filter(c -> ((JCheckBox) c).isSelected())
				.map(Component::getName)
				.map(String::toLowerCase)
				.filter(n -> {
					try {
						ExportOptions.valueOf(n);
						return true;
					} catch (Exception e) {
						return false;
					}
				}).map(ExportOptions::valueOf).toArray(ExportOptions[]::new);
	}

		private void onSelectJarFileButton(ActionEvent event) {
		FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
		Consumer<VirtualFile> chooserConsumer = new FileChooserConsumerImplForComboBox(this.outPutJarFileComboBox);
		FileChooser.chooseFile(descriptor, project, null, chooserConsumer);
	}


	private void onCancel() {
		this.dispose();
	}

	/**
	 * final selected files to export
	 *
	 * @return selected files, always not null, possible length=0
	 */
	public VirtualFile[] getExportingFiles() {
		return fileListDialog.getSelectedFiles().toArray(new VirtualFile[0]);
	}

	/**
	 * Retrieves the include and exclude selections from the file list dialog and returns an array of
	 * SettingSelectFile objects.
	 */
	public SettingSelectFile[] getIncludeExcludeSelections() {
		return this.fileListDialog.getStoreIncludeExcludeSelections();
	}

	public void onOK() {
		final VirtualFile[] finalSelectFiles = this.getExportingFiles();
		doExport(finalSelectFiles);
	}

	private void onDebug() {
	}

	public void setSelectedFiles(VirtualFile[] selectedFiles) {
		this.selectedFiles = selectedFiles;
		createFileListTree();
	}

	public void setIncludeExcludeSelections(SettingSelectFile[] inExSelectFiles) {
		this.selectedFiles =
				Arrays.stream(inExSelectFiles != null ? inExSelectFiles : new SettingSelectFile[0]).map(SettingSelectFile::getVirtualFile).toArray(VirtualFile[]::new);
		createFileListTree();
		this.fileListDialog.setFlaggedIncludeExcludeSelections(inExSelectFiles);
		this.fileListDialog.getHandler().setShouldUpdateIncludeExclude(true);
	}

	public FileListDialog getFileListDialog() {
		return fileListDialog;
	}

	public void doExport(VirtualFile[] exportFiles) {
		if (isEmpty(exportFiles)) {
			return;
		}
		final Application app = ApplicationManager.getApplication();
		final Module[] modules = CommonUtils.findModule(project, exportFiles);
		String selectedOutputJarFullPath = (String) this.outPutJarFileComboBox.getModel().getSelectedItem();
		if (selectedOutputJarFullPath == null || selectedOutputJarFullPath.trim().isEmpty()) {
			app.invokeAndWait(() -> showErrorDialog(project, "The selected output path should not empty",
					Constants.actionName));
			return;
		}
		Path exportJarFullPath = Paths.get(selectedOutputJarFullPath.trim());
		if (!Files.isDirectory(exportJarFullPath)) {
			Path exportJarParentPath = exportJarFullPath.getParent();
			if (exportJarParentPath == null) {// when input file without parent dir, current dir as parent dir.
				String basePath = project.getBasePath();
				exportJarParentPath = Paths.get(Objects.requireNonNullElse(basePath, "./"));
				exportJarFullPath = exportJarParentPath.resolve(exportJarFullPath);
			}
			if (!Files.exists(exportJarParentPath)) {
				app.invokeAndWait(() -> showErrorDialog(project, "The selected output path is not exists",
						Constants.actionName));
			} else {
				String exportJarName = exportJarFullPath.getFileName().toString();
				if (!exportJarName.endsWith(".jar")) {
					exportJarFullPath = Paths.get(exportJarFullPath + ".jar");
				}
				if (Files.exists(exportJarFullPath)) {
					final int[] result = new int[1];
					final Path finalJarPath = exportJarFullPath;
					app.invokeAndWait(() -> result[0] = Messages.showYesNoDialog(project, finalJarPath + " already " +
									"exists, replace it? ",
							Constants.actionName, getWarningIcon()));
					if (result[0] == Messages.NO) {
						return;
					}
				}
				this.dispose();
				templateHandler.saveCurTemplate();
				templateHandler.saveGlobalTemplate();
				final CompileStatusNotification packager = new ExportPacker(project, exportFiles, exportJarFullPath,
						pickExportOptions());
				app.invokeAndWait(() -> CompilerManager.getInstance(project).make(project, null == modules ?
						new Module[0] : modules, packager));
			}
		} else {
			app.invokeAndWait(() -> showErrorDialog(project, "Please specify export jar file name",
					Constants.actionName));
		}
	}

	private boolean isEmpty(VirtualFile[] exportFiles) {
		if (exportFiles == null || exportFiles.length == 0) {
			ApplicationManager.getApplication().invokeAndWait(() -> showErrorDialog(project, "Export files is empty," +
					" " +
					"please select them first", Constants.actionName));
			return true;
		}
		return false;
	}

	@Override
	protected @Nullable JComponent createCenterPanel() {
		return null;//will use this contentPane replace super contentPane, return null here
	}

	@Override
	public void dispose() {
		disposeFileListDialog();
		super.dispose();
	}

	/**
	 * collection current ui components size
	 *
	 * @return sizes
	 */
	public UISizes currentUISizes() {
		final UISizes sizes = new UISizes();
		sizes.setExportDialog(this.getSize());
		sizes.setFileSettingSplitPanel(this.fileListSettingSplitPanel.getPreferredSize());
		sizes.setFileSettingSplitRatio(this.fileListSettingSplitPanel.getProportion());
		return sizes;
	}

	private void disposeFileListDialog() {
		if (this.fileListDialog == null || this.fileListDialog.isDisposed()) {
			return;
		}
		ApplicationManager.getApplication().invokeLater(() -> this.fileListDialog.disposeIfNeeded());
	}

	private static class FileChooserConsumerImplForComboBox implements Consumer<VirtualFile> {
		private final JComboBox<String> comboBox;

		public FileChooserConsumerImplForComboBox(JComboBox<String> comboBox) {
			this.comboBox = comboBox;
		}

		@Override
		public void consume(VirtualFile virtualFile) {
			String filePath = virtualFile.getPath();
			if (filePath.trim().isEmpty()) {
				return;
			}
			((DefaultComboBoxModel<String>) comboBox.getModel()).insertElementAt(filePath, 0);
			comboBox.setSelectedIndex(0);
		}
	}

}

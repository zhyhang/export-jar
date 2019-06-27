package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.SystemIndependent;
import org.yanhuang.plugins.intellij.exportjar.ExportPacker;
import org.yanhuang.plugins.intellij.exportjar.HistoryData;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;
import org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.intellij.openapi.ui.Messages.*;

/**
 * export jar settings dialog (link to SettingDialog.form)
 */
public class SettingDialog extends JDialog {
	private DataContext dataContext;
	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JCheckBox exportJavaFileCheckBox;
	private JCheckBox exportClassFileCheckBox;
	private JCheckBox exportTestFileCheckBox;
	private JComboBox<String> outPutJarFileComboBox;
	private JButton selectJarFileButton;
	private HistoryData historyData;

	public SettingDialog(DataContext dataContext) {
		this.dataContext = dataContext;
		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonOK);
		this.buttonOK.addActionListener(e -> onOK());
		this.buttonCancel.addActionListener(e -> onCancel());

		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onCancel();
			}
		});
		this.contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), 1);
		this.selectJarFileButton.addActionListener(this::onSelectJarFileButton);

		readSaveHistory();
		initComboBox();
	}

	private void initComboBox() {
		String[] historyFiles;
		if (historyData != null) {
			historyFiles =
					Arrays.stream(Optional.ofNullable(historyData.getSavedJarInfos()).orElse(new HistoryData.SavedJarInfo[0]))
							.map(HistoryData.SavedJarInfo::getPath).toArray(String[]::new);
		} else {
			historyFiles = new String[0];
		}
		List<String> l = new ArrayList<>();
		l.add("a");
		l.toArray(new String[l.size()]);
		ComboBoxModel<String> model = new DefaultComboBoxModel<>(historyFiles);
		outPutJarFileComboBox.setModel(model);
	}

	private void readSaveHistory() {
		if (historyData != null) { // already initialized
			return;
		}
		Project project = CommonDataKeys.PROJECT.getData(this.dataContext);
		Constants.cachePath.toFile().mkdirs();
		if (Files.exists(Constants.historyFilePath)) {
			try {
				String historyJson = new String(Files.readAllBytes(Constants.historyFilePath), StandardCharsets.UTF_8);
				historyData = CommonUtils.fromJson(historyJson, HistoryData.class);
			} catch (Exception e) {
				MessagesUtils.warn(project, e.getMessage());
			}
		}
	}

	private void writeSaveHistory(Path exportJarName) {
		if (historyData == null) {
			this.historyData = new HistoryData();
		}
		HistoryData.SavedJarInfo savedJar = new HistoryData.SavedJarInfo();
		savedJar.setCreation(System.currentTimeMillis());
		savedJar.setPath(exportJarName.toString());
		historyData.addSavedJarInfo(savedJar);
		Project project = CommonDataKeys.PROJECT.getData(dataContext);
		Constants.cachePath.toFile().mkdirs();
		final String json = CommonUtils.toJson(historyData);
		if (json != null) {
			try {
				Files.write(Constants.historyFilePath, json.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				MessagesUtils.warn(project, e.getMessage());
			}
		}
	}

	private void onSelectJarFileButton(ActionEvent event) {
		Project project = CommonDataKeys.PROJECT.getData(this.dataContext);
		FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
		Consumer chooserConsumer = new FileChooserConsumerImplForComboBox(this.outPutJarFileComboBox);
		FileChooser.chooseFile(descriptor, project, null, chooserConsumer);
	}


	private void onCancel() {
		this.dispose();
	}

	private void onOK() {
		Project project = CommonDataKeys.PROJECT.getData(this.dataContext);
		Module[] modules = CommonUtils.findModule(this.dataContext);
		String selectedOutputJarFullPath = (String) this.outPutJarFileComboBox.getModel().getSelectedItem();
		if (selectedOutputJarFullPath == null || selectedOutputJarFullPath.trim().length() == 0) {
			showErrorDialog(project, "the selected output path should not empty", Constants.actionName);
			return;
		}
		Path exportJarFullPath = Paths.get(selectedOutputJarFullPath.trim());
		if (!Files.isDirectory(exportJarFullPath)) {
			Path exportJarParentPath = exportJarFullPath.getParent();
			if (exportJarParentPath == null) {// when input file name without parent dir, using current dir as parent
				// dir.
				@SystemIndependent String basePath = project.getBasePath();
				if (basePath == null) {
					exportJarParentPath = Paths.get("./");
				} else {
					exportJarParentPath = Paths.get(basePath);
				}
				exportJarFullPath = exportJarParentPath.resolve(exportJarFullPath);
			}
			if (!Files.exists(exportJarParentPath)) {
				showErrorDialog(project, "the selected output path is not exists", Constants.actionName);
			} else {
				String exportJarName = exportJarFullPath.getFileName().toString();
				if (!exportJarName.endsWith(".jar")) {
					exportJarFullPath = Paths.get(exportJarFullPath.toString() + ".jar");
				}
				if (Files.exists(exportJarFullPath)) {
					final int answer = showDialog(project, exportJarFullPath + " already exists, replace it? ",
							Constants.actionName, new String[]{YES_BUTTON, NO_BUTTON}, 1, getWarningIcon());
					if (answer == Messages.NO) {
						return;
					}
				}
				this.dispose();
				writeSaveHistory(exportJarFullPath);
				CompileStatusNotification packager = new ExportPacker(this.dataContext, exportJarFullPath,
						exportJavaFileCheckBox.isSelected(), exportClassFileCheckBox.isSelected(),
						exportTestFileCheckBox.isSelected());
				CompilerManager.getInstance(project).make(project, modules, packager);
			}
		} else {
			showErrorDialog(project, "please specify export jar file name", Constants.actionName);
		}
	}

	private static class FileChooserConsumerImplForComboBox implements Consumer<VirtualFile> {
		private JComboBox<String> comboBox;

		public FileChooserConsumerImplForComboBox(JComboBox<String> comboBox) {
			this.comboBox = comboBox;
		}

		@Override
		public void consume(VirtualFile virtualFile) {
			String filePath = virtualFile.getPath();
			if (filePath == null || filePath.trim().length() == 0) {
				return;
			}
			((DefaultComboBoxModel<String>) comboBox.getModel()).insertElementAt(filePath, 0);
			comboBox.setSelectedIndex(0);
		}
	}

}

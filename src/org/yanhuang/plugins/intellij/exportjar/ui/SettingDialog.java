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
import org.yanhuang.plugins.intellij.exportjar.ExportPacker;
import org.yanhuang.plugins.intellij.exportjar.HistoryData;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;
import org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.openapi.ui.Messages.showYesNoDialog;

/**
 * export jar settings dialog (link to SettingDialog.form)
 */
public class SettingDialog extends JDialog {
	private DataContext dataContext;
	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JTextField exportDirectoryField;
	private JButton selectPathButton;
	private JCheckBox exportJavaFileCheckBox;
	private JCheckBox exportClassFileCheckBox;
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
		this.contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(27, 0), 1);
		this.selectPathButton.addActionListener(e -> onSelectPathButtonAction());

		readSaveHistory();

		if (historyData != null && historyData.getSavedJarInfos() != null && historyData.getSavedJarInfos().length > 0) {
			this.exportDirectoryField.setText(historyData.getSavedJarInfos()[0].getPath());
		}
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

	private void onSelectPathButtonAction() {
		Project project = CommonDataKeys.PROJECT.getData(this.dataContext);
		FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
		FileChooserConsumerImpl chooserConsumer = new FileChooserConsumerImpl(this.exportDirectoryField);
		FileChooser.chooseFile(descriptor, project, null, chooserConsumer);
	}

	private void onCancel() {
		this.dispose();
	}

	private void onOK() {
		Project project = CommonDataKeys.PROJECT.getData(this.dataContext);
		Module[] modules = CommonUtils.findModule(this.dataContext);
		Path exportJarFullPath = Paths.get(this.exportDirectoryField.getText().trim());
		if (!Files.isDirectory(exportJarFullPath)) {
			Path exportJarParentPath = exportJarFullPath.getParent();
			if (!Files.exists(exportJarParentPath)) {
				showErrorDialog(project, "the selected output path is not exists", Constants.actionName);
			} else {
				String exportJarName = exportJarFullPath.getFileName().toString();
				if (!exportJarName.endsWith(".jar")) {
					exportJarFullPath = Paths.get(exportJarFullPath.toString() + ".jar");
				}
				if (Files.exists(exportJarFullPath)) {
					final int answer = showYesNoDialog(exportJarFullPath + " already exists, replace it? ",
							Constants.actionName
							, null);
					if (answer == Messages.NO) {
						this.dispose();
						return;
					}
				}
				writeSaveHistory(exportJarFullPath);
				CompileStatusNotification packager = new ExportPacker(this.dataContext, exportJarFullPath,
						exportJavaFileCheckBox.isSelected(), exportClassFileCheckBox.isSelected());
				CompilerManager.getInstance(project).make(project, modules, packager);
				this.dispose();
			}
		} else {
			showErrorDialog(project, "please specify export jar file name", Constants.actionName);
		}
	}

	private class FileChooserConsumerImpl implements Consumer<VirtualFile> {
		private JTextField ouPutDirectoryField;

		public FileChooserConsumerImpl(JTextField jTextField) {
			this.ouPutDirectoryField = jTextField;
		}

		@Override
		public void consume(VirtualFile virtualFile) {
			this.ouPutDirectoryField.setText(virtualFile.getPath());
		}
	}

}

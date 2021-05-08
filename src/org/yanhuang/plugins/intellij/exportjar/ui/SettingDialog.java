package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SeparatorFactory;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.content.MessageView;
import com.intellij.util.Consumer;
import com.intellij.util.WaitForProgressToShow;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yanhuang.plugins.intellij.exportjar.ExportPacker;
import org.yanhuang.plugins.intellij.exportjar.HistoryData;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;
import org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static com.intellij.openapi.ui.Messages.getWarningIcon;
import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.util.ui.JBUI.Panels.simplePanel;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * export jar settings dialog (link to SettingDialog.form)
 */
public class SettingDialog extends JDialog {
    private final Project project;
    @Nullable
    private VirtualFile[] selectedFiles;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox exportJavaFileCheckBox;
    private JCheckBox exportClassFileCheckBox;
    private JCheckBox exportTestFileCheckBox;
    private JComboBox<String> outPutJarFileComboBox;
    private JButton selectJarFileButton;
    private JPanel settingPanel;
    private JPanel fileListPanel;
    private JButton debugButton;
    private JPanel actionPanel;
    private HistoryData historyData;
    private FileListDialog fileListDialog;
    private BorderLayoutPanel fileListLabel;

    public SettingDialog(Project project, @Nullable VirtualFile[] selectedFiles) {
        MessageView.SERVICE.getInstance(project);//register message tool window to avoid pack error
        this.project = project;
        this.selectedFiles = selectedFiles;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        this.buttonOK.addActionListener(e -> onOK());
        this.buttonCancel.addActionListener(e -> onCancel());
        this.debugButton.addActionListener(e -> onDebug());

        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        this.contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        this.selectJarFileButton.addActionListener(this::onSelectJarFileButton);

        readSaveHistory();
        initComboBox();
        createFileListTree();

//        uiDebug();
    }

    /**
     * get setting panel which contains components except ok and cancel buttons.
     *
     * @return
     */
    public JPanel getSettingPanel() {
        return this.settingPanel;
    }

    private void initComboBox() {
        String[] historyFiles;
        if (historyData != null) {
            historyFiles =
                    Arrays.stream(Optional.ofNullable(historyData.getSavedJarInfo()).orElse(new HistoryData.SavedJarInfo[0]))
                            .map(HistoryData.SavedJarInfo::getPath).toArray(String[]::new);
        } else {
            historyFiles = new String[0];
        }
        ComboBoxModel<String> model = new DefaultComboBoxModel<>(historyFiles);
        outPutJarFileComboBox.setModel(model);
    }

    private void createFileListTree() {
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
        final TitledSeparator mySeparator = SeparatorFactory.createSeparator(Constants.titteFileList, selectTreePanel);
        final JPanel separatorPanel = simplePanel().addToBottom(mySeparator).addToTop(Box.createVerticalGlue());
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
        return new FileListDialog(this.project, List.copyOf(allVfs), null, null, true, false);
    }

    private void uiDebug() {
        debugButton.setVisible(true);
    }

    private void readSaveHistory() {
        if (historyData != null) { // already initialized
            return;
        }
        Constants.cachePath.toFile().mkdirs();
        if (Files.exists(Constants.historyFilePath)) {
            try {
                String historyJson = Files.readString(Constants.historyFilePath);
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
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        Consumer<VirtualFile> chooserConsumer = new FileChooserConsumerImplForComboBox(this.outPutJarFileComboBox);
        FileChooser.chooseFile(descriptor, project, null, chooserConsumer);
    }


    private void onCancel() {
        this.dispose();
    }

    public void onOK() {
        final VirtualFile[] finalSelectFiles = fileListDialog.getSelectedFiles().toArray(new VirtualFile[0]);
        doExport(finalSelectFiles);
    }

    private void onDebug() {
        SelectFilesDialog filesDialog = createFilesDialog();
        filesDialog.show();
    }

    public void setSelectedFiles(VirtualFile[] selectedFiles) {
        this.selectedFiles = selectedFiles;
        createFileListTree();
    }

    public void doExport(VirtualFile[] exportFiles) {
        final Module[] modules = CommonUtils.findModule(project, exportFiles);
        String selectedOutputJarFullPath = (String) this.outPutJarFileComboBox.getModel().getSelectedItem();
        if (selectedOutputJarFullPath == null || selectedOutputJarFullPath.trim().length() == 0) {
            WaitForProgressToShow.runOrInvokeAndWaitAboveProgress(() -> showErrorDialog(project, "The selected output path should not empty", Constants.actionName));
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
                WaitForProgressToShow.runOrInvokeAndWaitAboveProgress(() -> showErrorDialog(project, "The selected output path is not exists", Constants.actionName));
            } else {
                String exportJarName = exportJarFullPath.getFileName().toString();
                if (!exportJarName.endsWith(".jar")) {
                    exportJarFullPath = Paths.get(exportJarFullPath.toString() + ".jar");
                }
                if (Files.exists(exportJarFullPath)) {
                    final int[] result = new int[1];
                    final Path finalJarPath = exportJarFullPath;
                    WaitForProgressToShow.runOrInvokeAndWaitAboveProgress(
                            () -> result[0] = Messages.showYesNoDialog(project, finalJarPath + " already exists, replace it? ",
                                    Constants.actionName, getWarningIcon()));
                    if (result[0] == Messages.NO) {
                        return;
                    }
                }
                this.dispose();
                writeSaveHistory(exportJarFullPath);
                final CompileStatusNotification packager = new ExportPacker(project, exportFiles, exportJarFullPath,
                        exportJavaFileCheckBox.isSelected(), exportClassFileCheckBox.isSelected(),
                        exportTestFileCheckBox.isSelected());
                WaitForProgressToShow.runOrInvokeAndWaitAboveProgress(() ->
                        CompilerManager.getInstance(project).make(project, modules, packager));
            }
        } else {
            WaitForProgressToShow.runOrInvokeAndWaitAboveProgress(() -> showErrorDialog(project, "Please specify export jar file name", Constants.actionName));
        }
    }

    @Override
    public void dispose() {
        disposeFileListDialog();
        super.dispose();
    }

    private void disposeFileListDialog() {
        if (this.fileListDialog == null || this.fileListDialog.isDisposed()) {
            return;
        }
        if (EventQueue.isDispatchThread()) {
            this.fileListDialog.disposeIfNeeded();
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> this.fileListDialog.disposeIfNeeded());
            } catch (Exception ignored) {
            }
        }
    }

    private static class FileChooserConsumerImplForComboBox implements Consumer<VirtualFile> {
        private final JComboBox<String> comboBox;

        public FileChooserConsumerImplForComboBox(JComboBox<String> comboBox) {
            this.comboBox = comboBox;
        }

        @Override
        public void consume(VirtualFile virtualFile) {
            String filePath = virtualFile.getPath();
            if (filePath.trim().length() == 0) {
                return;
            }
            ((DefaultComboBoxModel<String>) comboBox.getModel()).insertElementAt(filePath, 0);
            comboBox.setSelectedIndex(0);
        }
    }

}

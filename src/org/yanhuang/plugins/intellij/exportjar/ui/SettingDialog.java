package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.content.MessageView;
import com.intellij.ui.tree.project.ProjectFileTreeModel;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Consumer;
import com.intellij.util.WaitForProgressToShow;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.Nullable;
import org.yanhuang.plugins.intellij.exportjar.ExportPacker;
import org.yanhuang.plugins.intellij.exportjar.HistoryData;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;
import org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils;

import javax.swing.*;
import javax.swing.tree.*;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static com.intellij.openapi.ui.Messages.getWarningIcon;
import static com.intellij.openapi.ui.Messages.showErrorDialog;

/**
 * export jar settings dialog (link to SettingDialog.form)
 */
public class SettingDialog extends JDialog {
    private final Project project;
    @Nullable
    private final VirtualFile[] selectedFiles;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox exportJavaFileCheckBox;
    private JCheckBox exportClassFileCheckBox;
    private JCheckBox exportTestFileCheckBox;
    private JComboBox<String> outPutJarFileComboBox;
    private JButton selectJarFileButton;
    private JPanel settingPanel;
    private JLabel fileListTreeLabel;
    private Tree fileListTree;
    private JTree tree1;
    private CheckboxTree fileListCheckboxTree;
    private JScrollPane fileListPanel;
    private HistoryData historyData;

    public SettingDialog(Project project, @Nullable VirtualFile[] selectedFiles) {
        MessageView.SERVICE.getInstance(project);//register message tool window to avoid pack error
        this.project = project;
        this.selectedFiles = selectedFiles;
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
        initFileListTree();
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

    private void initFileListTree(){
        TreeModel tmDefault = tree1.getModel();
        fileListTree.setModel(tmDefault);
//        TreeModel tmProject = new ProjectFileTreeModel(this.project);
        Container parent = fileListCheckboxTree.getParent();
        parent.remove(fileListCheckboxTree);
        fileListCheckboxTree = new CheckboxTree(new UIFactory.CheckTreeCellRenderer(), (CheckedTreeNode) convertCheckedTreeModel(tmDefault).getRoot());
        parent.add(fileListCheckboxTree);
        fileListCheckboxTree.getEmptyText().setText("Empty item");
        fileListCheckboxTree.setSelectionModel(new DefaultTreeSelectionModel());
        fileListCheckboxTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        TreeUtil.installActions(fileListCheckboxTree);
        TreeUtil.promiseSelectFirst(fileListCheckboxTree);
        fileListCheckboxTree.setRootVisible(true);
        fileListCheckboxTree.setShowsRootHandles(true);
        fileListCheckboxTree.setEnabled(true);
    }

    private TreeModel convertCheckedTreeModel(TreeModel tm) {
        if (tm == null || tm.getRoot()==null || tm.getRoot() instanceof CheckedTreeNode) {
            return tm;
        }
        DefaultMutableTreeNode originParent= (DefaultMutableTreeNode) tm.getRoot();
        CheckedTreeNode newParent = new CheckedTreeNode(originParent.getUserObject());
        newParent.setChecked(false);
        convertCheckedTreeNodes(originParent, newParent);
        return new DefaultTreeModel(newParent);
    }

    private void convertCheckedTreeNodes(DefaultMutableTreeNode originParent, CheckedTreeNode newParent) {
        if (originParent.getChildCount() == 0) {
            return;
        }
        for (int i = 0; i < originParent.getChildCount(); i++) {
            DefaultMutableTreeNode originChild = (DefaultMutableTreeNode) originParent.getChildAt(i);
            CheckedTreeNode newChild = new CheckedTreeNode(originChild.getUserObject());
            newChild.setChecked(false);
            newParent.add(newChild);
            convertCheckedTreeNodes(originChild, newChild);
        }
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

    private void onOK() {
        doExport(this.selectedFiles);
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

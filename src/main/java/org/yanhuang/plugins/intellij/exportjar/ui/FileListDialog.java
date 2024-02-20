package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.JBTreeTraverser;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile.SelectType;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Dialog extend from SelectFilesDialog without the buttons. It is used to show select files.
 */
public class FileListDialog extends SelectFilesDialog {

    protected final static Key<Boolean> KEY_RECURSIVE_SELECT_DIRECTORY= Key.create("flag_recursive_select_directory");
    protected final static Key<SelectType> KEY_TYPE_SELECT_FILE_DIRECTORY= Key.create("type_select_file_directory");

    private JComponent centerPanel;

    private boolean recursiveActionSelected;
    private boolean ignoreIncludeChanged;
    private final FileListTreeHandler handler;
    private transient SettingSelectFile[] savedIncludeExcludeSelections;

    public FileListDialog(Project project, @NotNull List<? extends VirtualFile> files, @Nullable String prompt, @Nullable VcsShowConfirmationOption confirmationOption, boolean selectableFiles, boolean deletableFiles) {
        super(project, files, prompt, confirmationOption, selectableFiles, deletableFiles);
        init();
        final ChangesTree filesTree = getFileList();
        // listener for restore the include/exclude select state because of mouse/space key select changed
        this.handler = new FileListTreeHandler(this);
        filesTree.setInclusionListener(handler::updateIncludeExcludeByPutFlag);
        filesTree.addSelectionListener(handler::updateIncludeExcludeBySettingFiles);
        filesTree.addGroupingChangeListener(handler::groupByChanged);
        filesTree.setCellRenderer(new FileListTreeCellRender(filesTree.getCellRenderer()));
    }

    @Override
    protected @Nullable
    JComponent createCenterPanel() {
        this.centerPanel = super.createCenterPanel();
        return this.centerPanel;
    }


    public JComponent getCenterPanel() {
        return centerPanel;
    }

    public void addFileTreeChangeListener(BiConsumer<FileListDialog, PropertyChangeEvent> listener) {
        getFileList().addPropertyChangeListener(e-> listener.accept(this,e));
    }

    @Override
    public @NotNull ChangesTree getFileList() {
        return super.getFileList();
    }

    /**
     * Retrieves the include and exclude selections from the file list dialog and returns an array of SettingSelectFile objects.
     * <li>general to use for template export setting files saving history</li>
     * <li>if get final files (only files) to export, use getSelectedFiles() method</li>
     *
     * @return An array of SettingSelectFile objects representing the include and exclude selections (files and dirs).
     *         The array contains the selected files with their respective select type (include or exclude),
     *         file path, and recursive flag.
     */
    public SettingSelectFile[] getIncludeExcludeSelections() {
        final VirtualFile[] svfFiles = getSelectedFiles().toArray(new VirtualFile[0]);
        final var vfSettingMap = createVfSettingMap();
        final List<SettingSelectFile> finalSelectFiles = createFinalSelectFiles(svfFiles, vfSettingMap);
        return finalSelectFiles.toArray(new SettingSelectFile[0]);
    }

    private Map<VirtualFile, SettingSelectFile> createVfSettingMap() {
        final Map<VirtualFile, SettingSelectFile> vfSettingMap = new HashMap<>();
        final JBTreeTraverser<TreeNode> treeNodes = TreeUtil.treeNodeTraverser(getFileList().getRoot());
        for (TreeNode treeNode : treeNodes) {
            processTreeNode((ChangesBrowserNode<?>) treeNode, vfSettingMap);
        }
        return vfSettingMap;
    }

    private void processTreeNode(ChangesBrowserNode<?> changeNode, Map<VirtualFile, SettingSelectFile> vfSettingMap) {
        final SettingSelectFile.SelectType selectType = changeNode.getUserData(FileListDialog.KEY_TYPE_SELECT_FILE_DIRECTORY);
        if (selectType == SettingSelectFile.SelectType.include || selectType == SettingSelectFile.SelectType.exclude) {
            final var isRecursive = Boolean.TRUE.equals(changeNode.getUserData(FileListDialog.KEY_RECURSIVE_SELECT_DIRECTORY));
            final var vf = FileListTreeHandler.getNodeBindVirtualFile(changeNode);
            if(vf!=null) {
                final SettingSelectFile selectFile = new SettingSelectFile();
                selectFile.setSelectType(selectType);
                selectFile.setFilePath(CommonUtils.toOsFile(vf).toString());
                selectFile.setRecursive(isRecursive);
                vfSettingMap.put(vf, selectFile);
            }
        }
    }

    private List<SettingSelectFile> createFinalSelectFiles(VirtualFile[] svfFiles, Map<VirtualFile, SettingSelectFile> vfSettingMap) {
        final List<SettingSelectFile> finalSelectFiles = new ArrayList<>();
        for (VirtualFile svfFile : svfFiles) {
            final SettingSelectFile selectFile = vfSettingMap.get(svfFile);
            if (selectFile == null) {
                final SettingSelectFile selectFileNew = new SettingSelectFile();
                selectFileNew.setFilePath(CommonUtils.toOsFile(svfFile).toString());
                finalSelectFiles.add(selectFileNew);
            }
        }
        final boolean ignored = finalSelectFiles.addAll(vfSettingMap.values());
        return finalSelectFiles;
    }

    @Override
    protected @NotNull DefaultActionGroup createToolbarActions() {
        final DefaultActionGroup group = super.createToolbarActions();
        group.addAll(FileListActions.treeOperationActions(this));
        return group;
    }

    public boolean isRecursiveActionSelected() {
        return recursiveActionSelected;
    }

    public void setRecursiveActionSelected(boolean recursiveActionSelected) {
        this.recursiveActionSelected = recursiveActionSelected;
    }

    public boolean isIgnoreIncludeChanged() {
        return ignoreIncludeChanged;
    }

    public void setIgnoreIncludeChanged(boolean ignoreIncludeChanged) {
        this.ignoreIncludeChanged = ignoreIncludeChanged;
    }

    public FileListTreeHandler getHandler() {
        return handler;
    }

    public SettingSelectFile[] getSavedIncludeExcludeSelections() {
        return savedIncludeExcludeSelections;
    }

    public void setSavedIncludeExcludeSelections(SettingSelectFile[] savedIncludeExcludeSelections) {
        this.savedIncludeExcludeSelections = savedIncludeExcludeSelections;
    }

}

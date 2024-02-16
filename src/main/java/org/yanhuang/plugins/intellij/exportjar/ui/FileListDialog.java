package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Dialog extend from SelectFilesDialog without the buttons. It is used to show select files.
 */
public class FileListDialog extends SelectFilesDialog {

    protected final static Key<Boolean> KEY_RECURSIVE_SELECT_DIRECTORY= Key.create("flag_recursive_select_directory");
    protected final static Key<IncludeExcludeType> KEY_TYPE_SELECT_FILE_DIRECTORY= Key.create("type_select_file_directory");
    public enum IncludeExcludeType {
        include, exclude,noop
    }

    private JComponent centerPanel;

    private boolean recursiveActionSelected;
    private boolean ignoreIncludeChanged;
    private final FileListTreeListener listener;

    public FileListDialog(Project project, @NotNull List<? extends VirtualFile> files, @Nullable String prompt, @Nullable VcsShowConfirmationOption confirmationOption, boolean selectableFiles, boolean deletableFiles) {
        super(project, files, prompt, confirmationOption, selectableFiles, deletableFiles);
        init();
        final ChangesTree filesTree = getFileList();
        // listener for restore the include/exclude select state because of mouse/space key select changed
        this.listener = new FileListTreeListener(this);
        filesTree.setInclusionListener(listener::updateByIncludeExclude);
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

    public FileListTreeListener getListener() {
        return listener;
    }

}

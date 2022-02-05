package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
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

    private JComponent centerPanel;

    public FileListDialog(Project project, @NotNull List<? extends VirtualFile> files, @Nullable String prompt, @Nullable VcsShowConfirmationOption confirmationOption, boolean selectableFiles, boolean deletableFiles) {
        super(project, files, prompt, confirmationOption, selectableFiles, deletableFiles);
        init();
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
}

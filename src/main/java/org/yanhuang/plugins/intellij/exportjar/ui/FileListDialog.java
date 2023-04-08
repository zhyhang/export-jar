package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
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

    private JComponent centerPanel;

    public FileListDialog(Project project, @NotNull List<? extends VirtualFile> files, @Nullable String prompt, @Nullable VcsShowConfirmationOption confirmationOption, boolean selectableFiles, boolean deletableFiles) {
        super(project, files, prompt, confirmationOption, selectableFiles, deletableFiles);
//        FileListTree tree = new FileListTree(project, selectableFiles, deletableFiles, files);
//        ReflectionUtil.setField(SelectFilesDialog.class, this, VirtualFileList.class, "myFileList", tree);
        init();
        final ChangesTree filesTree = getFileList();
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
        group.add(new AnAction("Recursive Selection","", AllIcons.Actions.ListChanges) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Messages.showInfoMessage("It's coming","Hello");
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
            }

        });
        group.add(new AnAction("Cancel Recursive Selection","", AllIcons.Actions.ListFiles) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Messages.showInfoMessage("It's coming","Hello");
            }

            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setEnabled(false);
            }

        });
        return group;

    }
}

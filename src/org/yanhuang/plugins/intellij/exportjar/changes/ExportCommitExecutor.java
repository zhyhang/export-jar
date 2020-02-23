package org.yanhuang.plugins.intellij.exportjar.changes;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CommitSession;
import com.intellij.openapi.vcs.changes.LocalCommitExecutor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;

/**
 * view of displaying local changes for export jar
 */
public class ExportCommitExecutor extends LocalCommitExecutor implements ProjectComponent {

    private Project project;

    public ExportCommitExecutor(Project project) {
        this.project = project;
    }

    public static ExportCommitExecutor getInstance(Project project) {
        return project.getComponent(ExportCommitExecutor.class);
    }

    @Nullable
    @Override
    public String getHelpId() {
        return null; //TODO help doc
    }

    @Nls
    @NotNull
    @Override
    public String getActionText() {
        return Constants.exportCommitButtonName;
    }

    @NotNull
    @Override
    public CommitSession createCommitSession() {
        return new ExportCommitSession(project);
    }

    @Override
    public void projectOpened() {
        ChangeListManager manager = project.getComponent(ChangeListManager.class);
        manager.registerCommitExecutor(this);
    }

}

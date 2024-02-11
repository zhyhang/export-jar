package org.yanhuang.plugins.intellij.exportjar.changes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.changes.CommitSession;
import com.intellij.openapi.vcs.changes.LocalCommitExecutor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;

/**
 * view of displaying local changes for export jar
 */
public class ExportCommitExecutor extends LocalCommitExecutor {

    private final Project project;

    public ExportCommitExecutor(Project project) {
        this.project = project;
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

    @Override
    public @NotNull
    CommitSession createCommitSession(@NotNull CommitContext commitContext) {
        return new ExportCommitSession(project);
    }

}

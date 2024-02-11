package org.yanhuang.plugins.intellij.exportjar.changes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.changes.actions.AbstractCommitChangesAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * perform export jar from local changes (i.e. vcs commits)
 */
public class ExportCommitAction extends AbstractCommitChangesAction {

	@Nullable
	@Override
	protected CommitExecutor getExecutor(@NotNull Project project) {
		return lookupExtentCommitExecutor(project);
	}

	private CommitExecutor lookupExtentCommitExecutor(@NotNull Project project) {
		final List<CommitExecutor> executors = ChangeListManager.getInstance(project).getRegisteredExecutors();
		for (CommitExecutor executor : executors) {
			if (executor instanceof ExportCommitExecutor) {
				return executor;
			}
		}
		return null;
	}

}

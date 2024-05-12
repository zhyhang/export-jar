package org.yanhuang.plugins.intellij.exportjar.changes;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.ui.LocalChangesSettingDialog;
import org.yanhuang.plugins.intellij.exportjar.ui.SettingDialog;
import org.yanhuang.plugins.intellij.exportjar.ui.UIFactory;
import org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils;

import java.util.List;

/**
 * perform export jar from local changes (i.e. vcs commits)
 */
public class ExportLocalChangesAction extends AnAction {
	private static final Logger LOGGER = Logger.getInstance(ExportLocalChangesAction.class);

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
		if (project != null) {
			final LocalChangesSettingDialog dialog = UIFactory.createLocalChangesSettingDialog(project);
			ApplicationManager.getApplication().invokeAndWait(dialog::show);
		} else {
			final String message = "not found project instance from AnActionEvent dataContext";
			MessagesUtils.errorNotify(ExportLocalChangesAction.class.getSimpleName(), message);
			LOGGER.warn(message);
		}
	}

	private static void simpleVcsExport(Project project) {
		final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
		final List<VirtualFile> affectedFiles = changeListManager.getAffectedFiles();
		final SettingDialog settingDialog = UIFactory.createSettingDialog(project, null, null);
		final ChangesTree changesTree = settingDialog.getFileListDialog().getFileList();
		changesTree.setIncludedChanges(affectedFiles);// trigger select all changes file
		ApplicationManager.getApplication().invokeAndWait(settingDialog::show);
	}

}

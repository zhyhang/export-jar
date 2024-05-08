package org.yanhuang.plugins.intellij.exportjar.changes;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog;
import com.intellij.openapi.vcs.changes.ui.DefaultCommitChangeListDialog;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.vcs.commit.SingleChangeListCommitWorkflow;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yanhuang.plugins.intellij.exportjar.ui.SettingDialog;
import org.yanhuang.plugins.intellij.exportjar.ui.SettingDialogCategory;
import org.yanhuang.plugins.intellij.exportjar.ui.UIFactory;
import org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * perform export jar from local changes (i.e. vcs commits)
 */
public class ExportLocalChangesAction extends AnAction {
	private static final Logger LOGGER = Logger.getInstance(ExportLocalChangesAction.class);

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
		if (project != null) {
			uiDebug(project);
			simpleVcsExport(project);
		} else {
			final String message = "not found project instance from AnActionEvent dataContext";
			MessagesUtils.errorNotify(ExportLocalChangesAction.class.getSimpleName(), message);
			LOGGER.warn(message);
		}
	}

	private static void simpleVcsExport(Project project) {
		final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
		final List<VirtualFile> affectedFiles = changeListManager.getAffectedFiles();
		final SettingDialog settingDialog = UIFactory.createSettingDialog(project, null, null,
				SettingDialogCategory.VCS);
		final ChangesTree changesTree = settingDialog.getFileListDialog().getFileList();
		changesTree.setIncludedChanges(affectedFiles);// trigger select all changes file
		ApplicationManager.getApplication().invokeAndWait(settingDialog::show);
	}

	private void uiDebug(@NotNull Project project) {
		final ChangeListManager changeManager = ChangeListManager.getInstance(project);
		final var pathsToCommit =
				Arrays.stream(ProjectLevelVcsManager.getInstance(project).getAllVersionedRoots()).map(VcsUtil::getFilePath);
		final LocalChangeList defaultChangeList = changeManager.getDefaultChangeList();
		final var changesToCommit =
				pathsToCommit.flatMap(it -> changeManager.getChangesIn(it).stream()).collect(Collectors.toList());
		final Collection<Change> defaultChanges = defaultChangeList.getChanges();
		final Collection<Change> includeChanges = ContainerUtil.intersection(defaultChanges, changesToCommit);
		final var workflow = new SingleChangeListCommitWorkflow(project, Set.of(), defaultChanges, defaultChangeList,
				List.of(), true, null, null);
		final var commitChangeListDialog = new DefaultCommitChangeListDialog(workflow);
		commitChangeListDialog.getInclusionModel().addInclusion(includeChanges);
		final DialogWrapper dialog = new DialogWrapper(false) {
			@Override
			protected @Nullable JComponent createCenterPanel() {
				return null;
			}
		};
		reflectInvokeInit(commitChangeListDialog);
		final BorderLayoutPanel borderLayoutPanel = JBUI.Panels.simplePanel().addToCenter(commitChangeListDialog.getBrowser());
		final JComponent bottomPanel = getChangesBrowserBottomPanel(commitChangeListDialog);
		if (bottomPanel != null) {
			borderLayoutPanel.addToBottom(bottomPanel);
		}
		dialog.getContentPane().add(borderLayoutPanel);
		dialog.setSize(400, 300);
		dialog.show();
	}

	private void reflectInvokeInit(CommitChangeListDialog commitChangeListDialog) {
		try {
			final List<Method> methods = ReflectionUtil.getClassDeclaredMethods(CommitChangeListDialog.class);
			final Method beforeInit = ReflectionUtil.findMethod(methods, "beforeInit");
			if (beforeInit != null) {
				beforeInit.setAccessible(true);
				beforeInit.invoke(commitChangeListDialog);
			} else {
				LOGGER.warn("init CommitChangeListDialog by invoking reflected beforeInit() (which not found)");
			}
		} catch (Exception e) {
			LOGGER.warn("init CommitChangeListDialog by invoking reflected beforeInit() error", e);
		}
	}

	private JComponent getChangesBrowserBottomPanel(CommitChangeListDialog commitChangeListDialog) {
		try {
			final List<Method> methods = ReflectionUtil.getClassDeclaredMethods(CommitChangeListDialog.class);
			final Method getBrowserBottomPanel = ReflectionUtil.findMethod(methods, "getBrowserBottomPanel");
			if (getBrowserBottomPanel != null) {
				getBrowserBottomPanel.setAccessible(true);
				return (JComponent) getBrowserBottomPanel.invoke(commitChangeListDialog);
			} else {
				LOGGER.warn("get browser bottom panel from CommitChangeListDialog by invoking " +
						"reflected getBrowserBottomPanel() (which not found)");
			}
		} catch (Exception e) {
			LOGGER.warn("get browser bottom panel from CommitChangeListDialog by invoking reflect " +
					"getBrowserBottomPanel() error", e);
		}
		return null;
	}

}

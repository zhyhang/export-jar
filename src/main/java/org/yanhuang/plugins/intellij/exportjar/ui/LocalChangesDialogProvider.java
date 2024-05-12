package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog;
import com.intellij.openapi.vcs.changes.ui.DefaultCommitChangeListDialog;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class LocalChangesDialogProvider {
	private static final Logger LOGGER = Logger.getInstance(LocalChangesDialogProvider.class);
	private CommitChangeListDialog borrowedChangesDialog;
	private JComponent borrowedChangesViewPanel;
	public JComponent createChangesViewPanel(@NotNull Project project, VirtualFile[] pathsToExport) {
		dispose();// dispose old ui components if need
		final var changeManager = ChangeListManager.getInstance(project);
		final var pathsToExportStream = pathsToExport == null || pathsToExport.length == 0 ?
				stream(ProjectLevelVcsManager.getInstance(project).getAllVersionedRoots()).map(VcsUtil::getFilePath) :
				stream(pathsToExport).map(VcsUtil::getFilePath);
		final var defaultChangeList = changeManager.getDefaultChangeList();
		final var changesToExport =
				pathsToExportStream.flatMap(it -> changeManager.getChangesIn(it).stream()).collect(Collectors.toList());
		final var defaultChanges = defaultChangeList.getChanges();
		final var includeChanges = ContainerUtil.intersection(defaultChanges, changesToExport);
		final var workflow = new WorkflowHelper().createWorkflow(project, Set.of(), includeChanges, defaultChangeList);
		final var commitChangeListDialog = new DefaultCommitChangeListDialog(workflow);
		this.borrowedChangesDialog = commitChangeListDialog;
		commitChangeListDialog.getBrowser().getViewer().getInclusionModel().addInclusion(includeChanges);
		reflectInvokeInit(commitChangeListDialog); // if reflect error, only ignore the changes view bottom status info
		final var borderLayoutPanel = JBUI.Panels.simplePanel().addToCenter(commitChangeListDialog.getBrowser());
		final var bottomPanel = reflectGetBrowserBottomPanel(commitChangeListDialog);
		if (bottomPanel != null) {//  if reflect error, only ignore the changes view bottom status info
			borderLayoutPanel.addToBottom(bottomPanel);
		}
		this.borrowedChangesViewPanel = bottomPanel;
		return borderLayoutPanel;
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

	private JComponent reflectGetBrowserBottomPanel(CommitChangeListDialog commitChangeListDialog) {
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

	public VirtualFile[] getSelectExportFiles(){
		final List<Change> includedChanges = this.borrowedChangesDialog.getBrowser().getIncludedChanges();
		final List<FilePath> includedUnVersionedFiles = this.borrowedChangesDialog.getBrowser().getIncludedUnversionedFiles();
		final List<VirtualFile> includedChangeFiles = includedChanges.stream().map(Change::getVirtualFile).collect(Collectors.toList());
		final List<VirtualFile> includedUnVersionedVfFiles = includedUnVersionedFiles.stream().map(FilePath::getVirtualFile).collect(Collectors.toList());
		final List<VirtualFile> allFiles = ContainerUtil.concat(includedChangeFiles, includedUnVersionedVfFiles);
		return allFiles.toArray(new VirtualFile[0]);
	}
	public void dispose(){
		if (borrowedChangesViewPanel != null) {
			borrowedChangesViewPanel = null;
		}
		if (borrowedChangesDialog != null) {
			borrowedChangesDialog.disposeIfNeeded();
			borrowedChangesDialog = null;
		}
	}


}

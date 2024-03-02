package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FileListTree extends SelectFilesDialog.VirtualFileList {
	public FileListTree(Project project, boolean selectableFiles, boolean deletableFiles, @NotNull List<?
			extends VirtualFile> files) {
		super(project, selectableFiles, deletableFiles, files);
	}

	@Override
	protected boolean isInclusionEnabled(@NotNull ChangesBrowserNode<?> node) {
//		if (Boolean.TRUE.equals(node.getUserData(FileListDialog.KEY_RECURSIVE_SELECT_DIRECTORY))) {
//			return false;
//		}
//		final ChangesBrowserNode<?> parent = node.getParent();
//		if (parent != null && Boolean.TRUE.equals(parent.getUserData(FileListDialog.KEY_RECURSIVE_SELECT_DIRECTORY))) {
//			return false;
//		}
		return super.isInclusionEnabled(node);
	}
}

package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.ui.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.tree.TreeUtil;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.intellij.openapi.vcs.changes.ui.TreeModelBuilder.buildFromVirtualFiles;

public final class VcsHelper {
	private static final Logger LOGGER = Logger.getInstance(VcsHelper.class);

	/**
	 * create virtual file tree for FileListDialog for making local changes export as same general export file
	 * @param project
	 * @param grouping
	 * @return
	 */
	public static DefaultTreeModel treeModelFromLocalChanges(Project project, ChangesGroupingPolicyFactory grouping) {
		final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
		final List<VirtualFile> affectedFiles = changeListManager.getAffectedFiles();
		final List<LocalChangeList> changeLists = changeListManager.getChangeLists();
		// keep compatible with prior to version 2020.3
		final List<FilePath> unVersionedFilesPaths =
				((ChangeListManagerImpl) changeListManager).getUnversionedFilesPaths();
		final List<VirtualFile> unVersionedFiles =
				unVersionedFilesPaths.stream().map(FilePath::getVirtualFile).collect(Collectors.toList());
		final DefaultTreeModel tempTreeModel = new TreeModelBuilder(project, grouping).setChangeLists(changeLists,
						false, null)
				.setUnversioned(unVersionedFilesPaths).build();
		final List<ChangesBrowserNode<?>> borrowedNodes = findChangeListUnVersionedFilesNode(tempTreeModel);
		final var borrowedChangeListNode = (ChangesBrowserChangeListNode) borrowedNodes.get(0);
		final var borrowedUnVersionedFilesNode = (ChangesBrowserUnversionedFilesNode) borrowedNodes.get(1);
		final DefaultTreeModel changeListModel = buildFromVirtualFiles(project, grouping, affectedFiles);
		final ChangesBrowserNode<?> changeListModelRoot = (ChangesBrowserNode<?>) changeListModel.getRoot();
		final List<TreeNode> changeListNodeList = TreeUtil.nodeChildren(changeListModelRoot).toList();
		changeListModelRoot.removeAllChildren();
		if (!affectedFiles.isEmpty() && borrowedChangeListNode != null) {
			changeListNodeList.forEach(node -> borrowedChangeListNode.add((MutableTreeNode) node));
			changeListModelRoot.add(borrowedChangeListNode);
		} else if (borrowedChangeListNode == null) {
			final String msg = "borrowedChangeListNode is null, something wrong, meanwhile vcs file changed?";
			LOGGER.warn(msg);
		}
		if (!unVersionedFiles.isEmpty() && borrowedUnVersionedFilesNode != null) {
			final DefaultTreeModel unVersionedFilesModel = buildFromVirtualFiles(project, grouping,
					unVersionedFiles);
			final var unVersionedFilesModelRoot = (ChangesBrowserNode<?>) unVersionedFilesModel.getRoot();
			final List<TreeNode> unVersionedFilesNodeList = TreeUtil.nodeChildren(unVersionedFilesModelRoot).toList();
			unVersionedFilesNodeList.forEach(node -> borrowedUnVersionedFilesNode.add((MutableTreeNode) node));
			changeListModelRoot.add(borrowedUnVersionedFilesNode);
		} else if (borrowedUnVersionedFilesNode == null) {
			final String msg = "borrowedUnVersionedFileNode is null, something wrong, meanwhile vcs file changed?";
			LOGGER.warn(msg);
		}
		return changeListModel;
	}

	private static List<ChangesBrowserNode<?>> findChangeListUnVersionedFilesNode(DefaultTreeModel treeModel) {
		final var changeListNode = new AtomicReference<ChangesBrowserChangeListNode>();
		final var unVersionedFilesNode = new AtomicReference<ChangesBrowserUnversionedFilesNode>();
		final ChangesBrowserNode<?> root = (ChangesBrowserNode<?>) treeModel.getRoot();
		TreeUtil.nodeChildren(root).forEach(n -> {
			if (n instanceof ChangesBrowserChangeListNode && changeListNode.get() == null) {
				final ChangesBrowserChangeListNode node = (ChangesBrowserChangeListNode) n;
				node.removeAllChildren();
				changeListNode.set(node);
			} else if (n instanceof ChangesBrowserUnversionedFilesNode && unVersionedFilesNode.get() == null) {
				final ChangesBrowserUnversionedFilesNode node = (ChangesBrowserUnversionedFilesNode) n;
				node.removeAllChildren();
				unVersionedFilesNode.set((ChangesBrowserUnversionedFilesNode) n);
			}
		});
		return List.of(changeListNode.get(), unVersionedFilesNode.get());
	}

}

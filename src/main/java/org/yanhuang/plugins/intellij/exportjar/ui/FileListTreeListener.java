package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.util.containers.JBTreeTraverser;
import com.intellij.util.ui.tree.TreeUtil;
import org.yanhuang.plugins.intellij.exportjar.ui.FileListDialog.IncludeExcludeType;

import javax.swing.tree.TreeNode;
import java.util.concurrent.atomic.AtomicReference;

/**
 * files list tree listener.
 * <li>repair include/exclude selection, when mouse click selection or key space press selection.</li>
 */
public class FileListTreeListener {
	// avoid to infinite recursively invoke updateByIncludeExclude()
	private final AtomicReference<Boolean> updatingIncludeExclude = new AtomicReference<>(Boolean.FALSE);
	private final FileListDialog dialog;
	private final ChangesTree filesTree;

	public FileListTreeListener(FileListDialog dialog) {
		this.dialog = dialog;
		this.filesTree = dialog.getFileList();
	}

	/**
	 * call this method repair include/exclude selection, when mouse click selection or key space press selection.
	 */
	public void updateByIncludeExclude() {
		// FileListActions is modifying changes tree inclusion model, ignore.
		if (dialog.isIgnoreIncludeChanged()) {
			return;
		}
		// self calling and modifying changes tree inclusion model, ignore
		if (!updatingIncludeExclude.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
			return;
		}
		try {
			repairInclusionChanges();
		} finally {
			updatingIncludeExclude.set(Boolean.FALSE);
		}
	}

	private void repairInclusionChanges() {
		final JBTreeTraverser<TreeNode> treeNodes = TreeUtil.treeNodeTraverser(filesTree.getRoot());
		for (TreeNode treeNode : treeNodes) {
			final var changeNode = (ChangesBrowserNode<?>) treeNode;
			final IncludeExcludeType selectType =
					changeNode.getUserData(FileListDialog.KEY_TYPE_SELECT_FILE_DIRECTORY);
			if (selectType == IncludeExcludeType.include || selectType == IncludeExcludeType.exclude) {
				final var isRecursive =
						Boolean.TRUE.equals(changeNode.getUserData(FileListDialog.KEY_RECURSIVE_SELECT_DIRECTORY));
				FileListActions.includeExcludeSubObjects(isRecursive, changeNode, filesTree, selectType);
			}
		}
	}

}

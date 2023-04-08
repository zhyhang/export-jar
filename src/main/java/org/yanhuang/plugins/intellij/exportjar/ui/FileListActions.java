package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import static org.yanhuang.plugins.intellij.exportjar.utils.Constants.*;
import static org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils.infoNotify;

/**
 * file list tree toolbar actions
 */
public class FileListActions {

	public static AnAction[] treeOperationActions(ChangesTree fileTree) {
		return new AnAction[]{new RecursiveDirectorySelectAction(fileTree),
				new CancelRecursiveDirectorySelectAction(fileTree)};
	}

	private static class RecursiveDirectorySelectAction extends AnAction {
		private final ChangesTree fileTree;

		public RecursiveDirectorySelectAction(ChangesTree fileTree) {
			super(actionNameRecursiveSelectByDir, null, AllIcons.Vcs.Folders);
			this.fileTree = fileTree;
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			// Flag
			// do selection recursive
			// notification select directory
			final TreePath[] selectionPaths = fileTree.getSelectionPaths();
			int totalFlag=0;
			for (TreePath selectionPath : (selectionPaths!=null?selectionPaths:new TreePath[0])) {
				final ChangesBrowserNode<?> node  = (ChangesBrowserNode<?>) selectionPath.getLastPathComponent();
				totalFlag+=traverseNodePutSelectFlag(node);
			}
			fileTree.repaint();
			infoNotify(titleFileList, String.format(notifyRecursiveSelectDirectory, totalFlag));
		}

		private int traverseNodePutSelectFlag(final ChangesBrowserNode<?> node) {
			final var nodeIterable = TreeUtil.treeNodeTraverser(node).preOrderDfsTraversal();
			int flagCount=0;
			for (TreeNode treeNode : nodeIterable) {
				final ChangesBrowserNode<?> changeNode = (ChangesBrowserNode<?>) treeNode;
				if (isFolderNode(changeNode)) {
					changeNode.putUserData(FileListDialog.KEY_RECURSIVE_SELECT_DIRECTORY,Boolean.TRUE);
					flagCount++;
				}
			}
			return flagCount;
		}

		private boolean isFolderNode(final ChangesBrowserNode<?> node){
			final Object nodeData = node.getUserObject();
			return !(nodeData instanceof VirtualFile) || ((VirtualFile) nodeData).isDirectory();
		}
	}

	private static class CancelRecursiveDirectorySelectAction extends AnAction {
		private final ChangesTree fileTree;

		public CancelRecursiveDirectorySelectAction(ChangesTree fileTree) {
			super(actionNameCancelRecursiveSelectByDir, null, AllIcons.Actions.ListFiles);
			this.fileTree = fileTree;
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			final TreePath[] selectionPaths = fileTree.getSelectionPaths();
			int totalRemoved=0;
			for (TreePath selectionPath : (selectionPaths!=null?selectionPaths:new TreePath[0])) {
				final ChangesBrowserNode<?> node  = (ChangesBrowserNode<?>) selectionPath.getLastPathComponent();
				totalRemoved+= traverseNodeRemoveSelectFlag(node);
			}
			fileTree.repaint();
			infoNotify(titleFileList, String.format(notifyCancelRecursiveSelectDirectory, totalRemoved));
		}

		private int traverseNodeRemoveSelectFlag(final ChangesBrowserNode<?> node) {
			final var nodeIterable = TreeUtil.treeNodeTraverser(node).preOrderDfsTraversal();
			int removeCount=0;
			for (TreeNode treeNode : nodeIterable) {
				final ChangesBrowserNode<?> changeNode = (ChangesBrowserNode<?>) treeNode;
				if (Boolean.TRUE.equals(changeNode.getUserData(FileListDialog.KEY_RECURSIVE_SELECT_DIRECTORY))) {
					changeNode.putUserData(FileListDialog.KEY_RECURSIVE_SELECT_DIRECTORY, null);
					removeCount++;
				}
			}
			return removeCount;
		}
	}

}

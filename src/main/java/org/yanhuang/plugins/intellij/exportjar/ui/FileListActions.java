package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile.SelectType;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import static com.intellij.util.ui.tree.TreeUtil.treeNodeTraverser;
import static java.util.stream.StreamSupport.stream;
import static org.yanhuang.plugins.intellij.exportjar.utils.Constants.*;

/**
 * file list tree toolbar actions.
 * include/exclude action to perform include select and exclude select type
 * clean include/exclude action to perform clean the previous selection
 * above 3 actions will take recursive toggle action selected state.
 * toggle include or exclude will checkbox state will disable
 */
public class FileListActions {

	public static AnAction[] treeOperationActions(FileListDialog dialog) {
		return new AnAction[]{new RecursiveToggleAction(dialog),
				new IncludeSelectAction(dialog),
				new ExcludeSelectAction(dialog),
				new CleanIncludeExcludeAction(dialog)};
	}

	private static void doIncludeExcludeAction(FileListDialog dialog,ChangesTree fileTree,SelectType selectType) {
		try {
			dialog.setIgnoreIncludeChanged(true);
			dialog.getHandler().updateIncludeExcludeSelectFiles(selectType);
			dialog.getHandler().includeExcludeObjectsBySelectFiles();// TODO only update action changes
		} finally {
			dialog.setIgnoreIncludeChanged(false);
		}
		fileTree.repaint();
	}

	private static void doIncludeExcludeAction(boolean isRecursive,
	                                           ChangesTree fileTree, SelectType selectType,
	                                           FileListDialog dialog) {
		final TreePath[] selectionPaths = fileTree.getSelectionPaths();
		try {
			dialog.setIgnoreIncludeChanged(true);
			for (TreePath selectionPath : (selectionPaths != null ? selectionPaths : new TreePath[0])) {
				final ChangesBrowserNode<?> node = (ChangesBrowserNode<?>) selectionPath.getLastPathComponent();
				putIncludeExcludeSelectFlag(isRecursive, node, fileTree, selectType);
			}
			// save setting select files for updating state sometime
			dialog.setFlagIncludeExcludeSelections(dialog.getStoreIncludeExcludeSelections());
		} finally {
			dialog.setIgnoreIncludeChanged(false);
		}
		fileTree.repaint();
	}

	public static void putIncludeExcludeSelectFlag(boolean isRecursive, final ChangesBrowserNode<?> node,
	                                                ChangesTree fileTree, SelectType selectType) {
//		if (isFolderNode(node)) {
//			node.putUserData(KEY_RECURSIVE_SELECT_DIRECTORY, isRecursive ? Boolean.TRUE : null);
//		}
//		node.putUserData(KEY_TYPE_SELECT_FILE_DIRECTORY, selectType);
		includeExcludeSubObjects(isRecursive, node, fileTree, selectType);
	}

	public static void includeExcludeSubObjects(boolean isRecursive, final ChangesBrowserNode<?> node,
	                                            ChangesTree fileTree, SelectType selectType) {
		if (isRecursive) {
			for (TreeNode subNode : treeNodeTraverser(node).preOrderDfsTraversal()) {
				includeExcludeObject(fileTree, selectType, subNode, true);
			}
		} else {
			for (TreeNode subNode : TreeUtil.nodeChildren(node)) {
				includeExcludeObject(fileTree, selectType, subNode, true);
			}
		}
		includeExcludeObject(fileTree, selectType, node, false);// force update current node
	}

	private static void includeExcludeObject(ChangesTree fileTree, SelectType selectType, TreeNode subNode,
	                                         boolean keepPreviousSelection) {
		final var changeNode = (ChangesBrowserNode<?>) subNode;
		if (isFolderNode(changeNode)) {
			return;
		}
		// if dir or file which already put include/exclude selection flag, don't modify
//		if (keepPreviousSelection && changeNode.getUserData(KEY_TYPE_SELECT_FILE_DIRECTORY) != null) {
//			return;
//		}
		final Object changeObj = changeNode.getUserObject();
		if (selectType == SelectType.include) {
			fileTree.includeChange(changeObj);
		} else if (selectType == SelectType.exclude) {
			fileTree.excludeChange(changeObj);
		}
	}

	public static boolean isFolderNode(final ChangesBrowserNode<?> node) {
		final Object nodeData = node.getUserObject();
		return !(nodeData instanceof VirtualFile) || ((VirtualFile) nodeData).isDirectory();
	}

	private static class IncludeSelectAction extends AnAction {
		private final FileListDialog dialog;
		private final ChangesTree fileTree;

		public IncludeSelectAction(FileListDialog dialog) {
			super(actionNameInclude, null, AllIcons.Actions.Selectall);
			this.dialog = dialog;
			this.fileTree = dialog.getFileList();
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			doIncludeExcludeAction(dialog, fileTree, SelectType.include);
		}

	}

	private static class ExcludeSelectAction extends AnAction {
		private final FileListDialog dialog;
		private final ChangesTree fileTree;

		public ExcludeSelectAction(FileListDialog dialog) {
			super(actionNameExclude, null, AllIcons.Actions.Unselectall);
			this.dialog = dialog;
			this.fileTree = dialog.getFileList();
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			doIncludeExcludeAction(dialog, fileTree, SelectType.exclude);
		}

	}

	private static class RecursiveToggleAction extends ToggleAction {
		private final FileListDialog dialog;

		public RecursiveToggleAction(FileListDialog dialog) {
			super(actionNameUnRecursiveSelection, null, AllIcons.Actions.ListFiles);
			this.dialog = dialog;
		}

		@Override
		public boolean isSelected(@NotNull AnActionEvent e) {
			return dialog.isRecursiveActionSelected();
		}

		@Override
		public void setSelected(@NotNull AnActionEvent e, boolean state) {
			dialog.setRecursiveActionSelected(state);
			Presentation presentation = e.getPresentation();
			if (state) {
				presentation.setIcon(AllIcons.Vcs.Folders);
				presentation.setText(actionNameRecursiveSelection);
			} else {
				presentation.setText(actionNameUnRecursiveSelection);
				presentation.setIcon(AllIcons.Actions.ListFiles);
			}
		}
	}

	private static class CleanIncludeExcludeAction extends AnAction {
		private final FileListDialog dialog;
		private final ChangesTree fileTree;

		public CleanIncludeExcludeAction(FileListDialog dialog) {
			super(actionNameCleanIncludeExclude, null, AllIcons.Actions.Undo);
			this.dialog = dialog;
			this.fileTree = dialog.getFileList();
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			final TreePath[] selectionPaths = fileTree.getSelectionPaths();
			final boolean recursive = dialog.isRecursiveActionSelected();
			for (TreePath selectionPath : (selectionPaths != null ? selectionPaths : new TreePath[0])) {
				final ChangesBrowserNode<?> node = (ChangesBrowserNode<?>) selectionPath.getLastPathComponent();
				cleanSelect(recursive, node);
			}
			fileTree.repaint();
		}

		private void cleanSelect(boolean recursive, ChangesBrowserNode<?> node) {
			if (recursive) {
				stream(treeNodeTraverser(node).preOrderDfsTraversal().spliterator(), false).forEach(this::cleanSelect);
			} else {
				cleanSelect(node);
			}
		}

		private void cleanSelect(final TreeNode node) {
			final var changeNode = (ChangesBrowserNode<?>) node;
			final var virtualFile = FileListTreeHandler.getNodeBindVirtualFile(changeNode);
			this.dialog.removeSavedIncludeExcludeSelection(virtualFile);
		}
	}

}

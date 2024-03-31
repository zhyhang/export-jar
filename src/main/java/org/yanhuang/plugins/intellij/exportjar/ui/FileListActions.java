package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.vcs.changes.actions.SetChangesGroupingAction;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile.SelectType;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

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

	private static void doIncludeExcludeAction(FileListDialog dialog, ChangesTree fileTree, SelectType selectType) {
		try {
			dialog.setIgnoreIncludeChanged(true);
			final var selectNodes = dialog.getHandler().updateIncludeExcludeBySelectNodes(selectType);
			dialog.getHandler().includeExcludeObjectsFrom(selectNodes);
		} finally {
			dialog.setIgnoreIncludeChanged(false);
		}
		fileTree.repaint();
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
			if (recursive) {
				cleanSelectFlagRecursive(selectionPaths == null ? new TreePath[0] : selectionPaths);
			} else {
				cleanSelectFlagNonRecursive(selectionPaths == null ? new TreePath[0] : selectionPaths);
			}
			dialog.getHandler().includeExcludeObjectsAll();
			fileTree.repaint();
		}

		/**
		 * Recursively cleans the select flag for the nodes represented by the provided array of TreePaths.
		 * Nodes are processed in sorted order based on their level (front node deal first), ensuring only
		 * non-descendant nodes of travelled nodes are considered (remove duplication handles).
		 * The method iterates through the filtered nodes and performs a recursive cleaning operation on each.
		 *
		 * @param selectionPaths An array of TreePaths representing nodes to clean the select flag for
		 */
		private void cleanSelectFlagRecursive(final TreePath[] selectionPaths) {
			final var travelledNodes = new ArrayList<ChangesBrowserNode<?>>();
			/*
			  Step-wise explanation:
			  1. Create an empty ArrayList called travelledNodes to store the nodes that have been processed.
			  2. Iterate over each TreePath in the selectionPaths array using Arrays.stream.
			  3. Map each TreePath to a ChangesBrowserNode using the getChangesNode method from the
			  FileListTreeHandler class.
			  4. Sort the nodes based on their level using Comparator.comparingInt.
			  5. Filter out nodes that are descendants of nodes already in the travelledNodes list using the
			  noneMatch method.
			  6. Add the filtered nodes to the travelledNodes list using the filter method.
			  7. For each filtered node, call the recursivelyClean method on it using the forEach method.
			 */
			Arrays.stream(selectionPaths)
					.map(FileListTreeHandler::getChangesNode)
					.sorted(Comparator.comparingInt(DefaultMutableTreeNode::getLevel))
					.filter(n -> travelledNodes.stream().noneMatch(travelledNode -> travelledNode.isNodeDescendant(n)))
					.filter(travelledNodes::add)
					.forEach(this::recursivelyClean);
		}

		private void recursivelyClean(ChangesBrowserNode<?> node) {
			stream(treeNodeTraverser(node).preOrderDfsTraversal().spliterator(), false).forEach(this::cleanSelectFlag);
		}

		private void cleanSelectFlagNonRecursive(final TreePath[] selectionPaths) {
			for (TreePath selectionPath : selectionPaths) {
				final ChangesBrowserNode<?> node = (ChangesBrowserNode<?>) selectionPath.getLastPathComponent();
				cleanSelectFlag(node);
			}
		}

		private void cleanSelectFlag(final TreeNode node) {
			final ChangesBrowserNode<?> changesNode = (ChangesBrowserNode<?>) node;
			final var virtualFile = FileListTreeHandler.getNodeBindVirtualFile(changesNode);
			this.dialog.removeFlaggedIncludeExcludeSelection(virtualFile);
		}
	}

	/**
	 * file list dialog toolbar group by action(group by directory not hide empty package).
	 * register action extension in plugin.xml.
	 * It main function write in FileListTreeGroupPolicyFactory
	 * @see FileListTreeGroupPolicyFactory
	 */
	public static class SetDirectoryAllChangesGroupingAction extends SetChangesGroupingAction {

		@NotNull
		@Override
		public String getGroupingKey() {
			return groupByDirectoryAll;
		}
	}

}

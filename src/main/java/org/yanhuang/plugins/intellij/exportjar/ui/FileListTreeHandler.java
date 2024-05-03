package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.ui.RepositoryChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.tree.TreeUtil;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile.SelectType;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils.toOsFile;

/**
 * files list tree handler.
 * <li>repair include/exclude selection, when mouse click selection or key space press selection.</li>
 */
public class FileListTreeHandler {
	// avoid to infinite recursively invoke updateByIncludeExclude()
	private final AtomicReference<Boolean> updatingIncludeExclude = new AtomicReference<>(Boolean.FALSE);
	private final FileListDialog dialog;
	private final ChangesTree filesTree;
	private transient boolean shouldUpdateIncludeExclude;

	public FileListTreeHandler(FileListDialog dialog) {
		this.dialog = dialog;
		this.filesTree = dialog.getFileList();
	}

	/**
	 * Updates the include/exclude selection for files based on the selected tree paths flag by doing actions.
	 *
	 * @param selectType The type of selection (include/exclude)
	 * @return select tree nodes from select path. not null.
	 */
	public List<ChangesBrowserNode<?>> updateIncludeExcludeBySelectNodes(SelectType selectType) {
		final TreePath[] selectTreePaths = filesTree.getSelectionPaths();
		final List<ChangesBrowserNode<?>> selectNodes = new ArrayList<>();
		for (TreePath selectionPath : (selectTreePaths != null ? selectTreePaths : new TreePath[0])) {
			final var node = getChangesNode(selectionPath);
			final var selectVirtualFile = getNodeBindVirtualFile(node);
			dialog.removeFlaggedIncludeExcludeSelection(selectVirtualFile);
			final var isRecursive =
					null != selectVirtualFile && selectVirtualFile.isDirectory() && dialog.isRecursiveActionSelected();
			final var selectFile = new SettingSelectFile();
			selectFile.setFilePath((null != selectVirtualFile) ? toOsFile(selectVirtualFile).toString() : null);
			selectFile.setSelectType(selectType);
			selectFile.setRecursive(isRecursive);
			dialog.putFlaggedIncludeExcludeSelection(selectVirtualFile, selectFile);
			selectNodes.add(node);
		}
		return selectNodes;
	}

	public static ChangesBrowserNode<?> getChangesNode(TreePath selectionPath) {
		return (ChangesBrowserNode<?>) selectionPath.getLastPathComponent();
	}

	public void includeExcludeObjectsFrom(List<ChangesBrowserNode<?>> selectNodes) {
		selectNodes.sort(Comparator.comparingInt(DefaultMutableTreeNode::getLevel));
		final var travelledNodes = new ArrayList<ChangesBrowserNode<?>>();
		for (ChangesBrowserNode<?> selectNode : selectNodes) {
			if (travelledNodes.stream().anyMatch(travelledNode -> travelledNode.isNodeDescendant(selectNode))) {
				continue;
			}
			includeExcludeObjectsFrom(TreeUtil.treeNodeTraverser(selectNode).preOrderDfsTraversal());
			travelledNodes.add(selectNode);
		}
	}

	public void includeExcludeObjectsAll() {
		final var preOrderRoot = TreeUtil.treeNodeTraverser(filesTree.getRoot()).preOrderDfsTraversal();
		includeExcludeObjectsFrom(preOrderRoot);
	}

	/**
	 * Collects and processes inclusion/exclusion settings for the provided pre-order depth-first traversal nodes.
	 * TimeComplex: O(n)
	 *
	 * @param preOrderDfsNodes The pre-order depth-first traversal nodes to collect settings from
	 */
	private void includeExcludeObjectsFrom(JBIterable<TreeNode> preOrderDfsNodes) {
		final var parentSelectionMap = new HashMap<ChangesBrowserNode<?>, SettingSelectFile>();
		final var includeObjects = new ArrayList<>();
		final var excludeObjects = new ArrayList<>();
		for (TreeNode treeNode : preOrderDfsNodes) {
			final var changeNode = (ChangesBrowserNode<?>) treeNode;
			final var currVirtualFile = getNodeBindVirtualFile(changeNode);
			if (currVirtualFile == null) {
				continue;
			}
			var selectFile = dialog.getFlaggedIncludeExcludeSelection(currVirtualFile);
			if (selectFile == null || isNullSelectType(selectFile)) {
				selectFile = parentSelectionMap.get(changeNode.getParent());
			}
			if (selectFile == null) {
				continue;
			}
			if (currVirtualFile.isDirectory()) {
				parentSelectionMap.put(changeNode, selectFile);
			} else {
				collectIncludeExcludeObjects(selectFile, currVirtualFile, changeNode, includeObjects, excludeObjects);
			}
		}
		this.filesTree.includeChanges(includeObjects);
		this.filesTree.excludeChanges(excludeObjects);
	}

	private void collectIncludeExcludeObjects(SettingSelectFile selectFile, VirtualFile currVirtualFile,
	                                          ChangesBrowserNode<?> changeNode, Collection<Object> includeObjects,
	                                          Collection<Object> excludeObjects) {
		if (selectFile.isRecursive() || currVirtualFile.getParent().equals(selectFile.getVirtualFile())
				|| !selectFile.getVirtualFile().isDirectory()) {
			if (selectFile.getSelectType() == SelectType.include) {
				includeObjects.add(changeNode.getUserObject());
			} else {
				excludeObjects.add(changeNode.getUserObject());
			}
		}
	}

	public static boolean isNullSelectType(SettingSelectFile selectFile) {
		return selectFile.getSelectType() == null || selectFile.getSelectType() == SelectType.noop;
	}

	/**
	 * call this method repair include/exclude selection, when mouse click selection or key space press selection.
	 * <br><b>generally used to inclusion changed listener</b>
	 * </br>Updates the changes tree inclusion model based on include or exclude criteria.
	 * If the dialog is set to ignore include changes or if another thread is already updating the inclusion model,
	 * this method will return without making any updates.
	 */
	public void reIncludeExcludeBySelections() {
		// FileListActions is modifying changes tree inclusion model, ignore.
		if (dialog.isIgnoreIncludeChanged()) {
			return;
		}
		// self calling and modifying changes tree inclusion model, ignore
		if (!updatingIncludeExclude.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
			return;
		}
		try {
			includeExcludeObjectsAll();
		} finally {
			updatingIncludeExclude.set(Boolean.FALSE);
		}
	}

	//general used to tree selection changed listener
	public void updateIncludeExcludeWhenSelectChange() {
		if (!this.isShouldUpdateIncludeExclude() || this.filesTree.getSelectionCount() == 0 || dialog.isFlaggedIncludeExcludeEmpty()) {
			return;
		}
		// possibly would not recursively call this method, but do following check anyway

		// FileListActions is modifying changes tree inclusion model, ignore.
		if (dialog.isIgnoreIncludeChanged()) {
			return;
		}
		// self calling and modifying changes tree inclusion model, ignore
		if (!updatingIncludeExclude.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
			return;
		}
		try {
			includeExcludeObjectsAll();
			this.setShouldUpdateIncludeExclude(false);
		} finally {
			updatingIncludeExclude.set(Boolean.FALSE);
		}
	}

	/**
	 * takes a ChangesBrowserNode as input and returns the VirtualFile associated
	 * with the node. It checks if the user object of the node is an instance of FilePath or VirtualFile and returns
	 * the corresponding VirtualFile.
	 *
	 * @param treeNode tree node
	 * @return virtual file or null
	 */

	public static VirtualFile getNodeBindVirtualFile(ChangesBrowserNode<?> treeNode) {
		VirtualFile vf = null;
		if (treeNode instanceof ChangesBrowserFileNode) {
			vf = ((ChangesBrowserFileNode) treeNode).getUserObject();
		} else if (treeNode instanceof ChangesBrowserFilePathNode) {
			vf = ((ChangesBrowserFilePathNode) treeNode).getUserObject().getVirtualFile();
		} else if (treeNode instanceof ChangesBrowserModuleNode) {
			vf = ((ChangesBrowserModuleNode) treeNode).getModuleRoot().getVirtualFile();
		} else if (treeNode instanceof RepositoryChangesBrowserNode) {
			vf = ((Repository) treeNode.getUserObject()).getRoot();
		}
		return vf;
	}

	public static boolean isFolderNode(final ChangesBrowserNode<?> node) {
		final VirtualFile virtualFile = getNodeBindVirtualFile(node);
		return virtualFile != null && virtualFile.isDirectory();
	}

	public void groupByChanged(PropertyChangeEvent evt) {
		this.setShouldUpdateIncludeExclude(true);
	}

	public boolean isShouldUpdateIncludeExclude() {
		return shouldUpdateIncludeExclude;
	}

	public void setShouldUpdateIncludeExclude(boolean shouldUpdateIncludeExclude) {
		this.shouldUpdateIncludeExclude = shouldUpdateIncludeExclude;
	}

	/**
	 * update changes tree grouping keys as to refresh tree displaying
	 *
	 * @param changesTree  changes tree
	 * @param groupingKeys new grouping keys
	 */
	public static void updateUIFileListTreeGrouping(final ChangesTree changesTree, String[] groupingKeys) {
		final ChangesGroupingSupport support = changesTree.getGroupingSupport();
		final Set<String> oldGroupingKeys = support.getGroupingKeys();
		final var tempGroupingKeys = new HashSet<>(List.of(groupingKeys));
		if (oldGroupingKeys.size() != tempGroupingKeys.size() || !oldGroupingKeys.containsAll(tempGroupingKeys)) {
			final var tempOldGroupingKeys = new HashSet<>(oldGroupingKeys);
			tempOldGroupingKeys.removeAll(tempGroupingKeys);
			tempOldGroupingKeys.forEach(key -> support.set(key, false));
			tempGroupingKeys.removeAll(oldGroupingKeys);
			tempGroupingKeys.forEach(key -> support.set(key, true));
		}
	}

	/**
	 * Collects virtual files in a tree structure by traversing the tree and filtering out directories.
	 *
	 * @param changesTree The ChangesTree containing the tree structure to collect virtual files from
	 * @return An array of VirtualFile objects representing the collected virtual files
	 */
	public static VirtualFile[] collectVirtualFilesInTree(final ChangesTree changesTree) {
		return TreeUtil.treeNodeTraverser(changesTree.getRoot()).preOrderDfsTraversal().map(n -> getNodeBindVirtualFile((ChangesBrowserNode<?>) n))
				.filter(Objects::nonNull)
				.filter(vf -> !vf.isDirectory())
				.toArray(new VirtualFile[0]);
	}

}

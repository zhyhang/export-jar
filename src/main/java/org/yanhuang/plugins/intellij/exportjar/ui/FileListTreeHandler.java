package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.vcs.changes.ui.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.tree.TreeUtil;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile.SelectType;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.beans.PropertyChangeEvent;
import java.nio.file.Path;
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

	private void includeExcludeObjectsFrom(JBIterable<TreeNode> preOrderDfsNodes) {
		final Map<String, SettingSelectFile> selectFileMap = collectIncludeExcludeObjects(preOrderDfsNodes);
		final SettingSelectFile[] selectFiles = selectFileMap.values().toArray(new SettingSelectFile[0]);
		final List<Map<Path, Object>> finalMaps = SettingSelectFile.combineFinalVirtualFiles(selectFiles);
		this.filesTree.includeChanges(finalMaps.get(0).values());
		this.filesTree.excludeChanges(finalMaps.get(1).values());
	}

	/**
	 * Collects and processes inclusion/exclusion settings for the provided pre-order depth-first traversal nodes.
	 *
	 * @param preOrderDfsNodes The pre-order depth-first traversal nodes to collect settings from
	 * @return A map of updated select file settings
	 */
	private Map<String, SettingSelectFile> collectIncludeExcludeObjects(JBIterable<TreeNode> preOrderDfsNodes) {
		final Map<String, SettingSelectFile> updatedSelectFileMap = new HashMap<>();
		final Map<VirtualFile, SettingSelectFile> parentSelectionMap = new HashMap<>();
		for (TreeNode treeNode : preOrderDfsNodes) {
			final var changeNode = (ChangesBrowserNode<?>) treeNode;
			final var currVirtualFile = getNodeBindVirtualFile(changeNode);
			if (currVirtualFile == null) {
				continue;
			}
			var selectFile = dialog.getFlaggedIncludeExcludeSelection(currVirtualFile);
			if (selectFile == null || isNullSelectType(selectFile)) {
				selectFile = findParentSelection(currVirtualFile, parentSelectionMap);
			} else {
				selectFile = selectFile.clone(); // avoid transient properties "mappingVfs" dirty
			}
			if (selectFile == null) {
				continue;
			}
			if (currVirtualFile.isDirectory()) {
				parentSelectionMap.put(currVirtualFile, selectFile);
			} else {
				//TODO performance change: toOsFile to remove
				selectFile.putMappingVf(CommonUtils.toOsFile(currVirtualFile), changeNode.getUserObject());
				updatedSelectFileMap.put(selectFile.getFilePath(), selectFile);
			}
		}
		return updatedSelectFileMap;
	}

	private SettingSelectFile findParentSelection(VirtualFile currVirtualFile, final Map<VirtualFile,
			SettingSelectFile> parentSelectionMap) {
		VirtualFile parent = currVirtualFile.getParent();
		final SettingSelectFile tempSelectFile = new SettingSelectFile();
		while (null != parent) {
			final SettingSelectFile selectFile = parentSelectionMap.get(parent);
			if (selectFile == null) {
				parentSelectionMap.put(parent, tempSelectFile);
				parent = parent.getParent();
			} else if (isNullSelectType(selectFile)
					|| isNonRecursiveInDirectParent(currVirtualFile, selectFile)) {
				return null; //represent: previous find already not found
			} else {
				tempSelectFile.shallowOverrideFrom(selectFile);
				return tempSelectFile;
			}
		}
		return null;
	}

	private boolean isNonRecursiveInDirectParent(VirtualFile currVirtualFile, SettingSelectFile selectFile) {
		return !selectFile.isRecursive() && (currVirtualFile.isDirectory() || !currVirtualFile.getParent().equals(selectFile.getVirtualFile()));
	}

	private boolean isNullSelectType(SettingSelectFile selectFile) {
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
}

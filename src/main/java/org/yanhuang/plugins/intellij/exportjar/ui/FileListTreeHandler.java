package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.JBTreeTraverser;
import com.intellij.util.ui.tree.TreeUtil;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile.SelectType;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;

import javax.swing.tree.TreeNode;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * files list tree handler.
 * <li>repair include/exclude selection, when mouse click selection or key space press selection.</li>
 */
public class FileListTreeHandler {
	// avoid to infinite recursively invoke updateByIncludeExclude()
	private final AtomicReference<Boolean> updatingIncludeExclude = new AtomicReference<>(Boolean.FALSE);
	private final FileListDialog dialog;
	private final ChangesTree filesTree;
	private transient SettingSelectFile[] updateStateSettingFiles;

	public FileListTreeHandler(FileListDialog dialog) {
		this.dialog = dialog;
		this.filesTree = dialog.getFileList();
	}

	/**
	 * call this method repair include/exclude selection, when mouse click selection or key space press selection.
	 * </br><b>Updates the changes tree inclusion model based on include or exclude criteria.
	 * If the dialog is set to ignore include changes or if another thread is already updating the inclusion model,
	 * this method will return without making any updates.</b>
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

	/**
	 * Repairs the changes related to including or excluding files or directories in the changes tree.
	 * Iterates through the tree nodes in the files tree and for each node that is marked as include or exclude,
	 * it checks if it should be included recursively and then calls FileListActions.includeExcludeSubObjects()
	 * to update the sub-objects accordingly.
	 */
	private void repairInclusionChanges() {
		final JBTreeTraverser<TreeNode> treeNodes = TreeUtil.treeNodeTraverser(filesTree.getRoot());
		for (TreeNode treeNode : treeNodes) {
			final var changeNode = (ChangesBrowserNode<?>) treeNode;
			final SelectType selectType =
					changeNode.getUserData(FileListDialog.KEY_TYPE_SELECT_FILE_DIRECTORY);
			if (selectType == SelectType.include || selectType == SelectType.exclude) {
				final var isRecursive =
						Boolean.TRUE.equals(changeNode.getUserData(FileListDialog.KEY_RECURSIVE_SELECT_DIRECTORY));
				FileListActions.includeExcludeSubObjects(isRecursive, changeNode, filesTree, selectType);
			}
		}
	}

	public void updateIncludeExcludeState() {
		if (this.updateStateSettingFiles == null || this.updateStateSettingFiles.length == 0) {
			return;
		}
		// FileListActions is modifying changes tree inclusion model, ignore.
		if (dialog.isIgnoreIncludeChanged()) {
			return;
		}
		// self calling and modifying changes tree inclusion model, ignore
		if (!updatingIncludeExclude.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
			return;
		}
		try{
			doUpdateIncludeExcludeState(this.updateStateSettingFiles);
			this.updateStateSettingFiles=null; // clean the property as only update once
		} finally {
			updatingIncludeExclude.set(Boolean.FALSE);
		}
	}

	private void doUpdateIncludeExcludeState(SettingSelectFile[] selectFiles) {
		final var vfSettingMap = new HashMap<VirtualFile, SettingSelectFile>();
		for (SettingSelectFile selectFile : selectFiles) {
			if (selectFile != null) {
				vfSettingMap.put(CommonUtils.fromOsFile(selectFile.getFilePath()), selectFile);
			}
		}
		final JBTreeTraverser<TreeNode> treeNodes = TreeUtil.treeNodeTraverser(filesTree.getRoot());
		for (TreeNode treeNode : treeNodes) {
			final var changeNode = (ChangesBrowserNode<?>) treeNode;
			final VirtualFile virtualFile = getNodeBindVirtualFile(changeNode);
			if (virtualFile == null) {
				continue;
			}
			doUpdateIncludeExcludeState(vfSettingMap, virtualFile, changeNode);
		}
	}

	private void doUpdateIncludeExcludeState(HashMap<VirtualFile, SettingSelectFile> vfSettingMap, VirtualFile virtualFile, ChangesBrowserNode<?> changeNode) {
		final SettingSelectFile selectFile = vfSettingMap.get(virtualFile);
		if (selectFile != null) {
			final SelectType fileSelectType = selectFile.getSelectType();
			if (fileSelectType == SelectType.include || fileSelectType == SelectType.exclude) {
				FileListActions.putIncludeExcludeSelectFlag(selectFile.isRecursive(), changeNode, this.filesTree,
						fileSelectType);
			}
		}
	}

	/**
	 * takes a ChangesBrowserNode as input and returns the VirtualFile associated
	 * 	with the node. It checks if the user object of the node is an instance of FilePath or VirtualFile and returns
	 * 	the corresponding VirtualFile.
	 * @param treeNode tree node
	 * @return virtual file or null
	 */

	public static VirtualFile getNodeBindVirtualFile(ChangesBrowserNode<?> treeNode) {
		final var nodeObject = treeNode.getUserObject();
		VirtualFile vf = null;
		if (nodeObject instanceof FilePath) {
			vf = ((FilePath) nodeObject).getVirtualFile();
		} else if (nodeObject instanceof VirtualFile) {
			vf = (VirtualFile) nodeObject;
		}
		return vf;
	}

	public void setUpdateStateSettingFiles(SettingSelectFile[] updateStateSettingFiles) {
		this.updateStateSettingFiles = updateStateSettingFiles;
	}
}

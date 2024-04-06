package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesGroupingPolicyFactory;
import com.intellij.openapi.vcs.changes.ui.ChangesGroupingSupport;
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.*;

import static com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode.createFilePath;
import static org.yanhuang.plugins.intellij.exportjar.utils.Constants.fileListTreeHelperNodeKey;

public class FileListTree extends SelectFilesDialog.VirtualFileList {

	public final static Key<Boolean> KEY_HELPER_NODE = Key.create(fileListTreeHelperNodeKey);

	private boolean expandAllDirectory = false;

	private final Set<VirtualFile> projectModuleRoots;

	public FileListTree(Project project, boolean selectableFiles, boolean deletableFiles, @NotNull List<?
			extends VirtualFile> files) {
		super(project, selectableFiles, deletableFiles, files);
		this.projectModuleRoots = initProjectModuleRoots(project);
	}

	private Set<VirtualFile> initProjectModuleRoots(Project project) {
		final var roots = new HashSet<VirtualFile>();
		final Module[] modules = ModuleManager.getInstance(project).getModules();
		for (Module module : modules) {
			final VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
			roots.addAll(List.of(contentRoots));
		}
		return roots;
	}

	@Override
	protected @NotNull DefaultTreeModel buildTreeModel(@NotNull ChangesGroupingPolicyFactory grouping, @NotNull List<?
			extends VirtualFile> changes) {
		final DefaultTreeModel defaultTreeModel = super.buildTreeModel(grouping, changes);
		if (isDirectoryModuleGrouping() && this.isExpandAllDirectory()) {
			expandDirectoryNodes(defaultTreeModel);
		}
		if (isDirectoryModuleGrouping()) {
			collapseDirectoryNotInModules(defaultTreeModel);
		}
		return defaultTreeModel;
	}

	private void expandDirectoryNodes(DefaultTreeModel model) {
		final ChangesBrowserNode<?> root = (ChangesBrowserNode<?>) model.getRoot();
		final var treeNodes = TreeUtil.treeNodeTraverser(root).postOrderDfsTraversal().toList();
		final var directoryNodeCache = new HashMap<VirtualFile, ChangesBrowserNode<?>>();
		treeNodes.forEach(n -> {
			final var node = (ChangesBrowserNode<?>) n;
			final ChangesBrowserNode<?> parent = node.getParent();
			final VirtualFile nodeVf = FileListTreeHandler.getNodeBindVirtualFile(node);
			final VirtualFile parentVf = FileListTreeHandler.getNodeBindVirtualFile(parent);
			if (isShouldExpand(node, nodeVf, parent, parentVf)) {
				expandDirectoryBetween(directoryNodeCache, node, nodeVf, parent, parentVf);
			}
		});
	}

	private void expandDirectoryBetween(Map<VirtualFile, ChangesBrowserNode<?>> directoryNodeCache,
	                                    ChangesBrowserNode<?> node, VirtualFile nodeVf, ChangesBrowserNode<?> parent,
	                                    VirtualFile parentVf) {
		var directParentVf = nodeVf.getParent();
		var currentNode = node;
		while (directParentVf != null && !directParentVf.equals(parentVf)) {
			final var cachingParent = directoryNodeCache.get(directParentVf);
			ChangesBrowserNode<?> directParent;
			if (cachingParent != null) {
				directParent = cachingParent;
			} else {
				directParent = createFilePath(VcsUtil.getFilePath(directParentVf));
				directParent.markAsHelperNode();
				directoryNodeCache.put(directParentVf, directParent);
			}
			if(!directParent.isNodeChild(currentNode)) {
				directParent.insert(currentNode, directParent.getChildCount());
			}
			currentNode = directParent;
			directParentVf = directParentVf.getParent();
		}
		parent.insert(currentNode, 0);
	}

	private static boolean isShouldExpand(ChangesBrowserNode<?> node, VirtualFile nodeVf, ChangesBrowserNode<?> parent,
	                                      VirtualFile parentVf) {
		return parent != null && parent.isRoot() ||
				nodeVf != null && parentVf != null && nodeVf.isDirectory() && parentVf.isDirectory() && !nodeVf.getParent().equals(parentVf);
	}

	private void collapseDirectoryNotInModules(DefaultTreeModel model) {
		final ChangesBrowserNode<?> root = (ChangesBrowserNode<?>) model.getRoot();
		final var treeNodes = TreeUtil.treeNodeTraverser(root).preOrderDfsTraversal().toList();
		treeNodes.forEach(n -> {
			final var node = (ChangesBrowserNode<?>) n;
			final var virtualFile = FileListTreeHandler.getNodeBindVirtualFile(node);
			if (virtualFile != null && !isInModule(virtualFile)) {
				moveChildToRoot(root, node);
			}
		});
	}

	private boolean isInModule(VirtualFile virtualFile) {
		for (VirtualFile moduleRoot : projectModuleRoots) {
			if (VfsUtil.isAncestor(moduleRoot, virtualFile, false)) {
				return true;
			}
		}
		return false;
	}

	private void moveChildToRoot(ChangesBrowserNode<?> root, ChangesBrowserNode<?> node) {
		node.removeFromParent();
		int rootChildCount = root.getChildCount();
		final List<TreeNode> nodeChildren = TreeUtil.nodeChildren(node).toList();
		for (int i = 0; i < nodeChildren.size(); i++) {
			root.insert((MutableTreeNode) nodeChildren.get(i), rootChildCount + i);
		}
	}

	public boolean isDirectoryModuleGrouping() {
		final Set<String> groupingKeys = this.getGroupingSupport().getGroupingKeys();
		return this.getGroupingSupport().isDirectory() &&
				(groupingKeys.size() == 1 ||
						groupingKeys.size() == 2 && groupingKeys.contains(ChangesGroupingSupport.MODULE_GROUPING));
	}

	public boolean isExpandAllDirectory() {
		return expandAllDirectory;
	}

	public void setExpandAllDirectory(boolean expandAllDirectory) {
		this.expandAllDirectory = expandAllDirectory;
	}
}

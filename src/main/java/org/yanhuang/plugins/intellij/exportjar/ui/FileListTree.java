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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	private Set<VirtualFile> initProjectModuleRoots(Project project){
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
		final var treeNodes = TreeUtil.treeNodeTraverser(root).postOrderDfsTraversal();
		treeNodes.forEach(n -> {
			final var node = (ChangesBrowserNode<?>) n;
			final ChangesBrowserNode<?> parent = node.getParent();
			final VirtualFile nodeVf = FileListTreeHandler.getNodeBindVirtualFile(node);
			final VirtualFile parentVf = FileListTreeHandler.getNodeBindVirtualFile(parent);
			if (isShouldExpand(node, nodeVf, parent, parentVf)) {
				expandDirectoryBetween(node, nodeVf, parent, parentVf);
			}
		});
	}

	private void expandDirectoryBetween(ChangesBrowserNode<?> node, VirtualFile nodeVf, ChangesBrowserNode<?> parent,
	                                    VirtualFile parentVf) {
		var directParentVf = nodeVf.getParent();
		var currentNode = node;
		while (directParentVf != null && !directParentVf.equals(parentVf)) {
			final var directParent = createFilePath(VcsUtil.getFilePath(directParentVf));
			directParent.markAsHelperNode();
			directParent.insert(currentNode, 0);
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

	private void collapseDirectoryNotInModules(DefaultTreeModel model){
		final ChangesBrowserNode<?> root = (ChangesBrowserNode<?>) model.getRoot();
		TreeUtil.treeNodeTraverser(root).bfsTraversal();
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

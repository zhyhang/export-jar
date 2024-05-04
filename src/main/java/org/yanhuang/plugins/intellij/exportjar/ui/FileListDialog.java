package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.changes.ui.*;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;
import org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;

import static com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode.createFilePath;
import static org.yanhuang.plugins.intellij.exportjar.utils.Constants.fileListTreeHelperNodeKey;

/**
 * Dialog extend from SelectFilesDialog without the buttons. It is used to show select files.
 */
public class FileListDialog extends SelectFilesDialog {
	public final static Key<Boolean> KEY_HELPER_NODE = Key.create(fileListTreeHelperNodeKey);

	private JComponent centerPanel;
	private DefaultActionGroup actionGroup;

	private boolean recursiveActionSelected;
	private boolean ignoreIncludeChanged;
	private final FileListTreeHandler handler;
	// virtual file to Include/Exclude selection mapping flagged by do actions
	private final Map<VirtualFile, SettingSelectFile> flaggedVirtualFileSelectionMap = new HashMap<>();

	public FileListDialog(Project project, @NotNull List<? extends VirtualFile> files, @Nullable String prompt,
                          @Nullable VcsShowConfirmationOption confirmationOption, boolean selectableFiles,
                          boolean deletableFiles) {
		super(project, files, prompt, confirmationOption, selectableFiles, deletableFiles);
		replaceFilesTree(project, files, selectableFiles, deletableFiles);
		init();
		final ChangesTree filesTree = getFileList();
		// listener for restore the include/exclude select state because of mouse/space key select changed
		this.handler = new FileListTreeHandler(this);
		filesTree.setInclusionListener(handler::reIncludeExcludeBySelections);
		filesTree.addSelectionListener(handler::updateIncludeExcludeWhenSelectChange);
		filesTree.addGroupingChangeListener(handler::groupByChanged);
		filesTree.setCellRenderer(new FileListTreeCellRender(this, filesTree.getCellRenderer()));
	}

	private void replaceFilesTree(Project project, List<? extends VirtualFile> files, boolean selectableFiles, boolean deletableFiles) {
		try {
			final Field treeField = getTreeField(getFileList());
			if (treeField != null) {
				final var newTree = new FileListTree(project, selectableFiles, deletableFiles, files);
				treeField.setAccessible(true);
				treeField.set(this, newTree);
			}
		} catch (IllegalAccessException e) {
			MessagesUtils.warn(project, "replace the file list tree field by customized error: " + e.getMessage());
		}
	}

	private Field getTreeField(ChangesTree orgFilesTree) throws IllegalAccessException {
		final List<Field> fields = ReflectionUtil.collectFields(SelectFilesDialog.class);
		for (Field field : fields) {
			field.setAccessible(true);
			if (field.get(this) == orgFilesTree) {
				return field;
			}
		}
		return null;
	}

	@Override
	protected @Nullable
	JComponent createCenterPanel() {
		this.centerPanel = super.createCenterPanel();
		return this.centerPanel;
	}

	public JComponent getCenterPanel() {
		return centerPanel;
	}

	public void addFileTreeChangeListener(BiConsumer<FileListDialog, PropertyChangeEvent> listener) {
		getFileList().addPropertyChangeListener(e -> listener.accept(this, e));
	}

	@Override
	public @NotNull ChangesTree getFileList() {
		return super.getFileList();
	}

	/**
	 * Retrieves the include and exclude selections from the file list dialog and returns an array of
     * SettingSelectFile objects.
	 * <li>general to use for template export setting files saving history</li>
	 * <li>if get final files (only files) to export, use getSelectedFiles() method</li>
	 *
	 * @return An array of SettingSelectFile objects representing the include and exclude selections (files and dirs).
	 * Include noop or select type is null.
	 * The array contains the selected files with their respective select type (include or exclude),
	 * file path, and recursive flag.
	 */
	public SettingSelectFile[] getStoreIncludeExcludeSelections() {
		final VirtualFile[] svfFiles = getSelectedFiles().toArray(new VirtualFile[0]);
		final List<SettingSelectFile> finalSelectFiles = createFinalSelectFiles(svfFiles);
		return finalSelectFiles.toArray(new SettingSelectFile[0]);
	}

	private List<SettingSelectFile> createFinalSelectFiles(VirtualFile[] svfFiles) {
		final List<SettingSelectFile> finalSelectFiles = new ArrayList<>();
		for (VirtualFile svfFile : svfFiles) {
			final SettingSelectFile selectFile = this.flaggedVirtualFileSelectionMap.get(svfFile);
			if (selectFile == null) {
				final SettingSelectFile selectFileNew = new SettingSelectFile();
				selectFileNew.setFilePath(CommonUtils.toOsFile(svfFile).toString());
				finalSelectFiles.add(selectFileNew);
			}
		}
		final boolean ignored = finalSelectFiles.addAll(filterMatchTreeNode());
		return finalSelectFiles;
	}

	/**
	 * filter SettingSelectFile that in file list tree.
	 */
	private List<SettingSelectFile> filterMatchTreeNode() {
		final ChangesBrowserNode<?> root = this.getFileList().getRoot();
		final Set<VirtualFile> allVfInTree = new HashSet<>();
		TreeUtil.treeNodeTraverser(root).preOrderDfsTraversal().forEach(n->{
			final var node = (ChangesBrowserNode<?>) n;
			final var vfs = FileListTreeHandler.getNodeBindVirtualFile(node);
			if (vfs != null) {
				allVfInTree.add(vfs);
			}
		});
		final var filterSelectFiles = new ArrayList<SettingSelectFile>();
		for (SettingSelectFile ssf : this.flaggedVirtualFileSelectionMap.values()) {
			final VirtualFile ssfVirtualFile = ssf.getVirtualFile();
			if (ssfVirtualFile == null || allVfInTree.contains(ssfVirtualFile)) {
				filterSelectFiles.add(ssf);
			}
		}
		return filterSelectFiles;
	}

	@Override
	protected @NotNull DefaultActionGroup createToolbarActions() {
		final DefaultActionGroup group = super.createToolbarActions();
		group.addAll(FileListActions.treeOperationActions(this));
		this.actionGroup = group;
		return group;
	}

	public boolean isRecursiveActionSelected() {
		return recursiveActionSelected;
	}

	public void setRecursiveActionSelected(boolean recursiveActionSelected) {
		this.recursiveActionSelected = recursiveActionSelected;
	}

	public boolean isIgnoreIncludeChanged() {
		return ignoreIncludeChanged;
	}

	public void setIgnoreIncludeChanged(boolean ignoreIncludeChanged) {
		this.ignoreIncludeChanged = ignoreIncludeChanged;
	}

	public FileListTreeHandler getHandler() {
		return handler;
	}

	public SettingSelectFile[] getFlaggedIncludeExcludeSelections() {
		return this.flaggedVirtualFileSelectionMap.values().toArray(new SettingSelectFile[0]);
	}

	public boolean isFlaggedIncludeExcludeEmpty() {
		return this.flaggedVirtualFileSelectionMap.isEmpty();
	}

	public void setFlaggedIncludeExcludeSelections(SettingSelectFile[] savedIncludeExcludeSelections) {
		this.flaggedVirtualFileSelectionMap.clear();// must clear first
		for (SettingSelectFile selectFile : savedIncludeExcludeSelections != null ? savedIncludeExcludeSelections :
				new SettingSelectFile[0]) {
			if (selectFile == null || FileListTreeHandler.isNullSelectType(selectFile)) {
				continue;// filter out the noop select type
			}
			this.putFlaggedIncludeExcludeSelection(selectFile.getVirtualFile(), selectFile);
		}
	}

	public SettingSelectFile getFlaggedIncludeExcludeSelection(VirtualFile vf) {
		return flaggedVirtualFileSelectionMap.get(vf);
	}

	public SettingSelectFile putFlaggedIncludeExcludeSelection(VirtualFile virtualFile, SettingSelectFile selectFile) {
		return this.flaggedVirtualFileSelectionMap.put(virtualFile, selectFile);
	}

	public SettingSelectFile removeFlaggedIncludeExcludeSelection(VirtualFile virtualFile) {
		return this.flaggedVirtualFileSelectionMap.remove(virtualFile);
	}

	public boolean isExpandAllDirectory(){
		if (this.getFileList() instanceof FileListTree) {
			return ((FileListTree) this.getFileList()).isExpandAllDirectory();
		}
		return false;
	}

	public String[] getGroupingKeys(){
		final Set<String> keys = this.getFileList().getGroupingSupport().getGroupingKeys();
		return keys.toArray(new String[0]);
	}

	public boolean isInModule(VirtualFile virtualFile) {
		if (this.getFileList() instanceof FileListTree) {
			return ((FileListTree) this.getFileList()).isInModule(virtualFile);
		}
		return false;
	}

	public void setExpandAllDirectory(boolean expandAllDirectory) {
		if (this.getFileList() instanceof FileListTree) {
			((FileListTree) this.getFileList()).setExpandAllDirectory(expandAllDirectory);
		}
	}

	boolean isDirectoryModuleGrouping() {
		if (this.getFileList() instanceof FileListTree) {
			return ((FileListTree) this.getFileList()).isDirectoryModuleGrouping();
		}
		return false;
	}

	private static class FileListTree extends VirtualFileList {

		private boolean expandAllDirectory = false;

		private Set<VirtualFile> projectModuleRoots;

		public FileListTree(Project project, boolean selectableFiles, boolean deletableFiles, @NotNull List<?
				extends VirtualFile> files) {
			super(project, selectableFiles, deletableFiles, files);
		}

		protected @NotNull DefaultTreeModel buildTreeModel(@NotNull ChangesGroupingPolicyFactory grouping, @NotNull List<?
				extends VirtualFile> changes) {
			// from 2023.2 should use following line
//			final DefaultTreeModel defaultTreeModel = super.buildTreeModel(grouping, changes);
			// for pass the IntelliJ Plugin Verifier
			final DefaultTreeModel defaultTreeModel = TreeModelBuilder.buildFromVirtualFiles(myProject, grouping, changes);
			if (isDirectoryModuleGrouping() && this.isExpandAllDirectory()) {
				expandDirectoryNodes(defaultTreeModel);
			}
			if (isDirectoryModuleGrouping()) {
				collapseDirectoryNotInModules(defaultTreeModel);
			}
			return defaultTreeModel;
		}

		// for compatible with before version 2022
		protected @NotNull DefaultTreeModel buildTreeModel(@NotNull List<? extends VirtualFile> changes) {
			final DefaultTreeModel defaultTreeModel = TreeModelBuilder.buildFromVirtualFiles(myProject, getGrouping(), changes);
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
			// as to compatible with version 2022 and before, must initial modules roots instance variable here can not in constructor
			if (projectModuleRoots == null) {
				initProjectModuleRoots(this.getProject());
			}
			for (VirtualFile moduleRoot : projectModuleRoots) {
				if (VfsUtil.isAncestor(moduleRoot, virtualFile, false)) {
					return true;
				}
			}
			return false;
		}

		private void initProjectModuleRoots(Project project) {
			final var mrs = new HashSet<VirtualFile>();
			final Module[] modules = ModuleManager.getInstance(project).getModules();
			for (Module module : modules) {
				final VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
				mrs.addAll(List.of(contentRoots));
			}
			this.projectModuleRoots = mrs;
		}

		private void moveChildToRoot(ChangesBrowserNode<?> root, ChangesBrowserNode<?> node) {
			node.removeFromParent();
			int rootChildCount = root.getChildCount();
			final List<TreeNode> nodeChildren = TreeUtil.nodeChildren(node).toList();
			for (int i = 0; i < nodeChildren.size(); i++) {
				root.insert((MutableTreeNode) nodeChildren.get(i), rootChildCount + i);
			}
		}

		private boolean isDirectoryModuleGrouping() {
			final Set<String> groupingKeys = this.getGroupingSupport().getGroupingKeys();
			return this.getGroupingSupport().isDirectory() &&
					(groupingKeys.size() == 1 ||
							groupingKeys.size() == 2 && groupingKeys.contains(ChangesGroupingSupport.MODULE_GROUPING));
		}

		private boolean isExpandAllDirectory() {
			return expandAllDirectory;
		}

		private void setExpandAllDirectory(boolean expandAllDirectory) {
			this.expandAllDirectory = expandAllDirectory;
		}

	}
}

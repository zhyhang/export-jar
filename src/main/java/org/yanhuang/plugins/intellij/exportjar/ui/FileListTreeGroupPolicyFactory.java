package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.ChangesGroupingPolicy;
import com.intellij.openapi.vcs.changes.ui.ChangesGroupingPolicyFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;

/**
 * Register in plugin.xml as changesGroupingPolicy extension.
 * Register the group by action in plugin.xml meanwhile.
 * @see FileListActions.SetDirectoryNoCollapseChangesGroupingAction
 */
public class FileListTreeGroupPolicyFactory extends ChangesGroupingPolicyFactory {
	@Override
	public @NotNull ChangesGroupingPolicy createGroupingPolicy(@NotNull Project project, @NotNull DefaultTreeModel model) {
//		return new FileListTreeGroupPolicy(project,model);
		//Unused, for pass the IntelliJ Plugin Verifier
		return null;
	}

}
// unused
//class FileListTreeGroupPolicy extends BaseChangesGroupingPolicy {
//	private final Project project;
//	private final DefaultTreeModel model;
//
//	public FileListTreeGroupPolicy(Project project, DefaultTreeModel model) {
//		this.project = project;
//		this.model = model;
//	}
//
//	@Override
//	public @Nullable ChangesBrowserNode<?> getParentNodeFor(@NotNull StaticFilePath nodePath, @NotNull ChangesBrowserNode<?> subtreeRoot) {
//		ChangesBrowserNode<?> nextPolicyGroup = getNextPolicy() != null ? getNextPolicy().getParentNodeFor(nodePath, subtreeRoot) : null;
//		ChangesBrowserNode<?> grandParent = nextPolicyGroup != null ? nextPolicyGroup : subtreeRoot;
//		ChangesBrowserNode<?> cachingRoot = getCachingRoot(grandParent, subtreeRoot);
//		Function<StaticFilePath, ChangesBrowserNode<?>> pathBuilder = PATH_NODE_BUILDER.getRequired(subtreeRoot);
//
//		return getParentNodeRecursive(nodePath, pathBuilder, grandParent, cachingRoot);
//
//	}
//
//	private ChangesBrowserNode<?> getParentNodeRecursive(StaticFilePath nodePath,
//	                                                     Function<StaticFilePath, ChangesBrowserNode<?>> pathBuilder,
//	                                                     ChangesBrowserNode<?> grandParent,
//	                                                     ChangesBrowserNode<?> cachingRoot) {
//		ChangesBrowserNode<?> cachedParent = null;
//		List<ChangesBrowserNode<?>> nodes = new ArrayList<>();
//
//		StaticFilePath parentPath = nodePath;
//		while (true) {
//			parentPath = parentPath.getParent();
//			if (parentPath == null) {
//				break;
//			}
//
//			cachedParent = DIRECTORY_CACHE.getValue(cachingRoot).get(parentPath.getKey());
//			if (cachedParent != null) {
//				break;
//			}
//
//			ChangesBrowserNode<?> pathNode = pathBuilder.apply(parentPath);
//			if (pathNode != null) {
//				pathNode.markAsHelperNode();
//				DIRECTORY_CACHE.getValue(cachingRoot).put(parentPath.getKey(), pathNode);
//				final int childCount = pathNode.getChildCount();
//				if(childCount<=1 && pathNode.getFileCount()<=0) {
//					final ChangesBrowserNode<?> helperNode = ChangesBrowserNode.createRoot();
//					helperNode.putUserData(FileListDialog.KEY_HELPER_NODE, Boolean.TRUE);
//					pathNode.insert(helperNode, childCount);
//				}
//				nodes.add(pathNode);
//			}
//		}
//
//		ChangesBrowserNode<?> node = cachedParent != null ? cachedParent : grandParent;
//		for (int i = nodes.size() - 1; i >= 0; i--) {
//			ChangesBrowserNode<?> nextNode = nodes.get(i);
//			model.insertNodeInto(nextNode, node, node.getChildCount());
//			node = nextNode;
//		}
//		return node;
//	}
//}

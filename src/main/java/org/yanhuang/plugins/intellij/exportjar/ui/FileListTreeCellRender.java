package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNodeRenderer;
import com.intellij.util.ui.ThreeStateCheckBox;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.Arrays;
import java.util.Optional;

import static com.intellij.ui.SimpleTextAttributes.SYNTHETIC_ATTRIBUTES;
import static org.yanhuang.plugins.intellij.exportjar.utils.Constants.toolTipRecursiveDirectorySelect;

public class FileListTreeCellRender implements TreeCellRenderer {
	private final TreeCellRenderer ideOrgRenderer;

	public FileListTreeCellRender(TreeCellRenderer ideOrgRenderer) {
		this.ideOrgRenderer = ideOrgRenderer;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
	                                              boolean leaf, int row, boolean hasFocus) {
		final ChangesBrowserNode<?> currentNode = (ChangesBrowserNode<?>) value;
		final JComponent orgRenderedNodeUI = (JComponent) ideOrgRenderer.getTreeCellRendererComponent(tree, value,
				selected, expanded, leaf, row, hasFocus);
		if (checkUpdateRecursiveNode(currentNode, orgRenderedNodeUI)) {
			renderRecursiveText(orgRenderedNodeUI);
		}
		return orgRenderedNodeUI;
	}

	private void renderRecursiveText(JComponent orgRenderedNodeUI) {
		final Component[] components = orgRenderedNodeUI.getComponents();
		final Optional<Component> fileRenderer =
				Arrays.stream(components).filter(c -> c instanceof ChangesBrowserNodeRenderer).findFirst();
		fileRenderer.ifPresent(c -> {
			final var renderer = ((ChangesBrowserNodeRenderer) c);
			renderer.append(" [R]", SYNTHETIC_ATTRIBUTES, false);
			renderer.setToolTipText(toolTipRecursiveDirectorySelect);
		});
	}

	private boolean checkUpdateRecursiveNode(final ChangesBrowserNode<?> currentNode, JComponent orgRenderedNodeUI) {
		final Component[] components = orgRenderedNodeUI.getComponents();
		final Optional<Component> checkboxRenderer =
				Arrays.stream(components).filter(c -> c instanceof ThreeStateCheckBox).findFirst();
		checkboxRenderer.ifPresent(c -> {
			final var checkBox = (ThreeStateCheckBox) c;
			if (checkBox.getState() == ThreeStateCheckBox.State.NOT_SELECTED) {
				// clear Recursive flag
				currentNode.putUserData(FileListDialog.KEY_RECURSIVE_SELECT_DIRECTORY, null);
			}
		});
		final Boolean recursiveDir = currentNode.getUserData(FileListDialog.KEY_RECURSIVE_SELECT_DIRECTORY);
		return Boolean.TRUE.equals(recursiveDir);
	}

}

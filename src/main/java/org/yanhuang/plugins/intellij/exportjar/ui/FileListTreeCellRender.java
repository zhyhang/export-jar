package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNodeRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile.SelectType;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static com.intellij.ui.SimpleTextAttributes.ERROR_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.SYNTHETIC_ATTRIBUTES;
import static org.yanhuang.plugins.intellij.exportjar.utils.Constants.*;

/**
 * renderer: according to include/exclude and recursive state to renderer the tree node (file/dir) text.
 * <pre>
 *     // can use following code to get tree select state checkbox
 *     	final Component[] components = orgRenderedNodeUI.getComponents();
 * 		final Optional<Component> checkboxRenderer =
 * 				Arrays.stream(components).filter(c -> c instanceof ThreeStateCheckBox).findFirst();
 * 		if (checkboxRenderer.isPresent()) {
 * 			final var checkBox = (ThreeStateCheckBox) checkboxRenderer.get();
 * 			return checkBox.getState();
 *                } else {
 * 			return ThreeStateCheckBox.State.DONT_CARE;
 *        }
 *
 * </pre>
 */
public class FileListTreeCellRender implements TreeCellRenderer {
	private final FileListDialog dialog;
	private final TreeCellRenderer ideOrgRenderer;

	private final static Map<String, String> tooltipMap = Map.of(
			// key=dir+include/exclude+recursive
			true + SelectType.include.name() + true, toolTipRecursiveDirectoryIncludeSelect,
			true + SelectType.include.name() + false, toolTipDirectoryIncludeSelect,
			true + SelectType.exclude.name() + true, toolTipRecursiveDirectoryExcludeSelect,
			true + SelectType.exclude.name() + false, toolTipDirectoryExcludeSelect,
			false + SelectType.include.name() + false, toolTipFileIncludeSelect,
			false + SelectType.exclude.name() + false, toolTipFileExcludeSelect
	);

	public FileListTreeCellRender(FileListDialog dialog, TreeCellRenderer ideOrgRenderer) {
		this.dialog = dialog;
		this.ideOrgRenderer = ideOrgRenderer;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
	                                              boolean leaf, int row, boolean hasFocus) {
		final ChangesBrowserNode<?> currentNode = (ChangesBrowserNode<?>) value;
		final JComponent orgRenderedNodeUI = (JComponent) ideOrgRenderer.getTreeCellRendererComponent(tree, value,
				selected, expanded, leaf, row, hasFocus);
		renderSelectFlagText(currentNode, orgRenderedNodeUI);
		return orgRenderedNodeUI;
	}

	private void renderSelectFlagText(ChangesBrowserNode<?> currentNode, JComponent orgRenderedNodeUI) {
		final VirtualFile virtualFile = FileListTreeHandler.getNodeBindVirtualFile(currentNode);
		final SettingSelectFile selectFile = dialog.getFlagIncludeExcludeSelection(virtualFile);
		final Boolean recursive = selectFile != null && selectFile.isRecursive();
		final SelectType selectType = selectFile != null ? selectFile.getSelectType() : SelectType.noop;
		final Component[] components = orgRenderedNodeUI.getComponents();
		final Optional<Component> fileRenderer =
				Arrays.stream(components).filter(c -> c instanceof ChangesBrowserNodeRenderer).findFirst();
		fileRenderer.ifPresent(c -> {
			final var renderer = ((ChangesBrowserNodeRenderer) c);
			if (Boolean.TRUE.equals(recursive)) {
				renderer.append(" " + Constants.flagRecursiveSelectUnSelect, SYNTHETIC_ATTRIBUTES, false);
			}
			if (SelectType.include == selectType) {
				renderer.append(" " + Constants.flagIncludeSelect, ERROR_ATTRIBUTES, false);
				setRenderTooltip(currentNode, renderer, Boolean.TRUE.equals(recursive), selectType);
			} else if (SelectType.exclude == selectType) {
				renderer.append(" " + Constants.flagExcludeSelect, ERROR_ATTRIBUTES, false);
				setRenderTooltip(currentNode, renderer, Boolean.TRUE.equals(recursive), selectType);
			}
		});
	}

	private void setRenderTooltip(ChangesBrowserNode<?> currentNode, ChangesBrowserNodeRenderer renderer,
	                              boolean isRecursive, SelectType selectType) {
		final String tooltipKey = FileListActions.isFolderNode(currentNode) + selectType.name() + isRecursive;
		final String tooltip = tooltipMap.get(tooltipKey);
		renderer.setToolTipText(tooltip);
	}

}

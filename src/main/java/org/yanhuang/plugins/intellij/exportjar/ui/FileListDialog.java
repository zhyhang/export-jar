package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Dialog extend from SelectFilesDialog without the buttons. It is used to show select files.
 */
public class FileListDialog extends SelectFilesDialog {

	private JComponent centerPanel;

	private boolean recursiveActionSelected;
	private boolean ignoreIncludeChanged;
	private final FileListTreeHandler handler;
	// virtual file to Include/Exclude selection mapping flagged by do actions
	private final Map<VirtualFile, SettingSelectFile> flaggedVirtualFileSelectionMap = new HashMap<>();

	public FileListDialog(Project project, @NotNull List<? extends VirtualFile> files, @Nullable String prompt,
                          @Nullable VcsShowConfirmationOption confirmationOption, boolean selectableFiles,
                          boolean deletableFiles) {
		super(project, files, prompt, confirmationOption, selectableFiles, deletableFiles);
		init();
		final ChangesTree filesTree = getFileList();
		// listener for restore the include/exclude select state because of mouse/space key select changed
		this.handler = new FileListTreeHandler(this);
		filesTree.setInclusionListener(handler::reIncludeExcludeBySelections);
		filesTree.addSelectionListener(handler::updateIncludeExcludeWhenSelectChange);
		filesTree.addGroupingChangeListener(handler::groupByChanged);
		filesTree.setCellRenderer(new FileListTreeCellRender(this, filesTree.getCellRenderer()));
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
	 * The array contains the selected files with their respective select type (include or exclude),
	 * file path, and recursive flag.
	 */
	public SettingSelectFile[] getStoreIncludeExcludeSelections() {
		final VirtualFile[] svfFiles = getSelectedFiles().toArray(new VirtualFile[0]);
		final List<SettingSelectFile> finalSelectFiles = createFinalSelectFiles(svfFiles,
                this.flaggedVirtualFileSelectionMap);
		return finalSelectFiles.toArray(new SettingSelectFile[0]);
	}

	private List<SettingSelectFile> createFinalSelectFiles(VirtualFile[] svfFiles,
                                                           Map<VirtualFile, SettingSelectFile> vfSettingMap) {
		final List<SettingSelectFile> finalSelectFiles = new ArrayList<>();
		for (VirtualFile svfFile : svfFiles) {
			final SettingSelectFile selectFile = vfSettingMap.get(svfFile);
			if (selectFile == null) {
				final SettingSelectFile selectFileNew = new SettingSelectFile();
				selectFileNew.setFilePath(CommonUtils.toOsFile(svfFile).toString());
				finalSelectFiles.add(selectFileNew);
			}
		}
		final boolean ignored = finalSelectFiles.addAll(vfSettingMap.values());
		return finalSelectFiles;
	}

	@Override
	protected @NotNull DefaultActionGroup createToolbarActions() {
		final DefaultActionGroup group = super.createToolbarActions();
		group.addAll(FileListActions.treeOperationActions(this));
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
		if (savedIncludeExcludeSelections == null) {
			return;
		}
		this.flaggedVirtualFileSelectionMap.clear();
		for (SettingSelectFile selectFile : savedIncludeExcludeSelections) {
            if (selectFile==null || FileListTreeHandler.isNullSelectType(selectFile)) {
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

}

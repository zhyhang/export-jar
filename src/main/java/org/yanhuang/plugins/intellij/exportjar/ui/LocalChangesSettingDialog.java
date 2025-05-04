package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;

public class LocalChangesSettingDialog extends SettingDialog{
	private LocalChangesDialogProvider provider;

	public LocalChangesSettingDialog(Project project, VirtualFile[] pathsToExport) {
		super(project, pathsToExport, null);
	}

	@Override
	protected void createFileListTree() {
		final JComponent changesViewPanel = getProviderComputeIfAbsent().createChangesViewPanel(this.project, this.selectedFiles);
		this.fileListPanel.add(changesViewPanel);
	}

	@Override
	protected void updateTemplateUiState() {
		// not allow to use template
		this.templateEnableCheckBox.setEnabled(false);
	}

	@Override
	public VirtualFile[] getExportingFiles() {
		return provider.getSelectExportFiles();
	}

	private LocalChangesDialogProvider getProviderComputeIfAbsent(){
		if (this.provider == null) {
			this.provider = new LocalChangesDialogProvider();
		}
		return this.provider;
	}

	@Override
	public void dispose() {
		disposeInEdt();
	}

	private void disposeInEdt(){
		ApplicationManager.getApplication().invokeAndWait(()->{
			provider.dispose();
			super.dispose();
		});
	}
}

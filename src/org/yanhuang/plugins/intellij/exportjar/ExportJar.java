package org.yanhuang.plugins.intellij.exportjar;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.yanhuang.plugins.intellij.exportjar.ui.Settings;

import java.awt.*;

public class ExportJar extends AnAction {


	@Override
	public void update(AnActionEvent event) {
		super.update(event);
		Presentation presentation = event.getPresentation();
		if (presentation.isEnabled()) {
			DataContext dataContext = event.getDataContext();
			Project project = (Project) CommonDataKeys.PROJECT.getData(dataContext);

			if (project == null) {
				presentation.setEnabled(false);
				presentation.setVisible(false);
				return;
			}

			VirtualFile[] virtualFiles = (VirtualFile[]) CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
			if (virtualFiles.length == 0) {
				presentation.setEnabled(false);
				presentation.setVisible(false);
				return;
			}

			String text = CompilerBundle.message("action.compile.description.selected.files", new Object[0]);
			presentation.setText(this.getButtonName(text), true);
			presentation.setEnabled(true);
		}
	}

	@Override
	public void actionPerformed(AnActionEvent event) {
		Settings setting = new Settings(event.getDataContext());
		setting.setResizable(false);
		setting.setSize(500, 200);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = setting.getSize();
		if (frameSize.height > screenSize.height) {
			frameSize.height = screenSize.height;
		}

		if (frameSize.width > screenSize.width) {
			frameSize.width = screenSize.width;
		}

		setting.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
		setting.setTitle(Constants.actionName);
		setting.setVisible(true);
	}

	private String getButtonName(String text) {
		return Constants.actionName + " " + text + "...";
	}

}

package org.yanhuang.plugins.intellij.exportjar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.ui.SettingDialog;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;

import java.awt.*;

/**
 * main entrance for export jar
 */
public class ExportJarAction extends AnAction {

	@Override
	public void update(@NotNull AnActionEvent event) {
		final Project project = event.getProject();
		boolean enable;
		event.getPresentation().setEnabledAndVisible((enable=(project != null)));
		if (enable){
			VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(event.getDataContext());
			event.getPresentation().setEnabledAndVisible((enable=(virtualFiles != null && virtualFiles.length > 0)));
		}
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent event) {
		SettingDialog setting = new SettingDialog(event.getDataContext());
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

}

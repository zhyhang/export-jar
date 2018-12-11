package org.yanhuang.plugins.intellij.exportjar;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.yanhuang.plugins.intellij.exportjar.ui.Settings;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;

import java.awt.*;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.*;

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

	private VirtualFile[] checkVirtualFiles(Project project, VirtualFile[] virtualFiles) {
		if (virtualFiles != null && virtualFiles.length != 0) {
			PsiManager psiManager = PsiManager.getInstance(project);
			CompilerConfiguration compilerConfiguration = CompilerConfiguration.getInstance(project);
			ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
			CompilerManager compilerManager = CompilerManager.getInstance(project);
			ArrayList<VirtualFile> validVfs = new ArrayList();
			int length = virtualFiles.length;

			for (int i = 0; i < length; ++i) {
				VirtualFile virtualFile = virtualFiles[i];
				if (projectFileIndex.isInSourceContent(virtualFile) && virtualFile.isInLocalFileSystem()) {
					if (virtualFile.isDirectory()) {
						PsiDirectory vfd = psiManager.findDirectory(virtualFile);
						if (vfd != null && JavaDirectoryService.getInstance().getPackage(vfd) != null) {
							validVfs.add(virtualFile);
						}
					} else {
						if (compilerManager.isCompilableFileType(virtualFile.getFileType()) ||
								compilerConfiguration.isCompilableResourceFile(project, virtualFile)) {
							validVfs.add(virtualFile);
						}
					}
				}
			}
			return VfsUtilCore.toVirtualFileArray(validVfs);
		} else {
			return VirtualFile.EMPTY_ARRAY;
		}
	}

}

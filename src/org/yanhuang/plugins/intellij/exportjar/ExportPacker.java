
package org.yanhuang.plugins.intellij.exportjar;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;
import org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExportPacker implements CompileStatusNotification {
	private DataContext dataContext;
	private Path exportJarFullPath;
	private Project project;

	public ExportPacker(DataContext dataContext, Path exportJarFullPath) {
		this.dataContext = dataContext;
		this.exportJarFullPath = exportJarFullPath;
		this.project = (Project) CommonDataKeys.PROJECT.getData(this.dataContext);
	}

	private void pack() throws Exception{
		MessagesUtils.clear(project);
		Module module = (Module) LangDataKeys.MODULE.getData(this.dataContext);
		//TODO final Module module = projectFileIndex.getModuleForFile(virtualFile);
		// get file's module

		String outPutPath = CompilerPathsEx.getModuleOutputPath(module, false);
		VirtualFile[] virtualFiles = (VirtualFile[]) CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(this.dataContext);

		Set<VirtualFile> allVfs = new HashSet();
		for (int i = 0; i < virtualFiles.length; ++i) {
			VirtualFile virtualFile = virtualFiles[i];
			CommonUtils.collectExportFiles(project, allVfs, virtualFile);
		}

		List<Path> filePaths = new ArrayList<>();
		List<String> jarEntryNames = new ArrayList<>();
		boolean hasError = false;
		for (VirtualFile vf : allVfs) {
			collectExportVirtualFile(filePaths, jarEntryNames, vf, outPutPath, true, true);
		}
		CommonUtils.createNewJar(project, exportJarFullPath, filePaths, jarEntryNames);
	}

	private void collectExportVirtualFile(List<Path> filePaths, List<String> jarEntryNames, VirtualFile virtualFile,
	                                      String targetPath, boolean exportSource, boolean exportClass) {
		// find package name
		PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile.isDirectory() ?
				virtualFile : virtualFile.getParent());
		PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
		String packagePath = psiPackage == null ? "" : psiPackage.getQualifiedName().replaceAll("\\.", "/");
		String fileName = virtualFile.getName();
		if (CompilerManager.getInstance(project).isCompilableFileType(virtualFile.getFileType())) {
			if (exportSource) {
				collectExportFile(filePaths, jarEntryNames, packagePath, Paths.get(virtualFile.getPath()));
			}
			// only export java classes
			if (exportClass && fileName.endsWith(".java")) {
				//lookup class and nested class files
				String outClassNamePrefix = fileName.substring(0, fileName.length() - 5);
				String outerClassName = outClassNamePrefix + ".class";
				String nestClassNamePrefix = outClassNamePrefix + "$";
				final Path classFilePath = Paths.get(targetPath).resolve(packagePath);
				try {
					Files.walk(classFilePath, 1).forEach(p -> {
						String className = p.getFileName().toString();
						if (className.equals(outerClassName) || className.endsWith(".class") && className.startsWith(nestClassNamePrefix)) {
							collectExportFile(filePaths, jarEntryNames, packagePath, p);
						}
					});
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			collectExportFile(filePaths, jarEntryNames, packagePath, Paths.get(virtualFile.getPath()));
		}
	}

	private void collectExportFile(List<Path> filePaths, List<String> jarEntryNames, String packagePath,
	                               Path filePath) {
		filePaths.add(filePath);
		String normalPackagePath = "".equals(packagePath) ? "" : packagePath.endsWith("/") ? packagePath :
				packagePath + "/";
		jarEntryNames.add(normalPackagePath + filePath.getFileName());
	}

	@Override
	public void finished(boolean b, int error, int i1, CompileContext compileContext) {
		if (error == 0) {
			try {
				this.pack();
			} catch (Exception e) {
				StringWriter dmsg = new StringWriter();// 详细信息
				PrintWriter pw = new PrintWriter(dmsg);
				e.printStackTrace(pw);
				MessagesUtils.error(project, dmsg.toString());
				MessagesUtils.errorNotify(Constants.actionName + " status", "export jar error");
			}
			MessagesUtils.info(project, exportJarFullPath +" complete export successfully");
			MessagesUtils.infoNotify(Constants.actionName + " status", exportJarFullPath + "<br> complete " +
					"export successfully");
		} else {
			MessagesUtils.error(project, "compile error");
			MessagesUtils.infoNotify(Constants.actionName + " status", "compile error");
		}
	}


}

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
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
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

/**
 * after compile successfully, do pack file and export to jar
 */
public class ExportPacker implements CompileStatusNotification {
	private DataContext dataContext;
	private Path exportJarFullPath;
	private Project project;
	private boolean exportJava;
	private boolean exportClass;
	private boolean exportTests;

	public ExportPacker(DataContext dataContext, Path exportJarFullPath, boolean exportJava, boolean exportClass,
	                    boolean exportTests) {
		this.dataContext = dataContext;
		this.exportJarFullPath = exportJarFullPath;
		this.exportClass = exportClass;
		this.exportJava = exportJava;
		this.exportTests = exportTests;
		this.project = CommonDataKeys.PROJECT.getData(this.dataContext);
	}

	private void pack() throws Exception {
		MessagesUtils.clear(project);
		Module module = LangDataKeys.MODULE.getData(this.dataContext);
		VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(this.dataContext);

		Set<VirtualFile> allVfs = new HashSet();
		for (int i = 0; i < virtualFiles.length; ++i) {
			VirtualFile virtualFile = virtualFiles[i];
			CommonUtils.collectExportFilesNest(project, allVfs, virtualFile);
		}

		List<Path> filePaths = new ArrayList<>();
		List<String> jarEntryNames = new ArrayList<>();
		for (VirtualFile vf : allVfs) {
			collectExportVirtualFile(filePaths, jarEntryNames, vf);
		}
		CommonUtils.createNewJar(project, exportJarFullPath, filePaths, jarEntryNames);
	}

	private void collectExportVirtualFile(List<Path> filePaths, List<String> jarEntryNames, VirtualFile virtualFile) {
		final boolean inTestSourceContent =
				ProjectRootManager.getInstance(project).getFileIndex().isInTestSourceContent(virtualFile);
		if (inTestSourceContent && !exportTests) { // not export test source and resource files
			return;
		}
		// find package name
		PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile.isDirectory() ?
				virtualFile : virtualFile.getParent());
		PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
		String packagePath = psiPackage == null ? "" : psiPackage.getQualifiedName().replaceAll("\\.", "/");
		String fileName = virtualFile.getName();
		if (CompilerManager.getInstance(project).isCompilableFileType(virtualFile.getFileType())) {
			if (exportJava) {
				collectExportFile(filePaths, jarEntryNames, packagePath, Paths.get(virtualFile.getPath()));
			}
			// only export java classes
			if (psiPackage != null && exportClass && fileName.endsWith(".java")) {
				//lookup class and nested class files
				final Set<String> localClassNames = findLocalClassName(psiPackage.getClasses(), virtualFile);
				String outClassNamePrefix = fileName.substring(0, fileName.length() - 5);
				String outerClassName = outClassNamePrefix + ".class";
				String nestClassNamePrefix = outClassNamePrefix + "$";
				ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
				final Module module = projectFileIndex.getModuleForFile(virtualFile);
				if (module == null) {
					throw new RuntimeException("not found module info of file " + virtualFile.getName());
				}
				String outPutPath = null;
				if (inTestSourceContent) {
					outPutPath = CompilerPathsEx.getModuleOutputPath(module, true);
				} else {
					outPutPath = CompilerPathsEx.getModuleOutputPath(module, false);
				}
				final Path classFilePath = Paths.get(outPutPath).resolve(packagePath);
				try {
					Files.walk(classFilePath, 1).forEach(p -> {
						String className = p.getFileName().toString();
						if (className.equals(outerClassName) || className.endsWith(".class") && className.startsWith(nestClassNamePrefix)
								|| localClassNames.contains(className)) {
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

	/**
	 * find local class name define in one java file
	 *
	 * @param classes
	 * @param javaFile
	 * @return
	 */
	private Set<String> findLocalClassName(PsiClass[] classes, VirtualFile javaFile) {
		Set<String> localClasses = new HashSet<>();
		for (PsiClass psic : classes) {
			if (psic.getSourceElement().getContainingFile().getVirtualFile().equals(javaFile)) {
				localClasses.add(psic.getName() + ".class");
			}
		}
		return localClasses;
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
				return;
			}
			MessagesUtils.info(project, exportJarFullPath + " complete export successfully");
			MessagesUtils.infoNotify(Constants.actionName + " status", exportJarFullPath + "<br> complete " +
					"export successfully");
		} else {
			MessagesUtils.error(project, "compile error");
			MessagesUtils.infoNotify(Constants.actionName + " status", "compile error");
		}
	}


}

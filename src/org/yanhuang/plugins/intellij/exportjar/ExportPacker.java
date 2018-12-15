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
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
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

import static com.intellij.psi.impl.compiled.ClsFileImpl.EMPTY_ATTRIBUTES;

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

	private void pack() {
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
				String fileNameWithoutJava = fileName.substring(0, fileName.length() - ".java".length());
				final Set<String> localClassNames = findLocalClassName(psiPackage.getClasses(), virtualFile);
				ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
				final Module module = projectFileIndex.getModuleForFile(virtualFile);
				if (module == null) {
					throw new RuntimeException("not found module info of file " + virtualFile.getName());
				}
				String outPutPath;
				if (inTestSourceContent) {
					outPutPath = CompilerPathsEx.getModuleOutputPath(module, true);
				} else {
					outPutPath = CompilerPathsEx.getModuleOutputPath(module, false);
				}
				//find inner class
				final Path classFileBasePath = Paths.get(outPutPath).resolve(packagePath);
				Set<String> offspringClassNames = new HashSet<>();
				for (String localClassName : localClassNames) {
					findOffspringClassName(offspringClassNames, classFileBasePath.resolve(localClassName + ".class"));
				}
				try {
					Files.walk(classFileBasePath, 1).forEach(p -> {
						String classFileName = p.getFileName().toString();
						if (!classFileName.endsWith(".class")) {
							return;
						}
						String className = classFileName.substring(0, classFileName.length() - ".class".length());
						if (localClassNames.contains(className)) {
							collectExportFile(filePaths, jarEntryNames, packagePath, p);
						} else if (offspringClassNames.contains(className)) {
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
	 * @param classes  psi classes in the package
	 * @param javaFile current java file
	 * @return Class name set includes name of the class their source code in the java file
	 */
	private Set<String> findLocalClassName(PsiClass[] classes, VirtualFile javaFile) {
		Set<String> localClasses = new HashSet<>();
		for (PsiClass psiClass : classes) {
			if (psiClass.getSourceElement().getContainingFile().getVirtualFile().equals(javaFile)) {
				localClasses.add(psiClass.getName());
			}
		}
		return localClasses;
	}

	private void findOffspringClassName(Set<String> offspringClassNames, Path ancestorClassFile) {
		try {
			ClassReader reader = new ClassReader(Files.readAllBytes(ancestorClassFile));
			final String ancestorClassName = reader.getClassName();
			reader.accept(new ClassVisitor(Opcodes.API_VERSION) {
				@Override
				public void visitInnerClass(String name, String outer, String inner, int access) {
					final int indexSplash = name.lastIndexOf('/');
					String className = indexSplash >= 0 ? name.substring(indexSplash + 1) : name;
					if (offspringClassNames.contains(className) || !name.startsWith(ancestorClassName)) {
						return;
					}
					offspringClassNames.add(className);
					Path innerClassPath = ancestorClassFile.getParent().resolve(className + ".class");
					findOffspringClassName(offspringClassNames, innerClassPath);
				}
			}, EMPTY_ATTRIBUTES, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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

package org.yanhuang.plugins.intellij.exportjar;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils.*;

/**
 * when compiled successfully, pack file and export to jar
 */
public class ExportPacker implements CompileStatusNotification {
    private final Project project;
    private final VirtualFile[] selectedFiles;
    private final Path exportJarFullPath;
    private final boolean exportJava;
    private final boolean exportClass;
    private final boolean exportTests;

    public ExportPacker(Project project, VirtualFile[] selectedFiles, Path exportJarFullPath, boolean exportJava, boolean exportClass, boolean exportTests) {
        this.project = project;
        this.selectedFiles = selectedFiles;
        this.exportJarFullPath = exportJarFullPath;
        this.exportClass = exportClass;
        this.exportJava = exportJava;
        this.exportTests = exportTests;
    }

    private void pack() {
        clear(project);
        VirtualFile[] virtualFiles = this.selectedFiles;
        if (virtualFiles == null) {
            virtualFiles = new VirtualFile[0];
        }
        Set<VirtualFile> allVfs = new HashSet<>();
        for (VirtualFile virtualFile : virtualFiles) {
            CommonUtils.collectExportFilesNest(project, allVfs, virtualFile);
        }
        List<Path> filePaths = new ArrayList<>();
        Map<Path, VirtualFile> filePathVfMap = new HashMap<>();
        List<String> jarEntryNames = new ArrayList<>();
        for (VirtualFile vf : allVfs) {
            final int startIndex = filePaths.size();
            collectExportVirtualFile(filePaths, jarEntryNames, vf);
            for (int i = startIndex; i < filePaths.size(); i++) {
                filePathVfMap.put(filePaths.get(i), vf);
            }
        }
        CommonUtils.createNewJar(project, exportJarFullPath, filePaths, jarEntryNames, filePathVfMap);
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
                PsiClass[] psiClasses = psiPackage.getClasses();
                if (psiClasses.length == 0) {
                    warn(project, "not found class info of java file " + virtualFile.getPath());
                    return;// possible only package-info.java or module-info.java in the package, ignore them
                }
                final Set<String> localClassNames = CommonUtils.findClassNameDefineIn(psiClasses, virtualFile);
                ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
                final Module module = projectFileIndex.getModuleForFile(virtualFile);
                if (module == null) {
                    throw new RuntimeException("not found module info of file " + virtualFile.getName());
                }
                String outPutPath;
                if (inTestSourceContent) {
                    outPutPath = CompilerPaths.getModuleOutputPath(module, true);
                } else {
                    outPutPath = CompilerPaths.getModuleOutputPath(module, false);
                }
                if (outPutPath == null) {
                    throw new RuntimeException("not found module " + module.getName() + " output path");
                }
                //find inner class
                final Path classFileBasePath = Paths.get(outPutPath).resolve(packagePath);
                Set<String> offspringClassNames = new HashSet<>();
                for (String localClassName : localClassNames) {
                    CommonUtils.findOffspringClassName(offspringClassNames,
                            classFileBasePath.resolve(localClassName + ".class"));
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

    @Override
    public void finished(boolean b, int error, int i1, @NotNull CompileContext compileContext) {
        if (error == 0) {
            try {
                this.pack();
            } catch (Exception e) {
                error(project, stackInfo(e));
                errorNotify(Constants.actionName + " status", "export jar error, detail in the messages" +
                        " " +
                        "tab");
                return;
            }
            info(project, exportJarFullPath + " complete export successfully");
            infoNotify(Constants.actionName + " status", exportJarFullPath + "<br> complete " +
                    "export successfully");
        } else {
            error(project, "compile error");
            infoNotify(Constants.actionName + " status", "compile error, detail in the messages tab");
        }
    }

}

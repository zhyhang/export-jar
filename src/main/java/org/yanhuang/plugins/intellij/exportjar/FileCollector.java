package org.yanhuang.plugins.intellij.exportjar;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import org.yanhuang.plugins.intellij.exportjar.model.ExportOptions;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils.warn;

/**
 * Collects the concrete files (sources, compiled classes, resources, webapp files)
 * that should be packed into the export JAR, based on the selected virtual files
 * and the chosen {@link ExportOptions}.
 * <p>
 * Produces index-aligned filePaths/entryNames lists plus a path→VirtualFile map
 * (used by the caller for per-entry progress notification).
 */
public class FileCollector {

    /** Collected, index-aligned file paths and jar entry names. */
    public static class Result {
        private final List<Path> filePaths = new ArrayList<>();
        private final List<String> entryNames = new ArrayList<>();
        private final Map<Path, VirtualFile> filePathVfMap = new HashMap<>();

        public List<Path> getFilePaths() {
            return filePaths;
        }

        public List<String> getEntryNames() {
            return entryNames;
        }

        public Map<Path, VirtualFile> getFilePathVfMap() {
            return filePathVfMap;
        }
    }

    private final Project project;
    private final Set<ExportOptions> options;

    public FileCollector(Project project, Set<ExportOptions> options) {
        this.project = project;
        this.options = options;
    }

    /**
     * Collect all exportable files reachable from the selected virtual files.
     *
     * @param selectedFiles selected virtual files (may be null)
     * @return collection result
     */
    public Result collect(VirtualFile[] selectedFiles) {
        final Result result = new Result();
        final VirtualFile[] roots = selectedFiles == null ? new VirtualFile[0] : selectedFiles;
        final Set<VirtualFile> allVfs = new HashSet<>();
        for (VirtualFile virtualFile : roots) {
            CommonUtils.collectExportFilesNest(project, allVfs, virtualFile);
        }
        for (VirtualFile vf : allVfs) {
            final int startIndex = result.filePaths.size();
            collectExportVirtualFile(result.filePaths, result.entryNames, vf);
            for (int i = startIndex; i < result.filePaths.size(); i++) {
                result.filePathVfMap.put(result.filePaths.get(i), vf);
            }
        }
        if (options.contains(ExportOptions.add_directory)) {
            addDirectoryEntries(result.filePaths, result.entryNames);
        }
        return result;
    }

    private void addDirectoryEntries(List<Path> filePaths, List<String> entryNames) {
        final var added = new HashSet<String>();
        final int size = entryNames.size();
        for (int i = 0; i < size; i++) {
            addDirectoryEntry(filePaths, entryNames, entryNames.get(i), added);
        }
    }

    private void addDirectoryEntry(List<Path> filePaths, List<String> entryNames, String entryName, HashSet<String> added) {
        int spIndex = entryName.indexOf("/");
        while (spIndex > 0) {
            final String dir = entryName.substring(0, spIndex + 1);
            if (added.add(dir)) {
                filePaths.add(null);
                entryNames.add(dir);
            }
            spIndex = entryName.indexOf("/", spIndex + 1);
        }
    }

    private void collectExportVirtualFile(List<Path> filePaths, List<String> jarEntryNames, VirtualFile virtualFile) {
        final boolean inTestSourceContent = ProjectRootManager.getInstance(project).getFileIndex().isInTestSourceContent(virtualFile);

        if (inTestSourceContent && !options.contains(ExportOptions.export_test)) {
            return;
        }

        if (CommonUtils.isWebAppExportFile(project, virtualFile)) {
            String entryPath = CommonUtils.toWebAppEntryName(project, virtualFile);
            if (entryPath != null) {
                collectExportFile(filePaths, jarEntryNames, "", Paths.get(virtualFile.getPath()), entryPath);
            }
            return;
        }

        PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile.isDirectory() ? virtualFile : virtualFile.getParent());
        PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
        String packagePath = psiPackage == null ? "" : psiPackage.getQualifiedName().replaceAll("\\.", "/");
        String fileName = virtualFile.getName();

        if (CompilerManager.getInstance(project).isCompilableFileType(virtualFile.getFileType())) {
            if (options.contains(ExportOptions.export_java)) {
                collectExportFile(filePaths, jarEntryNames, packagePath, Paths.get(virtualFile.getPath()));
            }
            // only export java classes
            if (psiPackage != null && options.contains(ExportOptions.export_class) && isExportClassSourceFile(fileName)) {
                PsiClass[] psiClasses = psiPackage.getClasses();
                if (psiClasses.length == 0) {
                    warn(project, "not found class info of source file " + virtualFile.getPath());
                    return;// possible only package-info.java or module-info.java in the package, ignore them
                }
                final Set<String> localClassNames = CommonUtils.findClassNameDefineIn(psiClasses, virtualFile);
                ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
                final Module module = projectFileIndex.getModuleForFile(virtualFile);
                if (module == null) {
                    throw new ExportJarException("not found module info of file " + virtualFile.getName());
                }
                String outPutPath;
                if (inTestSourceContent) {
                    outPutPath = CompilerPaths.getModuleOutputPath(module, true);
                } else {
                    outPutPath = CompilerPaths.getModuleOutputPath(module, false);
                }
                if (outPutPath == null) {
                    throw new ExportJarException("not found module " + module.getName() + " output path");
                }
                //find inner class
                final Path classFileBasePath = Paths.get(outPutPath).resolve(packagePath);
                Set<String> offspringClassNames = new HashSet<>();
                for (String localClassName : localClassNames) {
                    CommonUtils.findOffspringClassName(offspringClassNames, classFileBasePath.resolve(localClassName + ".class"));
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
                    throw new ExportJarException(e);
                }
            }
        } else {
            collectExportFile(filePaths, jarEntryNames, packagePath, Paths.get(virtualFile.getPath()));
        }
    }

    private boolean isExportClassSourceFile(String fileName) {
        return fileName.endsWith(".java") || fileName.endsWith(".kt");
    }

    private void collectExportFile(List<Path> filePaths, List<String> jarEntryNames, String packagePath, Path filePath, String entryName) {
        filePaths.add(filePath);
        String normalEntryName = entryName == null ? "" : entryName;
        if (entryName == null) {
            String normalPackagePath = "".equals(packagePath) ? "" : packagePath.endsWith("/") ? packagePath : packagePath + "/";
            normalEntryName = normalPackagePath + filePath.getFileName();
        }
        jarEntryNames.add(normalEntryName);
    }

    private void collectExportFile(List<Path> filePaths, List<String> jarEntryNames, String packagePath, Path filePath) {
        collectExportFile(filePaths, jarEntryNames, packagePath, filePath, null);
    }
}

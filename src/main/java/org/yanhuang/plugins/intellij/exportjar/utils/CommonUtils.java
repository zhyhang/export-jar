package org.yanhuang.plugins.intellij.exportjar.utils;

import com.google.gson.Gson;
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.org.objectweb.asm.Attribute;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class CommonUtils {

    private static int versionOpcodes;

    static {
        try {
            final Field apiVersion = Opcodes.class.getField("API_VERSION");
            versionOpcodes = (int) apiVersion.get(null);
        } catch (Exception e) {
            versionOpcodes = Opcodes.API_VERSION;
        }
    }

    public static String toJson(Object obj) {
        return new Gson().toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> cls) {
        return new Gson().fromJson(json, cls);
    }


    /**
     * collect recursively all files (filter by isValidExport) of root dir and it's descendants.
     * @param project project file in
     * @param collected saved collected files
     * @param parentVf root dir
     */
    public static void collectExportFilesNest(Project project, Set<VirtualFile> collected, VirtualFile parentVf) {
        if (!parentVf.isDirectory() && isValidExport(project, parentVf)) {
            collected.add(parentVf);
        }
        //try to use com.intellij.openapi.vfs.VfsUtilCore.visitChildrenRecursively
        final VirtualFile[] children = parentVf.getChildren();
        for (VirtualFile child : children) {
            if (child.isDirectory()) {
                collectExportFilesNest(project, collected, child);
            } else if (isValidExport(project, child)) {
                collected.add(child);
            }
        }
    }

    /**
     * Recursively collects all files (not filter) under the given directory.
     *
     * @param parentVf the directory to start the search from.
     * @return a collection of VirtualFiles representing all files under the directory. If parentVf is file, return it only.
     */
    public static Collection<VirtualFile> collectFilesNest(VirtualFile parentVf) {
        final Collection<VirtualFile> files = new ArrayList<>();
        if (parentVf == null) {
            return files;
        }
        VfsUtil.visitChildrenRecursively(parentVf, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory()) {
                    files.add(file);
                }
                return true;
            }
        });
        return files;
    }

    /**
     * Collects the child files from a given parent VirtualFile.
     *
     * @param parentVf The parent VirtualFile from which to collect child files.
     * @return A collection of VirtualFile objects representing the child files of the parent VirtualFile.
     *         If the parentVf is null, an empty list is returned.
     *         If the parentVf is not a directory, a list containing only the parentVf is returned.
     *         Otherwise, a list of child files (non-directory files) is returned.
     */
    public static Collection<VirtualFile> collectChildFiles(VirtualFile parentVf) {
        if (parentVf == null) {
            return List.of();
        }
        if (!parentVf.isDirectory()) {
            return List.of(parentVf);
        }else{
            return Arrays.stream(parentVf.getChildren()).filter(Predicate.not(VirtualFile::isDirectory)).collect(Collectors.toList());
        }
    }

    public static void createNewJar(Project project, Path jarFileFullPath, List<Path> filePaths,
                                    List<String> entryNames, Map<Path, VirtualFile> filePathVfMap) {
        final Manifest manifest = new Manifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "2.5.0");
        // remove according https://github.com/zhyhang/export-jar/issues/15
        // mainAttributes.put(new Attributes.Name("Created-By"), Constants.creator);
        try (OutputStream os = Files.newOutputStream(jarFileFullPath);
             BufferedOutputStream bos = new BufferedOutputStream(os);
             JarOutputStream jos = new JarOutputStream(bos, manifest)) {
            for (int i = 0; i < entryNames.size(); i++) {
                String entryName = entryNames.get(i);
                JarEntry je = new JarEntry(entryName);
                Path filePath = filePaths.get(i);
                jos.putNextEntry(je);
                if (filePath != null && Files.isRegularFile(filePath)) {
                    // using origin entry(file) last modified time
                    je.setLastModifiedTime(Files.getLastModifiedTime(filePath));
                    jos.write(Files.readAllBytes(filePath));
                }
                jos.closeEntry();
                VirtualFile vf = filePathVfMap.get(filePath);
                if (vf != null) {
                    MessagesUtils.infoAndMore(project, "packed " + filePath + " to jar", vf);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * check selected file can export
     *
     * @param project     project object
     * @param virtualFile selected file
     * @return true can export, false can not export
     */
    private static boolean isValidExport(Project project, VirtualFile virtualFile) {
        PsiManager psiManager = PsiManager.getInstance(project);
        CompilerConfiguration compilerConfiguration = CompilerConfiguration.getInstance(project);
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        CompilerManager compilerManager = CompilerManager.getInstance(project);
        return isValidExport(project, virtualFile, projectFileIndex, psiManager, compilerManager, compilerConfiguration);
    }

    private static boolean isValidExport(Project project, VirtualFile virtualFile, ProjectFileIndex projectFileIndex, PsiManager psiManager, CompilerManager compilerManager, CompilerConfiguration compilerConfiguration) {
        if (projectFileIndex.isInSourceContent(virtualFile) && virtualFile.isInLocalFileSystem()) {
            if (virtualFile.isDirectory()) {
                PsiDirectory vfd = psiManager.findDirectory(virtualFile);
                return vfd != null && JavaDirectoryService.getInstance().getPackage(vfd) != null;
            } else {
                return compilerManager.isCompilableFileType(virtualFile.getFileType()) ||
                        compilerConfiguration.isCompilableResourceFile(project, virtualFile);
            }
        }
        return false;
    }

    /**
     * lookup modules from data context
     *
     * @param context
     * @return
     */
    public static Module[] findModule(DataContext context) {
        Project project = CommonDataKeys.PROJECT.getData(context);
        Module[] modules = LangDataKeys.MODULE_CONTEXT_ARRAY.getData(context);
        if (modules == null) {
            Module module = LangDataKeys.MODULE.getData(context);
            if (module != null) {
                return new Module[]{module};
            }
            VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(context);
            if (virtualFiles == null || virtualFiles.length == 0) {
                return null;
            }
            return findModule(project, virtualFiles);
        } else {
            return modules;
        }
    }

    /**
     * find modules where virtual files locating in
     *
     * @param project      project
     * @param virtualFiles selected virtual files
     * @return modules, or null
     */
    public static Module[] findModule(Project project, VirtualFile[] virtualFiles) {
        if (virtualFiles == null || virtualFiles.length == 0) {
            return null;
        }
        Set<Module> ms = new HashSet<>();
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        for (VirtualFile virtualFile : virtualFiles) {
            final Module m = projectFileIndex.getModuleForFile(virtualFile);
            if (m != null) {
                ms.add(m);
            }
        }
        return ms.toArray(new Module[0]);
    }

    /**
     * find class name define in one java file, not including inner class and anonymous class
     *
     * @param classes  psi classes in the package
     * @param javaFile current java file
     * @return class name set includes name of the class their source code in the java file
     */
    public static Set<String> findClassNameDefineIn(PsiClass[] classes, VirtualFile javaFile) {
        Set<String> localClasses = new HashSet<>();
        for (PsiClass psiClass : classes) {
            if (psiClass.getSourceElement().getContainingFile().getVirtualFile().equals(javaFile)) {
                localClasses.add(psiClass.getName());
            }
        }
        return localClasses;
    }

    /**
     * find inner classes and anonymous classes belong to the ancestor class
     * <li>nested call to read the class file, to parse inner classes and anonymous classes</li>
     *
     * @param offspringClassNames store of found class name
     * @param ancestorClassFile   the ancestor class file full path
     */
    public static void findOffspringClassName(@NotNull Set<String> offspringClassNames, Path ancestorClassFile) {
        try {
            ClassReader reader = new ClassReader(Files.readAllBytes(ancestorClassFile));
            final String ancestorClassName = reader.getClassName();
            reader.accept(new ClassVisitor(versionOpcodes) {
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
            }, new Attribute[0], ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static VirtualFile fromOsFile(String osFilePath) {
        return LocalFileSystem.getInstance().findFileByPathIfCached(osFilePath);
    }

    public static Path toOsFile(VirtualFile virtualFile) {
        return Path.of(virtualFile.getPath());
    }

}

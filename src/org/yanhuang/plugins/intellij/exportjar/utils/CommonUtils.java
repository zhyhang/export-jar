package org.yanhuang.plugins.intellij.exportjar.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class CommonUtils {

	private static final ObjectMapper om = new ObjectMapper();

	static {
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		om.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public static String toJson(Object obj) {
		if (obj == null) {
			return null;
		}
		try {
			return om.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T fromJson(String json, Class<T> cls) {
		if (json == null || json.trim().isEmpty()) {
			return null;
		}
		try {
			return om.readValue(json, cls);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void collectExportFilesNest(Project project, Set<VirtualFile> collected, VirtualFile parentVf) {
		if (!parentVf.isDirectory() && isValidExport(project, parentVf)) {
			collected.add(parentVf);
		}
		final VirtualFile[] children = parentVf.getChildren();
		for (VirtualFile child : children) {
			if (child.isDirectory()) {
				collectExportFilesNest(project, collected, child);
			} else if (isValidExport(project, child)) {
				collected.add(child);
			}
		}
	}

	public static void createNewJar(Project project, Path jarFileFullPath, List<Path> filePaths,
	                                List<String> entryNames) {
		final Manifest manifest = new Manifest();
		Attributes mainAttributes = manifest.getMainAttributes();
		mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		mainAttributes.put(new Attributes.Name("Created-By"), Constants.creator);
		try (OutputStream os = Files.newOutputStream(jarFileFullPath);
		     BufferedOutputStream bos = new BufferedOutputStream(os);
		     JarOutputStream jos = new JarOutputStream(bos, manifest)) {
			for (int i = 0; i < entryNames.size(); i++) {
				String entryName = entryNames.get(i);
				JarEntry je = new JarEntry(entryName);
				Path filePath = filePaths.get(i);
				// using origin entry(file) last modified time
				je.setLastModifiedTime(Files.getLastModifiedTime(filePath));
				jos.putNextEntry(je);
				if (!Files.isDirectory(filePath)) {
					jos.write(Files.readAllBytes(filePath));
				}
				jos.closeEntry();
				MessagesUtils.info(project, "packed " + filePath + " to jar");
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
		if (projectFileIndex.isInSourceContent(virtualFile) && virtualFile.isInLocalFileSystem()) {
			if (virtualFile.isDirectory()) {
				PsiDirectory vfd = psiManager.findDirectory(virtualFile);
				if (vfd != null && JavaDirectoryService.getInstance().getPackage(vfd) != null) {
					return true;
				}
			} else {
				if (compilerManager.isCompilableFileType(virtualFile.getFileType()) ||
						compilerConfiguration.isCompilableResourceFile(project, virtualFile)) {
					return true;
				}
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
			Set<Module> ms = new HashSet<>();
			ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
			for (VirtualFile virtualFile : virtualFiles) {
				final Module m = projectFileIndex.getModuleForFile(virtualFile);
				if (m != null) {
					ms.add(m);
				}
			}
			return ms.toArray(new Module[0]);
		} else {
			return modules;
		}
	}

}

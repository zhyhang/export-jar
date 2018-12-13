package org.yanhuang.plugins.intellij.exportjar.utils;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class CommonUtils {

	public static boolean matchFileNamingConventions(String fileName) {
		return fileName.matches("[^/\\\\<>*?|\"]+");
	}

	public static void iterateDirectory(Project project, Set<VirtualFile> allVfs, VirtualFile vf) {
		if (!vf.isDirectory()) {
			allVfs.add(vf);
		}
		final VirtualFile[] children = vf.getChildren();
		for (VirtualFile child : children) {
			if (child.isDirectory()) {
				iterateDirectory(project, allVfs, child);
			} else {
				allVfs.add(child);
			}
		}
	}

	public static void collectExportFiles(Project project, Set<VirtualFile> collected, VirtualFile parentVf) {
		if (!parentVf.isDirectory() && isValidExport(project, parentVf)) {
			collected.add(parentVf);
		}
		final VirtualFile[] children = parentVf.getChildren();
		for (VirtualFile child : children) {
			if (child.isDirectory()) {
				iterateDirectory(project, collected, child);
			} else if (isValidExport(project, child)) {
				collected.add(child);
			}
		}
	}

	public static String getTheSameStart(List<String> strings) {
		if (strings != null && strings.size() != 0) {
			int max = 888888;
			Iterator stringIterator = strings.iterator();

			while (stringIterator.hasNext()) {
				String string = (String) stringIterator.next();
				if (string.length() < max) {
					max = string.length();
				}
			}

			StringBuilder sb = new StringBuilder();
			HashSet set = new HashSet();

			for (int i = 0; i < max; ++i) {
				Iterator iterator = strings.iterator();

				while (iterator.hasNext()) {
					String string = (String) iterator.next();
					set.add(string.charAt(i));
				}

				if (set.size() != 1) {
					break;
				}

				sb.append(set.iterator().next());
				set.clear();
			}

			return sb.toString();
		} else {
			return "";
		}
	}

	private static int getMinorVersion(String vs) {
		int dashIndex = vs.lastIndexOf(95);
		if (dashIndex >= 0) {
			StringBuilder builder = new StringBuilder();

			for (int idx = dashIndex + 1; idx < vs.length(); ++idx) {
				char ch = vs.charAt(idx);
				if (!Character.isDigit(ch)) {
					break;
				}

				builder.append(ch);
			}

			if (builder.length() > 0) {
				try {
					return Integer.parseInt(builder.toString());
				} catch (NumberFormatException var5) {
					;
				}
			}
		}

		return 0;
	}

	public static String getJDKPath(Project project) {
		JavaSdkVersion sdkVersion = null;
		Sdk projectJdk = null;
		int sdkMinorVersion = 0;
		Set<Sdk> candidates = new HashSet();
		Sdk defaultSdk = ProjectRootManager.getInstance(project).getProjectSdk();
		if (defaultSdk != null && defaultSdk.getSdkType() instanceof JavaSdkType) {
			candidates.add(defaultSdk);
		}

		Module[] modules = ModuleManager.getInstance(project).getModules();
		int length = modules.length;

		for (int i = 0; i < length; ++i) {
			Module module = modules[i];
			Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
			if (sdk != null && sdk.getSdkType() instanceof JavaSdkType) {
				candidates.add(sdk);
			}
		}

		JavaSdk javaSdkType = JavaSdk.getInstance();
		Iterator sdkIterator = candidates.iterator();

		while (true) {
			while (true) {
				Sdk candidate;
				String vs;
				JavaSdkVersion candidateVersion;
				do {
					do {
						if (!sdkIterator.hasNext()) {
							Sdk internalJdk = JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
							if (projectJdk == null || sdkVersion == null || !sdkVersion.isAtLeast(JavaSdkVersion.JDK_1_6)) {
								projectJdk = internalJdk;
							}

							JavaSdkType projectJdkType = (JavaSdkType) projectJdk.getSdkType();
							return projectJdkType.getBinPath(projectJdk) + File.separator;


						}

						candidate = (Sdk) sdkIterator.next();
						vs = candidate.getVersionString();
					} while (vs == null);

					candidateVersion = javaSdkType.getVersion(vs);
				} while (candidateVersion == null);

				int candidateMinorVersion = getMinorVersion(vs);
				if (projectJdk == null) {
					sdkVersion = candidateVersion;
					sdkMinorVersion = candidateMinorVersion;
					projectJdk = candidate;
				} else {
					int result = candidateVersion.compareTo(sdkVersion);
					if (result > 0 || result == 0 && candidateMinorVersion > sdkMinorVersion) {
						sdkVersion = candidateVersion;
						sdkMinorVersion = candidateMinorVersion;
						projectJdk = candidate;
					}
				}
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


}

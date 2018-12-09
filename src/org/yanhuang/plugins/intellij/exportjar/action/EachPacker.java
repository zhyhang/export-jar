package org.yanhuang.plugins.intellij.exportjar.action;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;

public class EachPacker extends Packager {
    private DataContext dataContext;
    private String exportPath;

    public EachPacker(DataContext dataContext, String exportPath) {
        this.dataContext = dataContext;
        this.exportPath = exportPath;
    }

    @Override
    public void pack() {
        Project project = (Project) CommonDataKeys.PROJECT.getData(this.dataContext);
        Module module = (Module) LangDataKeys.MODULE.getData(this.dataContext);
        VirtualFile[] virtualFiles = (VirtualFile[]) CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(this.dataContext);
        String outPutPath = CompilerPathsEx.getModuleOutputPath(module, false);
        HashSet<VirtualFile> directories = new HashSet();
        VirtualFile[] files1 = virtualFiles;
        int length1 = virtualFiles.length;

        for (int i = 0; i < length1; ++i) {
            VirtualFile virtualFile = files1[i];
            CommonUtils.iterateDirectory(project, directories, virtualFile);
        }

        String jdkPath = CommonUtils.getJDKPath(project);
        Iterator iterator = directories.iterator();

        while (true) {
            PsiDirectory psiDirectory;
            do {
                if (!iterator.hasNext()) {
                    return;
                }

                VirtualFile directory = (VirtualFile) iterator.next();
                psiDirectory = PsiManager.getInstance(project).findDirectory(directory);
            } while (psiDirectory == null);

            PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
            StringBuilder command = new StringBuilder(jdkPath);
            command.append("jar");
            command.append(" cvf ");
            command.append(this.exportPath);
            command.append("/");
            command.append(psiPackage.getQualifiedName());
            command.append(".jar");
            File outPutDirectory = new File(outPutPath + "/" + psiPackage.getQualifiedName().replaceAll("\\.", "/"));
            if (outPutDirectory.exists()) {
                File[] files = outPutDirectory.listFiles();
                int length = files.length;

                for (int index = 0; index < length; ++index) {
                    File file = files[index];
                    if (file.isFile()) {
                        command.append(" -C ");
                        command.append(outPutPath);
                        command.append(" ");
                        command.append(psiPackage.getQualifiedName().replaceAll("\\.", "/"));
                        command.append("/");
                        command.append(file.getName());
                    }
                }
            }

            Messages.info(project, command.toString());

            try {
                Process process = Runtime.getRuntime().exec(command.toString());
                BufferedReader stream = new BufferedReader(new InputStreamReader(process.getInputStream(), System.getProperty("sun.jnu.encoding", Charset.defaultCharset().name())));
                Messages.info(project, Charset.defaultCharset().name());
                String str;
                while ((str = stream.readLine()) != null) {
                    Messages.info(project, str);
                }
            } catch (Exception var17) {
                var17.printStackTrace();
            }
        }
    }

    @Override
    public void finished(boolean b, int error, int i1, CompileContext compileContext) {
        if (error == 0) {
            this.pack();
        } else {
            Project project = (Project) CommonDataKeys.PROJECT.getData(this.dataContext);
            Messages.info(project, "compile error");
        }

    }
}

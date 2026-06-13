package org.yanhuang.plugins.intellij.exportjar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.model.ExportOptions;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;
import org.yanhuang.plugins.intellij.exportjar.utils.action.CopyTextToClipboardAction;
import org.yanhuang.plugins.intellij.exportjar.utils.action.ShowInExplorerAction;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.yanhuang.plugins.intellij.exportjar.utils.MessagesUtils.*;

/**
 * when compiled successfully, pack file and export to jar
 */
public class ExportPacker implements CompileStatusNotification {
    private final Project project;
    private final VirtualFile[] selectedFiles;
    private final Path exportJarFullPath;
    private final Set<ExportOptions> exportOptionSet;

    public ExportPacker(Project project, VirtualFile[] selectedFiles, Path exportJarFullPath, ExportOptions[] exportOptions) {
        this.project = project;
        this.selectedFiles = selectedFiles;
        this.exportJarFullPath = exportJarFullPath;
        this.exportOptionSet = Arrays.stream(exportOptions).collect(Collectors.toSet());
    }

    private void pack() {
        clearMessagePanel(project);
        final FileCollector.Result collected = new FileCollector(project, exportOptionSet).collect(selectedFiles);
        new JarWriter().write(exportJarFullPath, collected.getFilePaths(), collected.getEntryNames(),
                (filePath, entryName) -> {
                    final VirtualFile vf = collected.getFilePathVfMap().get(filePath);
                    if (vf != null) {
                        infoAndMore(project, "packed " + filePath + " to jar", vf);
                    }
                });
    }

    @Override
    public void finished(boolean b, int error, int i1, @NotNull CompileContext compileContext) {
        if (error == 0) {
            ApplicationManager.getApplication().runWriteAction(this::whenFinishSuccess);
        } else {
            error(project, "compile error");
            infoNotify(Constants.actionName + " status", "compile error, detail in the messages tab");
        }
    }

    private void whenFinishSuccess() {
        try {
            this.pack();
        } catch (Exception e) {
            error(project, stackInfo(e));
            errorNotify(Constants.actionName + " status", "export jar error, detail in the messages tab");
            return;
        }
        info(project, exportJarFullPath + " complete export successfully");
        infoNotify(Constants.actionName + " status", exportJarFullPath + "<br> complete export successfully", List.of(new CopyTextToClipboardAction(exportJarFullPath.toString()), new ShowInExplorerAction(exportJarFullPath)));

    }

}

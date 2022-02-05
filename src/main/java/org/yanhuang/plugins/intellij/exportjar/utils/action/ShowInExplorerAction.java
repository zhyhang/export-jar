package org.yanhuang.plugins.intellij.exportjar.utils.action;

import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * open the file in system file explorer
 */
public class ShowInExplorerAction extends AnAction {
    private final Path filePath;

    public ShowInExplorerAction(Path filePath) {
        super(Constants.actionNameExplorer);
        this.filePath = filePath;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (filePath != null && !Files.isDirectory(filePath)) {
            RevealFileAction.openFile(filePath.toFile());
        } else if (filePath != null) {
            RevealFileAction.openDirectory(filePath.toFile());
        }
    }
}

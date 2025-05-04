package org.yanhuang.plugins.intellij.exportjar;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.ui.SettingDialog;
import org.yanhuang.plugins.intellij.exportjar.ui.UIFactory;

/**
 * main entrance for export jar
 */
public class ExportJarAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final SettingDialog setting = UIFactory.createSettingDialog(event.getDataContext());
        ApplicationManager.getApplication().invokeAndWait(setting::show);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}

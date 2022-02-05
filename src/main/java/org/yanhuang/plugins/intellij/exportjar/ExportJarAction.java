package org.yanhuang.plugins.intellij.exportjar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.ui.SettingDialog;
import org.yanhuang.plugins.intellij.exportjar.ui.UIFactory;

/**
 * main entrance for export jar
 */
public class ExportJarAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        final Project project = event.getProject();
        boolean enable;
        event.getPresentation().setEnabledAndVisible((enable = (project != null)));
        if (enable) {
            VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(event.getDataContext());
            event.getPresentation().setEnabledAndVisible((enable = (virtualFiles != null && virtualFiles.length > 0)));
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        SettingDialog setting = UIFactory.createSettingDialog(event.getDataContext());
        setting.setVisible(true);
    }

}

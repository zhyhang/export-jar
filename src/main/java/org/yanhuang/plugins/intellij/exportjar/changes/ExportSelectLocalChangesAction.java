package org.yanhuang.plugins.intellij.exportjar.changes;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * perform export jar from local changes (i.e. vcs commits)，initial select specified changes.
 */
public class ExportSelectLocalChangesAction  extends ExportLocalChangesAction{
    @Override
    protected VirtualFile[] initialSelection(Project project, AnActionEvent e) {
        return CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
    }
}

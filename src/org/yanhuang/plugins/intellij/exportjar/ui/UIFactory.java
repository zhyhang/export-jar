package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.history.integration.ui.views.RevisionsList;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.SimpleTextAttributes;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Objects;

/**
 * factory for creating setting dialog
 */
public class UIFactory {

    /**
     * create and config setting-dialog, without visibility
     *
     * @return SettingDialog
     */
    public static SettingDialog createSettingDialog(DataContext dataContext) {
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        VirtualFile[] selectedFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
        return createSettingDialog(project, selectedFiles);
    }

    /**
     * create and config setting-dialog, without visibility
     *
     * @param project project
     * @return SettingDialog
     */
    public static SettingDialog createSettingDialog(Project project) {
        return createSettingDialog(project, null);
    }

    /**
     * create and config setting-dialog, without visibility
     *
     * @param project       project
     * @param selectedFiles selected files
     * @return SettingDialog
     */
    public static SettingDialog createSettingDialog(Project project, VirtualFile[] selectedFiles) {
        SettingDialog setting = new SettingDialog(project, selectedFiles);
        setting.getSettingPanel().setPreferredSize(Constants.settingPanelSize);
        setting.setResizable(true);
        setting.setSize(Constants.settingDialogSize);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = setting.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }

        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }

        setting.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        setting.setTitle(Constants.actionName);
        return setting;
    }

    /**
     * Example for checkbox tree render
     */
    public static class CheckTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {

        @Override
        public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            String text = "";
            if (value instanceof DefaultMutableTreeNode) {
                text = Objects.toString(((DefaultMutableTreeNode) value).getUserObject(), "");
            }
            getTextRenderer().setIcon(AllIcons.FileTypes.Java);
            getTextRenderer().append(text, SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES);
        }
    }

}

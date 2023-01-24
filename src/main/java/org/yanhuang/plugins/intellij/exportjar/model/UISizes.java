package org.yanhuang.plugins.intellij.exportjar.model;

import org.yanhuang.plugins.intellij.exportjar.utils.Constants;

import java.awt.*;

public class UISizes {
    public static final UISizes DEFAULT_UI = new UISizes();

    static {
        DEFAULT_UI.exportDialog= Constants.settingDialogSize;
        DEFAULT_UI.fileSettingSplitPanel = Constants.fileListSettingSplitPanelSize;
        DEFAULT_UI.fileSettingSplitRatio = 0.5f;
    }

    private Dimension exportDialog;
    private Dimension fileSettingSplitPanel;
    private float fileSettingSplitRatio;

    public Dimension getExportDialog() {
        return exportDialog;
    }

    public void setExportDialog(final Dimension exportDialog) {
        this.exportDialog = exportDialog;
    }

    public Dimension getFileSettingSplitPanel() {
        return fileSettingSplitPanel;
    }

    public void setFileSettingSplitPanel(final Dimension fileSettingSplitPanel) {
        this.fileSettingSplitPanel = fileSettingSplitPanel;
    }

    public float getFileSettingSplitRatio() {
        return fileSettingSplitRatio;
    }

    public void setFileSettingSplitRatio(final float fileSettingSplitRatio) {
        this.fileSettingSplitRatio = fileSettingSplitRatio;
    }
}

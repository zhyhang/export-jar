package org.yanhuang.plugins.intellij.exportjar.utils.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;

import java.awt.datatransfer.StringSelection;

/**
 * action copy text to clipboard
 */
public class CopyTextToClipboardAction extends AnAction {
    private final String text;
    private final boolean convertUnixLinSeparator;

    public CopyTextToClipboardAction(String text) {
        this(text, false);
    }

    public CopyTextToClipboardAction(String text, boolean convertUnixLinSeparator) {
        super(Constants.actionNameCopy);
        this.text = text;
        this.convertUnixLinSeparator = convertUnixLinSeparator;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (text != null) {
            if (convertUnixLinSeparator) {
                final String t = StringUtil.convertLineSeparators(text);
                CopyPasteManager.getInstance().setContents(new StringSelection(t));
            }else{
                CopyPasteManager.getInstance().setContents(new StringSelection(text));
            }
        }
    }
}

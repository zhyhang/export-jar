package org.yanhuang.plugins.intellij.exportjar.utils;

import com.intellij.compiler.impl.ProblemsViewPanel;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.*;
import com.intellij.util.ui.MessageCategory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MessagesUtils {

    /**
     * find or create if absent
     *
     * @param project current project
     * @return found message view panel(packing export jar), create if absent
     */
    private static ProblemsViewPanel messageViewPanel(Project project) {
        final MessageView messageView = MessageView.SERVICE.getInstance(project);
        ContentManager cm;
        try {
            cm = messageView.getContentManager();
        } catch (Exception ignored) {
            final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
            cm = toolWindow != null ? toolWindow.getContentManager() : null;
        }
        final Content existViewPanel = cm == null ? null : cm.findContent(Constants.infoTabName);
        if (existViewPanel != null) {
            return (ProblemsViewPanel) existViewPanel.getComponent();
        }
        ProblemsViewPanel viewPanel = new ProblemsViewPanel(project);
        if (cm != null) {
            Content content = ContentFactory.SERVICE.getInstance().createContent(viewPanel, Constants.infoTabName, true);
            cm.addContent(content);
            cm.setSelectedContent(content);
        }
        return viewPanel;
    }

    /**
     * clear message in message view panel
     *
     * @param project current panel
     */
    public static void clear(Project project) {
        if (ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW) != null) {
            ContentManagerUtil.cleanupContents(null, project, Constants.infoTabName);
        }
    }

    /**
     * show info message in message view panel
     *
     * @param project current project
     * @param string  message
     */
    public static void info(Project project, String string) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
        if (toolWindow != null) {
            toolWindow.activate(null, false);
        }
        messageViewPanel(project).addMessage(MessageCategory.INFORMATION, new String[]{string}, null, -1, -1, null);
    }

    /**
     * show error message in message view panel
     *
     * @param project current project
     * @param string  error message
     */
    public static void error(Project project, String string) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
        if (toolWindow != null) {
            toolWindow.activate(null, false);
        }
        messageViewPanel(project).addMessage(MessageCategory.ERROR, new String[]{string}, null, -1, -1, null);
    }

    /**
     * show warn message in message view panel
     *
     * @param project current project
     * @param string  warn message
     */
    public static void warn(Project project, String string) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
        if (toolWindow != null) {
            toolWindow.activate(null, false);
        }
        messageViewPanel(project).addMessage(MessageCategory.WARNING, new String[]{string}, null, -1, -1, null);
    }

    /**
     * show info notification popup
     *
     * @param title   title
     * @param message message showing in popup, can be html snippet
     */
    public static void infoNotify(String title, String message) {
        Notifications.Bus.notify(new Notification(Constants.actionName, title,
                message, NotificationType.INFORMATION));
    }

    /**
     * show error notification popup
     *
     * @param title   title
     * @param message message showing in popup, can be html snippet
     */
    public static void errorNotify(String title, String message) {
        Notifications.Bus.notify(new Notification(Constants.actionName, title,
                message, NotificationType.ERROR));
    }

    /**
     * throwable stack tracking print to string
     *
     * @param throwable exception
     * @return stack tracking info
     */
    public static String stackInfo(Throwable throwable) {
        StringWriter dmsg = new StringWriter();
        PrintWriter pw = new PrintWriter(dmsg);
        throwable.printStackTrace(pw);
        return dmsg.toString();
    }

}

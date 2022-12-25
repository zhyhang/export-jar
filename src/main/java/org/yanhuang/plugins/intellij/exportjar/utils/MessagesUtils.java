package org.yanhuang.plugins.intellij.exportjar.utils;

import com.intellij.compiler.impl.ProblemsViewPanel;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.MessageCategory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class MessagesUtils {

    /**
     * find or create if absent
     *
     * @param project current project
     * @return found message view panel(packing export jar), create if absent
     */
    private static ProblemsViewPanel messageViewPanel(Project project) {
        final MessageView messageView = getMessageView(project);
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
            Content content = getContentFactory().createContent(viewPanel, Constants.infoTabName, true);
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
     * @param text  message
     */
    public static void info(Project project, String text) {
        infoAndMore(project, text, null);
    }

    public static void infoAndMore(Project project, String text, VirtualFile file) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
        if (toolWindow != null) {
            toolWindow.activate(null, false);
        }
        messageViewPanel(project).addMessage(MessageCategory.INFORMATION, new String[]{text}, file, -1, -1, null);
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
        infoNotify(title, message, List.of());
    }

    /**
     * show info notification popup with actions
     * @param title title
     * @param message content
     * @param actions actions show in popup and event log window
     */
    public static void infoNotify(String title, String message, List<AnAction> actions) {
        final Notification notification = new Notification(Constants.actionName, title, message, NotificationType.INFORMATION);
        ContainerUtil.notNullize(actions).forEach(notification::addAction);
        Notifications.Bus.notify(notification);
    }


    /**
     * show error notification popup
     *
     * @param title   title
     * @param message message showing in popup, can be html snippet
     */
    public static void errorNotify(String title, String message) {
        errorNotify(title, message, List.of());
    }

    /**
     * show error notification popup
     * @param title title
     * @param message content
     * @param actions actions show in popup and event log window
     */
    public static void errorNotify(String title, String message, List<AnAction> actions) {
        final Notification notification = new Notification(Constants.actionName, title, message, NotificationType.ERROR);
        ContainerUtil.notNullize(actions).forEach(notification::addAction);
        Notifications.Bus.notify(notification);
    }


    /**
     * throwable stack tracking print to string
     *
     * @param throwable exception
     * @return stack tracking info
     */
    public static String stackInfo(Throwable throwable) {
        final StringWriter msg = new StringWriter();
        final PrintWriter pw = new PrintWriter(new StringWriter());
        throwable.printStackTrace(pw);
        return msg.toString();
    }

    /**
     * delegate get message view method to platform method for compatible with old version.
     * use com.intellij.ui.content.MessageView#getInstance(com.intellij.openapi.project.Project) when this plugin support mini version update to greater than 222.2680.4.
     * @param project project
     * @return message view
     */
    public static MessageView getMessageView(Project project){
        return project.getService(MessageView.class);
    }

    /**
     * delegate get ContentFactory method to platform method for compatible with old version.
     *  use com.intellij.ui.content.ContentFactory#getInstance() when this plugin support mini version update to greater than 222.2680.4.
     * @return ContentFactory
     */
    public static ContentFactory getContentFactory() {
        return ApplicationManager.getApplication().getService(ContentFactory.class);
    }

}

package org.yanhuang.plugins.intellij.exportjar.action;

import com.intellij.compiler.impl.ProblemsViewPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.MessageView;
import org.yanhuang.plugins.intellij.exportjar.Constants;

public class Messages {

	private static ProblemsViewPanel getInstance(Project project) {
		MessageView messageView = MessageView.SERVICE.getInstance(project);
		ProblemsViewPanel packMessages = null;
		for (Content content : messageView.getContentManager().getContents()) {
			if (Constants.infoTabName.equals(content.getTabName())) {
				packMessages = (ProblemsViewPanel) content.getComponent();
				break;
			}
		}
		if (packMessages != null) {
			return packMessages;
		}
		packMessages = new ProblemsViewPanel(project);
		Content content = ContentFactory.SERVICE.getInstance().createContent(packMessages, Constants.infoTabName, true);
		messageView.getContentManager().addContent(content);
		messageView.getContentManager().setSelectedContent(content);
		return packMessages;
	}

	public static void clear(Project project) {
		MessageView messageView = MessageView.SERVICE.getInstance(project);
		for (Content content : messageView.getContentManager().getContents()) {
			if (Constants.infoTabName.equals(content.getTabName())) {
				ProblemsViewPanel viewPanel = (ProblemsViewPanel) content.getComponent();
				viewPanel.close();
				break;
			}
		}
	}

	public static void info(Project project, String string) {
		ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
		if (toolWindow != null) {
			toolWindow.activate(null, false);
		}
		getInstance(project).addMessage(3, new String[]{string}, null, -1, -1, null);
	}
}

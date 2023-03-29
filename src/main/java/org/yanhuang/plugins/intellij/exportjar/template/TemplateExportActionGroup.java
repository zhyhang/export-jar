package org.yanhuang.plugins.intellij.exportjar.template;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yanhuang.plugins.intellij.exportjar.model.SettingHistory;
import org.yanhuang.plugins.intellij.exportjar.model.SettingTemplate;
import org.yanhuang.plugins.intellij.exportjar.settings.HistoryDao;
import org.yanhuang.plugins.intellij.exportjar.ui.SettingDialog;
import org.yanhuang.plugins.intellij.exportjar.ui.UIFactory;
import org.yanhuang.plugins.intellij.exportjar.utils.Constants;

import java.util.List;

/**
 * dynamic actions created from saved templates, every click action group will refresh child actions i.e. recreate
 * actions from saved templates.
 */
public class TemplateExportActionGroup extends ActionGroup {
	private final HistoryDao dao = new HistoryDao();

	@Override
	public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
		final Project project = e != null ? e.getProject() : null;
		if (project != null) {
			return createTemplateActions(project);
		} else {
			return new AnAction[0];
		}
	}

	private AnAction[] createTemplateActions(Project project) {
		final SettingHistory history = dao.readOrDefault();
		final List<SettingTemplate> templates = dao.getProjectTemplates(history, project.getName());
		return templates.stream().limit(Constants.maxTemplateExportActionSize)
				.map(t -> createTemplateAction(project, t)).toArray(AnAction[]::new);
	}

	private AnAction createTemplateAction(Project project, SettingTemplate template) {
		return new AnAction(template.getName(), template.getName(), null) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				final SettingDialog dialog = UIFactory.createSettingDialog(project, template.getName());
				dialog.setVisible(true);
			}
		};
	}

}

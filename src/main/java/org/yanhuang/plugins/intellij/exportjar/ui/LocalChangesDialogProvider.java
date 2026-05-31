package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ui.TreeActionsToolbarPanel;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

/**
 * Provides the local-changes view panel for the export dialog.
 * <p>
 * Owns an {@link ExportChangesTree} fed directly from {@link ChangeListManager} — no reflection,
 * no borrowing of {@code DefaultCommitChangeListDialog} or {@code MultipleLocalChangeListsBrowser}
 * (the latter is package-private and cannot be constructed from outside its package).
 * </p>
 */
public class LocalChangesDialogProvider {

    private ExportChangesTree changesTree;

    public JComponent createChangesViewPanel(@NotNull Project project, VirtualFile[] pathsToExport) {
        dispose(); // dispose old tree if any

        final ChangeListManager changeManager = ChangeListManager.getInstance(project);

        // Compute which changes fall under the export paths
        final var pathsToExportStream = pathsToExport == null ?
                stream(ProjectLevelVcsManager.getInstance(project).getAllVersionedRoots()).map(VcsUtil::getFilePath) :
                stream(pathsToExport).map(VcsUtil::getFilePath);
        final var changesToExport =
                pathsToExportStream.flatMap(it -> changeManager.getChangesIn(it).stream()).collect(Collectors.toList());

        // Determine which of those changes are in the default change list (pre-selected)
        final var defaultChanges = changeManager.getDefaultChangeList().getChanges();
        final var includeChanges = ContainerUtil.intersection(defaultChanges, changesToExport);

        // Compute unversioned files under the export paths (public API since 2020.3+)
        final List<FilePath> allUnversioned = changeManager.getUnversionedFilesPaths();
        final List<VirtualFile> exportRoots = pathsToExport == null ? List.of() : List.of(pathsToExport);
        final var includeUnversionedFiles = allUnversioned.stream()
                .filter(fp -> {
                    final VirtualFile vf = fp.getVirtualFile();
                    if (vf == null) return false;
                    // if no export roots specified, include all unversioned
                    if (exportRoots.isEmpty()) return true;
                    return exportRoots.stream().anyMatch(root -> VfsUtil.isAncestor(root, vf, false));
                })
                .collect(Collectors.toList());

        // Build the owned tree with all change lists + all unversioned (full view)
        final ExportChangesTree tree = new ExportChangesTree(project);
        final Collection<? extends ChangeList> allChangeLists = changeManager.getChangeLists();
        tree.setData(allChangeLists, allUnversioned);
        tree.rebuildTree();

        // Seed initial inclusion: changes in default list under export paths + unversioned under export paths
        tree.getInclusionModel().clearInclusion();
        tree.getInclusionModel().addInclusion(includeChanges);
        tree.getInclusionModel().addInclusion(includeUnversionedFiles);

        this.changesTree = tree;

        // Build the panel: toolbar at NORTH, scrollable tree at CENTER
        final DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(Separator.getInstance());
        final var groupByAction = ActionManager.getInstance().getAction("ChangesView.GroupBy");
        if (groupByAction != null) {
            actionGroup.add(groupByAction);
        }
        final ActionToolbar toolbar = ActionManager.getInstance()
                .createActionToolbar("ExportLocalChanges", actionGroup, true);
        toolbar.setTargetComponent(tree);
        final JPanel toolbarPanel = new TreeActionsToolbarPanel(toolbar, tree);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(toolbarPanel, BorderLayout.NORTH);
        panel.add(ScrollPaneFactory.createScrollPane(tree), BorderLayout.CENTER);
        return panel;
    }

    public VirtualFile[] getSelectExportFiles() {
        if (changesTree == null) {
            return new VirtualFile[0];
        }
        return changesTree.getIncludedVirtualFiles();
    }

    public void dispose() {
        if (changesTree != null) {
            changesTree = null;
        }
    }
}

package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ui.ChangesGroupingPolicyFactory;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;
import java.util.Collection;
import java.util.List;

/**
 * Owned ChangesTree for local-changes export view.
 * <p>
 * Replaces the former approach of borrowing a {@code MultipleLocalChangeListsBrowser} from
 * {@code DefaultCommitChangeListDialog} (which is package-private and cannot be constructed
 * directly from outside its package). This tree is fed directly from {@code ChangeListManager}
 * and renders changelist + unversioned nodes using the standard {@code TreeModelBuilder} API.
 * </p>
 */
public class ExportChangesTree extends ChangesTree {

    private Collection<? extends ChangeList> changeLists = List.of();
    private List<? extends FilePath> unversionedPaths = List.of();

    public ExportChangesTree(@NotNull Project project) {
        // showCheckboxes=true, highlightProblems=false
        super(project, true, false);
    }

    /**
     * Sets the change lists and unversioned file paths that this tree should display.
     * Call {@link #rebuildTree()} after setting these to refresh the view.
     *
     * @param changeLists      change lists from {@code ChangeListManager.getChangeLists()}
     * @param unversionedPaths unversioned file paths from {@code ChangeListManager.getUnversionedFilesPaths()}
     */
    public void setData(@NotNull Collection<? extends ChangeList> changeLists,
                        @NotNull List<? extends FilePath> unversionedPaths) {
        this.changeLists = changeLists;
        this.unversionedPaths = unversionedPaths;
    }

    @Override
    public void rebuildTree() {
        final ChangesGroupingPolicyFactory grouping = getGroupingSupport().getGrouping();
        final DefaultTreeModel model = new TreeModelBuilder(myProject, grouping)
                .setChangeLists(changeLists, false, null)
                .setUnversioned(unversionedPaths)
                .build();
        updateTreeModel(model);
    }

    /**
     * Returns the virtual files currently checked (included) in this tree.
     * Maps {@code Change} user objects via {@link Change#getVirtualFile()} and
     * {@code FilePath} user objects via {@link FilePath#getVirtualFile()}, filtering nulls.
     *
     * @return array of included virtual files; never null, may be empty
     */
    @NotNull
    public VirtualFile[] getIncludedVirtualFiles() {
        return getInclusionModel().getInclusion().stream()
                .map(obj -> {
                    if (obj instanceof Change) {
                        return ((Change) obj).getVirtualFile();
                    } else if (obj instanceof FilePath) {
                        return ((FilePath) obj).getVirtualFile();
                    }
                    return null;
                })
                .filter(vf -> vf != null)
                .toArray(VirtualFile[]::new);
    }
}

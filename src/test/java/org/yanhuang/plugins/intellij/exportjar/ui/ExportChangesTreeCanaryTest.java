package org.yanhuang.plugins.intellij.exportjar.ui;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.yanhuang.plugins.intellij.exportjar.ui.ExportChangesTree;
import org.yanhuang.plugins.intellij.exportjar.ui.LocalChangesDialogProvider;

import javax.swing.*;
import java.util.List;

/**
 * Canary test for the VCS local-changes path.
 * Verifies that ExportChangesTree and LocalChangesDialogProvider can be constructed and used
 * without reflection or borrowing internal platform dialogs.
 * If this test fails after an SDK upgrade, it signals that the owned-tree API has changed.
 */
public class ExportChangesTreeCanaryTest extends BasePlatformTestCase {

    public void testExportChangesTree_constructsWithoutException() {
        // ExportChangesTree must be constructable with just a Project
        ExportChangesTree tree = new ExportChangesTree(getProject());
        assertNotNull("ExportChangesTree should be constructable", tree);
    }

    public void testExportChangesTree_setDataAndRebuild_doesNotThrow() {
        ExportChangesTree tree = new ExportChangesTree(getProject());
        // setData with empty collections + rebuildTree must not throw
        tree.setData(List.of(), List.of());
        tree.rebuildTree();
        assertNotNull("tree model should be non-null after rebuildTree", tree.getModel());
    }

    public void testExportChangesTree_getIncludedVirtualFiles_emptyWhenNothingIncluded() {
        ExportChangesTree tree = new ExportChangesTree(getProject());
        tree.setData(List.of(), List.of());
        tree.rebuildTree();
        var files = tree.getIncludedVirtualFiles();
        assertNotNull(files);
        assertEquals("no files should be included initially", 0, files.length);
    }

    public void testLocalChangesDialogProvider_createPanel_doesNotThrow() {
        // LocalChangesDialogProvider must create a panel without reflection or dialog-borrow
        LocalChangesDialogProvider provider = new LocalChangesDialogProvider();
        JComponent panel = provider.createChangesViewPanel(getProject(), null);
        assertNotNull("createChangesViewPanel should return a non-null component", panel);
        provider.dispose();
    }

    public void testLocalChangesDialogProvider_getSelectExportFiles_emptyBeforeCreate() {
        LocalChangesDialogProvider provider = new LocalChangesDialogProvider();
        var files = provider.getSelectExportFiles();
        assertNotNull(files);
        assertEquals("no files before createChangesViewPanel", 0, files.length);
    }
}

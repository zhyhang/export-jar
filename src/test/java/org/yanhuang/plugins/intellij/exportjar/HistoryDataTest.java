package org.yanhuang.plugins.intellij.exportjar;

import org.junit.Test;
import org.yanhuang.plugins.intellij.exportjar.model.ExportJarInfo;

import static org.junit.Assert.*;

public class HistoryDataTest {

    private static ExportJarInfo makeJar(String path, long creation) {
        ExportJarInfo info = new ExportJarInfo();
        info.setPath(path);
        info.setCreateTime(creation);
        return info;
    }

    @Test
    public void testAddSavedJarInfoDedup() {
        HistoryData data = new HistoryData();
        ExportJarInfo jar1 = makeJar("/a/out.jar", 100L);
        ExportJarInfo jar2 = makeJar("/b/out.jar", 200L);

        data.addSavedJarInfo(jar1);
        data.addSavedJarInfo(jar2);

        assertEquals("should have 2 entries", 2, data.getSavedJarInfo().length);
    }

    @Test
    public void testAddSavedJarInfoNoDuplicatePath() {
        HistoryData data = new HistoryData();
        ExportJarInfo jar1 = makeJar("/a/out.jar", 100L);
        ExportJarInfo jar2 = makeJar("/a/out.jar", 200L); // same path, newer creation

        data.addSavedJarInfo(jar1);
        data.addSavedJarInfo(jar2);

        // same path => dedup, only one entry
        assertEquals("same path should not duplicate", 1, data.getSavedJarInfo().length);
        // the surviving entry should have the newer creation time
        assertEquals(200L, data.getSavedJarInfo()[0].getCreation());
    }

    @Test
    public void testAddSavedJarInfoOrderByCreationDescending() {
        HistoryData data = new HistoryData();
        ExportJarInfo jar1 = makeJar("/a/out.jar", 100L);
        ExportJarInfo jar2 = makeJar("/b/out.jar", 300L);
        ExportJarInfo jar3 = makeJar("/c/out.jar", 200L);

        data.addSavedJarInfo(jar1);
        data.addSavedJarInfo(jar2);
        data.addSavedJarInfo(jar3);

        ExportJarInfo[] saved = data.getSavedJarInfo();
        assertEquals(3, saved.length);
        // descending by creation: 300, 200, 100
        assertEquals(300L, saved[0].getCreation());
        assertEquals(200L, saved[1].getCreation());
        assertEquals(100L, saved[2].getCreation());
    }

    @Test
    public void testAddSavedJarInfoNull() {
        HistoryData data = new HistoryData();
        data.addSavedJarInfo(null); // should not throw
        assertNull(data.getSavedJarInfo());
    }
}

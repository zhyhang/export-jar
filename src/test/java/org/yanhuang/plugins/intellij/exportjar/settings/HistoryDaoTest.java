package org.yanhuang.plugins.intellij.exportjar.settings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.yanhuang.plugins.intellij.exportjar.model.ExportJarInfo;
import org.yanhuang.plugins.intellij.exportjar.model.SettingHistory;
import org.yanhuang.plugins.intellij.exportjar.model.SettingTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.Assert.*;

/**
 * Tests for HistoryDao using the injectable cache-root constructor (no real user.home writes).
 */
public class HistoryDaoTest {

    private Path tempDir;
    private HistoryDao dao;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("history-dao-test");
        dao = new HistoryDao(tempDir);
    }

    @After
    public void tearDown() throws IOException {
        // delete temp dir recursively
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> p.toFile().delete());
        }
    }

    @Test
    public void readOrDefault_returnsDefaultWhenNoFile() {
        SettingHistory h = dao.readOrDefault();
        assertNotNull(h);
        assertNotNull(h.getGlobal());
    }

    @Test
    public void initV2023_createsHistoryFile() {
        dao.initV2023();
        assertTrue("history file should exist after initV2023",
                tempDir.resolve("history_v2023.json").toFile().exists());
    }

    @Test
    public void initV2023_idempotent() {
        dao.initV2023();
        long modified1 = tempDir.resolve("history_v2023.json").toFile().lastModified();
        dao.initV2023();
        long modified2 = tempDir.resolve("history_v2023.json").toFile().lastModified();
        assertEquals("second initV2023 should not overwrite existing file", modified1, modified2);
    }

    @Test
    public void saveAndReadProject_roundTrip() {
        dao.initV2023();
        SettingTemplate t = new SettingTemplate();
        t.setName("myTemplate");
        t.setCreateTime(1000L);
        t.setUpdateTime(1000L);
        t.setExportJar(new ExportJarInfo[0]);

        dao.saveProject("myProject", t);

        SettingHistory h = dao.readOrDefault();
        assertNotNull(h.getProjects());
        assertTrue(h.getProjects().containsKey("myProject"));
        assertEquals(1, h.getProjects().get("myProject").size());
        assertEquals("myTemplate", h.getProjects().get("myProject").get(0).getName());
    }

    @Test
    public void saveProject_mergesExistingTemplate() {
        dao.initV2023();
        SettingTemplate t1 = new SettingTemplate();
        t1.setName("t1");
        t1.setCreateTime(1000L);
        t1.setUpdateTime(1000L);
        t1.setExportJar(new ExportJarInfo[0]);

        SettingTemplate t2 = new SettingTemplate();
        t2.setName("t2");
        t2.setCreateTime(2000L);
        t2.setUpdateTime(2000L);
        t2.setExportJar(new ExportJarInfo[0]);

        dao.saveProject("proj", t1);
        dao.saveProject("proj", t2);

        SettingHistory h = dao.readOrDefault();
        assertEquals(2, h.getProjects().get("proj").size());
    }

    @Test
    public void getProjectTemplates_returnsTemplatesForProject() {
        dao.initV2023();
        SettingTemplate t = new SettingTemplate();
        t.setName("tmpl");
        t.setCreateTime(1000L);
        t.setUpdateTime(1000L);
        t.setExportJar(new ExportJarInfo[0]);
        dao.saveProject("proj", t);

        SettingHistory h = dao.readOrDefault();
        var templates = dao.getProjectTemplates(h, "proj");
        assertEquals(1, templates.size());
        assertEquals("tmpl", templates.get(0).getName());
    }

    @Test
    public void getProjectTemplates_returnsEmptyForUnknownProject() {
        dao.initV2023();
        SettingHistory h = dao.readOrDefault();
        var templates = dao.getProjectTemplates(h, "nonexistent");
        assertNotNull(templates);
        assertTrue(templates.isEmpty());
    }
}

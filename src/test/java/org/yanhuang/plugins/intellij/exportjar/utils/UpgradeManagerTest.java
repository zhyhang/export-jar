package org.yanhuang.plugins.intellij.exportjar.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.yanhuang.plugins.intellij.exportjar.settings.HistoryDao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.Assert.*;

/**
 * Tests for UpgradeManager migration logic using injectable cache-root (no real user.home writes).
 * Tests that do NOT trigger the actual migration path (which needs a running platform for notifications)
 * are plain JUnit4. Tests that exercise the full migration path are in UpgradeManagerPlatformTest.
 */
public class UpgradeManagerTest {

    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("upgrade-manager-test");
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> p.toFile().delete());
        }
    }

    @Test
    public void migrateHistoryToV2023_noOldFile_doesNothing() {
        // no old select_history.json → migration is a no-op (returns before needing project)
        UpgradeManager.migrateHistoryToV2023(null, tempDir);
        assertFalse("v2023 file should NOT be created when no old file exists",
                tempDir.resolve("history_v2023.json").toFile().exists());
    }

    @Test
    public void migrateHistoryToV2023_alreadyMigrated_doesNotOverwrite() throws IOException {
        // create both old and v2023 files — migration is a no-op (returns before needing project)
        Path oldFile = tempDir.resolve("select_history.json");
        Files.writeString(oldFile, "{}");
        Path v2023File = tempDir.resolve("history_v2023.json");
        Files.writeString(v2023File, "{\"sentinel\":true}");
        long originalModified = v2023File.toFile().lastModified();

        UpgradeManager.migrateHistoryToV2023(null, tempDir);

        assertEquals("v2023 file should not be overwritten if already exists",
                originalModified, v2023File.toFile().lastModified());
    }

    @Test
    public void historyDaoRoundTrip_independentOfUpgradeManager() throws IOException {
        // HistoryDao can init and round-trip independently of UpgradeManager
        HistoryDao dao = new HistoryDao(tempDir);
        dao.initV2023();
        assertTrue("history file should exist after initV2023",
                tempDir.resolve("history_v2023.json").toFile().exists());
        var history = dao.readOrDefault();
        assertNotNull("HistoryDao should read a valid history", history);
    }
}

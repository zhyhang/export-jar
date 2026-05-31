package org.yanhuang.plugins.intellij.exportjar.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.junit.Assert.*;

public class CommonUtilsCreateJarTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testCreateNewJar() throws Exception {
        // Create source files with known content
        Path srcDir = tmp.newFolder("src").toPath();
        Path fileA = srcDir.resolve("A.class");
        Path fileB = srcDir.resolve("B.class");
        byte[] bytesA = new byte[]{0x01, 0x02, 0x03};
        byte[] bytesB = new byte[]{0x0A, 0x0B};
        Files.write(fileA, bytesA);
        Files.write(fileB, bytesB);

        // Build entry lists: two regular files + one directory entry
        List<Path> filePaths = Arrays.asList(fileA, fileB, null);
        List<String> entryNames = Arrays.asList("com/example/A.class", "com/example/B.class", "com/example/");

        Path jarPath = tmp.newFolder("out").toPath().resolve("test.jar");

        // project=null is safe: infoAndMore only runs when vf != null, and map is empty
        // Use HashMap (not Map.of()) because null filePath (directory entry) would cause NPE in Map.of().get(null)
        CommonUtils.createNewJar(null, jarPath, filePaths, entryNames, new HashMap<>());

        assertTrue("jar file should exist", jarPath.toFile().exists());

        try (JarFile jf = new JarFile(jarPath.toFile())) {
            // Check manifest version
            assertEquals("2.5.0",
                    jf.getManifest().getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION));

            // Check regular entries exist and bytes match
            var entryA = jf.getJarEntry("com/example/A.class");
            assertNotNull("entry A should exist", entryA);
            byte[] readA = jf.getInputStream(entryA).readAllBytes();
            assertArrayEquals("bytes for A should match", bytesA, readA);

            var entryB = jf.getJarEntry("com/example/B.class");
            assertNotNull("entry B should exist", entryB);
            byte[] readB = jf.getInputStream(entryB).readAllBytes();
            assertArrayEquals("bytes for B should match", bytesB, readB);

            // Check directory entry
            var dirEntry = jf.getJarEntry("com/example/");
            assertNotNull("directory entry should exist", dirEntry);
            assertTrue("directory entry should be a directory", dirEntry.isDirectory());

            // Timestamp: just verify it's non-negative (implementation sets it after putNextEntry)
            assertTrue("entry time should be non-negative", entryA.getTime() >= 0);
            assertTrue("entry time should be non-negative", entryB.getTime() >= 0);
        }
    }
}

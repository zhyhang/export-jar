package org.yanhuang.plugins.intellij.exportjar;

import org.junit.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class JarEntryAssemblerTest {

    @Test
    public void testAddEntryWithPackagePath() {
        JarEntryAssembler a = new JarEntryAssembler();
        a.addEntry("com/example", Path.of("/x/Foo.class"));
        assertEquals(1, a.size());
        assertEquals(Path.of("/x/Foo.class"), a.filePathAt(0));
        assertEquals("com/example/Foo.class", a.getEntryNames().get(0));
    }

    @Test
    public void testAddEntryWithEmptyPackagePath() {
        JarEntryAssembler a = new JarEntryAssembler();
        a.addEntry("", Path.of("/x/Foo.txt"));
        assertEquals(1, a.size());
        assertEquals("Foo.txt", a.getEntryNames().get(0));
    }

    @Test
    public void testAddEntryWithTrailingSlashPackagePath() {
        JarEntryAssembler a = new JarEntryAssembler();
        a.addEntry("com/example/", Path.of("/x/Foo.class"));
        assertEquals(1, a.size());
        // trailing slash already present — no double slash
        assertEquals("com/example/Foo.class", a.getEntryNames().get(0));
    }

    @Test
    public void testAddEntryWithExplicitEntryName() {
        JarEntryAssembler a = new JarEntryAssembler();
        a.addEntry("webapp/static", Path.of("/x/app.css"), "webapp/static/app.css");
        assertEquals(1, a.size());
        // explicit entryName wins; packagePath is ignored
        assertEquals("webapp/static/app.css", a.getEntryNames().get(0));
        assertEquals(Path.of("/x/app.css"), a.filePathAt(0));
    }

    @Test
    public void testAddDirectoryEntries() {
        JarEntryAssembler a = new JarEntryAssembler();
        a.addEntry("a/b/c", Path.of("/x/File.txt"));
        // entry name is "a/b/c/File.txt"
        assertEquals("a/b/c/File.txt", a.getEntryNames().get(0));

        a.addDirectoryEntries();

        List<String> names = a.getEntryNames();
        List<Path> paths = a.getFilePaths();

        // original entry still at index 0
        assertEquals("a/b/c/File.txt", names.get(0));
        assertNotNull(paths.get(0));

        // directory entries appended
        assertTrue(names.contains("a/"));
        assertTrue(names.contains("a/b/"));
        assertTrue(names.contains("a/b/c/"));

        // null filePath for each directory entry
        for (int i = 1; i < names.size(); i++) {
            assertNull("dir entry should have null path: " + names.get(i), paths.get(i));
        }

        // lists stay index-aligned
        assertEquals(names.size(), paths.size());
    }

    @Test
    public void testAddDirectoryEntriesNoDuplicates() {
        JarEntryAssembler a = new JarEntryAssembler();
        a.addEntry("a/b", Path.of("/x/F1.txt"));
        a.addEntry("a/b", Path.of("/x/F2.txt"));
        a.addDirectoryEntries();

        long aCount = a.getEntryNames().stream().filter("a/"::equals).count();
        long abCount = a.getEntryNames().stream().filter("a/b/"::equals).count();
        assertEquals("a/ should appear exactly once", 1, aCount);
        assertEquals("a/b/ should appear exactly once", 1, abCount);
    }

    @Test
    public void testAddDirectoryEntriesTopLevelNoSlash() {
        JarEntryAssembler a = new JarEntryAssembler();
        a.addEntry("", Path.of("/x/Root.txt"));
        // entry name is "Root.txt" — no slash, so no dir entries
        int sizeBefore = a.size();
        a.addDirectoryEntries();
        assertEquals("no dir entries for top-level file", sizeBefore, a.size());
    }

    @Test
    public void testSizeAndIndexAlignment() {
        JarEntryAssembler a = new JarEntryAssembler();
        a.addEntry("p", Path.of("/a/A.class"));
        a.addEntry("p/q", Path.of("/b/B.class"));
        assertEquals(2, a.size());
        assertEquals(a.getFilePaths().size(), a.getEntryNames().size());
        assertEquals(Path.of("/a/A.class"), a.filePathAt(0));
        assertEquals(Path.of("/b/B.class"), a.filePathAt(1));
    }
}

package org.yanhuang.plugins.intellij.exportjar.model;

import org.junit.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SettingSelectFileTest {

    private static SettingSelectFile makeFile(String filePath, SettingSelectFile.SelectType type, Path... mappedPaths) {
        SettingSelectFile sf = new SettingSelectFile();
        sf.setFilePath(filePath);
        sf.setSelectType(type);
        Object token = new Object();
        for (Path p : mappedPaths) {
            sf.putMappingVf(p, token);
        }
        return sf;
    }

    private static SettingSelectFile makeFileWithToken(String filePath, SettingSelectFile.SelectType type,
                                                        Path mappedPath, Object token) {
        SettingSelectFile sf = new SettingSelectFile();
        sf.setFilePath(filePath);
        sf.setSelectType(type);
        sf.putMappingVf(mappedPath, token);
        return sf;
    }

    @Test
    public void testNullInputReturnsEmptyMaps() {
        List<Map<Path, Object>> result = SettingSelectFile.combineFinalVirtualFiles(null);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).isEmpty());
        assertTrue(result.get(1).isEmpty());
    }

    @Test
    public void testEmptyArrayReturnsEmptyMaps() {
        List<Map<Path, Object>> result = SettingSelectFile.combineFinalVirtualFiles(new SettingSelectFile[0]);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).isEmpty());
        assertTrue(result.get(1).isEmpty());
    }

    @Test
    public void testIncludeOnlyFile() {
        Path p = Path.of("/a/b/file.java");
        Object token = new Object();
        SettingSelectFile sf = makeFileWithToken("/a/b", SettingSelectFile.SelectType.include, p, token);

        List<Map<Path, Object>> result = SettingSelectFile.combineFinalVirtualFiles(new SettingSelectFile[]{sf});

        assertTrue("file should be in include map", result.get(0).containsKey(p));
        assertFalse("file should not be in exclude map", result.get(1).containsKey(p));
        assertEquals(token, result.get(0).get(p));
    }

    @Test
    public void testExcludeOnlyFile() {
        Path p = Path.of("/a/b/file.java");
        Object token = new Object();
        SettingSelectFile sf = makeFileWithToken("/a/b", SettingSelectFile.SelectType.exclude, p, token);

        List<Map<Path, Object>> result = SettingSelectFile.combineFinalVirtualFiles(new SettingSelectFile[]{sf});

        assertFalse("file should not be in include map", result.get(0).containsKey(p));
        assertTrue("file should be in exclude map", result.get(1).containsKey(p));
    }

    @Test
    public void testExcludeMoreSpecificThanInclude() {
        // include /a, exclude /a/b — same token (same virtual file)
        // exclude path starts-with include path => excluded
        Path p = Path.of("/a/b/file.java");
        Object token = new Object();

        SettingSelectFile include = makeFileWithToken("/a", SettingSelectFile.SelectType.include, p, token);
        SettingSelectFile exclude = makeFileWithToken("/a/b", SettingSelectFile.SelectType.exclude, p, token);

        List<Map<Path, Object>> result = SettingSelectFile.combineFinalVirtualFiles(
                new SettingSelectFile[]{include, exclude});

        assertFalse("file should not be in include map when exclude is more specific",
                result.get(0).containsKey(p));
        assertTrue("file should be in exclude map when exclude is more specific",
                result.get(1).containsKey(p));
    }

    @Test
    public void testIncludeMoreSpecificThanNonMatchingExclude() {
        // include /a/b, exclude /a/c — different paths, same token
        // exclude path /a/c does NOT start-with include path /a/b => included
        Path p = Path.of("/a/b/file.java");
        Object token = new Object();

        SettingSelectFile include = makeFileWithToken("/a/b", SettingSelectFile.SelectType.include, p, token);
        // exclude maps a different path, not the same token
        SettingSelectFile exclude = new SettingSelectFile();
        exclude.setFilePath("/a/c");
        exclude.setSelectType(SettingSelectFile.SelectType.exclude);
        exclude.putMappingVf(Path.of("/a/c/other.java"), new Object());

        List<Map<Path, Object>> result = SettingSelectFile.combineFinalVirtualFiles(
                new SettingSelectFile[]{include, exclude});

        assertTrue("file should be in include map", result.get(0).containsKey(p));
        assertFalse("file should not be in exclude map", result.get(1).containsKey(p));
    }
}

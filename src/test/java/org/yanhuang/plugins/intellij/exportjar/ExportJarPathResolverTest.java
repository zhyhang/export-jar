package org.yanhuang.plugins.intellij.exportjar;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link ExportJarPathResolver} (pure logic, no IntelliJ fixtures needed).
 */
public class ExportJarPathResolverTest {

    @Test
    public void nullOrBlankPathReturnsEmptyStatus() {
        assertEquals(ExportJarPathResolver.Status.EMPTY_PATH,
                ExportJarPathResolver.resolve(null, null).getStatus());
        assertEquals(ExportJarPathResolver.Status.EMPTY_PATH,
                ExportJarPathResolver.resolve("   ", null).getStatus());
    }

    @Test
    public void existingDirectoryReturnsIsDirectory() throws IOException {
        final Path dir = Files.createTempDirectory("export-resolver-dir");
        final ExportJarPathResolver.Result result = ExportJarPathResolver.resolve(dir.toString(), null);
        assertEquals(ExportJarPathResolver.Status.IS_DIRECTORY, result.getStatus());
        assertNull(result.getResolvedPath());
    }

    @Test
    public void nonExistingParentReturnsParentNotExists() {
        final String bogus = "/this/path/surely/does/not/exist/out.jar";
        final ExportJarPathResolver.Result result = ExportJarPathResolver.resolve(bogus, null);
        assertEquals(ExportJarPathResolver.Status.PARENT_NOT_EXISTS, result.getStatus());
    }

    @Test
    public void appendsJarSuffixWhenMissing() throws IOException {
        final Path dir = Files.createTempDirectory("export-resolver-suffix");
        final Path noSuffix = dir.resolve("artifact");
        final ExportJarPathResolver.Result result = ExportJarPathResolver.resolve(noSuffix.toString(), null);
        assertEquals(ExportJarPathResolver.Status.RESOLVED, result.getStatus());
        assertNotNull(result.getResolvedPath());
        assertEquals("artifact.jar", result.getResolvedPath().getFileName().toString());
    }

    @Test
    public void keepsJarSuffixWhenPresent() throws IOException {
        final Path dir = Files.createTempDirectory("export-resolver-keep");
        final Path withSuffix = dir.resolve("artifact.jar");
        final ExportJarPathResolver.Result result = ExportJarPathResolver.resolve(withSuffix.toString(), null);
        assertEquals(ExportJarPathResolver.Status.RESOLVED, result.getStatus());
        assertEquals("artifact.jar", result.getResolvedPath().getFileName().toString());
    }

    @Test
    public void fileNameWithoutParentResolvesAgainstProjectBase() throws IOException {
        final Path base = Files.createTempDirectory("export-resolver-base");
        final ExportJarPathResolver.Result result = ExportJarPathResolver.resolve("out.jar", base.toString());
        assertEquals(ExportJarPathResolver.Status.RESOLVED, result.getStatus());
        assertEquals(base.resolve("out.jar"), result.getResolvedPath());
    }
}

package org.yanhuang.plugins.intellij.exportjar.utils;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class CommonUtilsOffspringClassTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testFindOffspringClassName() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assume.assumeNotNull("Skipping: no Java compiler available (JRE-only runtime)", compiler);

        // Write a source file with inner and nested classes
        File srcDir = tmp.newFolder("src");
        File pkgDir = new File(srcDir, "p");
        assertTrue(pkgDir.mkdirs());
        File srcFile = new File(pkgDir, "Foo.java");
        java.nio.file.Files.writeString(srcFile.toPath(),
                "package p;\n" +
                "public class Foo {\n" +
                "    class Inner {}\n" +
                "    static class Nested {}\n" +
                "}\n");

        File outDir = tmp.newFolder("out");
        int result = compiler.run(null, null, null,
                "-d", outDir.getAbsolutePath(),
                srcFile.getAbsolutePath());
        assertTrue("Compilation should succeed", result == 0);

        Path fooClassFile = outDir.toPath().resolve("p/Foo.class");
        assertTrue("Foo.class should exist", fooClassFile.toFile().exists());

        Set<String> offspringNames = new HashSet<>();
        CommonUtils.findOffspringClassName(offspringNames, fooClassFile);

        assertTrue("Should contain Foo$Inner", offspringNames.contains("Foo$Inner"));
        assertTrue("Should contain Foo$Nested", offspringNames.contains("Foo$Nested"));
    }
}

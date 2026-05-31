package org.yanhuang.plugins.intellij.exportjar.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class SimpleFileLockTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testLockLifecycle() throws Exception {
        Path lockFile = tmp.newFolder("locks").toPath().resolve("test.lock");

        SimpleFileLock lock = new SimpleFileLock(lockFile);

        assertFalse("fresh lock should not be locked", lock.isLocked());

        assertTrue("first tryLock should succeed", lock.tryLock());
        assertTrue("after tryLock, isLocked should be true", lock.isLocked());

        assertFalse("second tryLock on same path should fail (file exists)", lock.tryLock());

        assertTrue("unlock should succeed", lock.unlock());
        assertFalse("after unlock, isLocked should be false", lock.isLocked());
    }

    @Test
    public void testConstructorCreatesMissingParentDirs() {
        Path nestedLock = tmp.getRoot().toPath()
                .resolve("deep").resolve("nested").resolve("dir").resolve("my.lock");

        // Parent dirs do not exist yet — constructor should create them
        SimpleFileLock lock = new SimpleFileLock(nestedLock);

        assertTrue("parent dirs should be created by constructor",
                nestedLock.getParent().toFile().exists());

        // Should be able to lock after parent dirs are created
        assertTrue("tryLock should succeed after parent dirs created", lock.tryLock());
        assertTrue(lock.isLocked());
        lock.unlock();
    }
}

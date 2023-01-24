package org.yanhuang.plugins.intellij.exportjar.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * simple file lock learnt from Intellij Coverage IOUtils
 */
public final class SimpleFileLock {
        final File lock;

        public SimpleFileLock(Path lockFile) {
            this.lock =lockFile.toFile();
            if (this.lock.getParentFile() != null) {
                final boolean ignored = this.lock.getParentFile().mkdirs();
            }
        }

        public boolean isLocked() {
            return this.lock.exists();
        }

        public boolean tryLock() {
            try {
                return this.lock.createNewFile();
            } catch (IOException e) {
                return false;
            }
        }

        public boolean unlock() {
            try {
                return this.lock.delete();
            } catch (Exception e) {
                return false;
            }
        }

    }
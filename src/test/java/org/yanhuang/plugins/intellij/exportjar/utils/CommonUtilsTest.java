package org.yanhuang.plugins.intellij.exportjar.utils;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import static org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils.collectFilesNest;
import static org.yanhuang.plugins.intellij.exportjar.utils.CommonUtils.fromOsFile;

public class CommonUtilsTest extends BasePlatformTestCase {

	public void testCollectFilesNest() throws IOException {
		// null check
		assertEmpty(collectFilesNest(null));
		// make directories and files
		final Path dirRoot = Files.createTempDirectory("dir-root");
		final Path dirS1 = dirRoot.resolve("dir-s1");
		final Path dirS1S1 = dirS1.resolve("dir-s1-s1");
		boolean ignored = dirS1S1.toFile().mkdirs();
		final Path dirS1S2 = dirS1.resolve("dir-s1-s2");
		ignored = dirS1S2.toFile().mkdirs();
		final Path dirS1Empty = dirS1.resolve("dir-s1-empty");
		ignored = dirS1Empty.toFile().mkdirs();
		final Path dirS2 = dirRoot.resolve("dir-s2");
		final Path dirS2S1 = dirS2.resolve("dir-s2-s1");
		ignored = dirS2S1.toFile().mkdirs();
		final Path dirS2S2 = dirS2.resolve("dir-s2-s2");
		ignored = dirS2S2.toFile().mkdirs();
		final Path dirS2Empty = dirS2.resolve("dir-s2-empty");
		ignored = dirS2Empty.toFile().mkdirs();
		final Path dirEmpty = dirRoot.resolve("dir-empty");
		ignored = dirEmpty.toFile().mkdirs();
		final Path fileRoot1 = dirRoot.resolve("tf1");
		final Path fileRoot2 = dirRoot.resolve("tf2");
		Files.createFile(fileRoot1);
		Files.createFile(fileRoot2);
		final Path fileS1 = dirS1.resolve("tf");
		Files.createFile(fileS1);
		final Path fileS1S1 = dirS1S1.resolve("tf");
		Files.createFile(fileS1S1);
		final Path fileS1S2 = dirS1S2.resolve("tf");
		Files.createFile(fileS1S2);
		final Path fileS2 = dirS2.resolve("tf");
		Files.createFile(fileS2);
		final Path fileS2S1 = dirS2S1.resolve("tf");
		Files.createFile(fileS2S1);
		final Path fileS2S2 = dirS2S2.resolve("tf");
		Files.createFile(fileS2S2);
		// recursively collect
		final VirtualFile vfDirRoot = fromOsFile(dirRoot.toString());
		final Collection<VirtualFile> virtualFiles1 = collectFilesNest(vfDirRoot);
		assertEquals(8, virtualFiles1.size());
		assertTrue(virtualFiles1.contains(fromOsFile(fileRoot1.toString())));
		assertTrue(virtualFiles1.contains(fromOsFile(fileRoot2.toString())));
		assertTrue(virtualFiles1.contains(fromOsFile(fileS1.toString())));
		assertTrue(virtualFiles1.contains(fromOsFile(fileS1S1.toString())));
		assertTrue(virtualFiles1.contains(fromOsFile(fileS1S2.toString())));
		assertTrue(virtualFiles1.contains(fromOsFile(fileS2.toString())));
		assertTrue(virtualFiles1.contains(fromOsFile(fileS2S1.toString())));
		assertTrue(virtualFiles1.contains(fromOsFile(fileS2S2.toString())));
		// empty dir
		final VirtualFile vfDirEmpty = fromOsFile(dirEmpty.toString());
		final Collection<VirtualFile> virtualFiles2 = collectFilesNest(vfDirEmpty);
		assertEmpty(virtualFiles2);
		// dir s1
		final VirtualFile vfDirS1 = fromOsFile(dirS1.toString());
		final Collection<VirtualFile> virtualFiles3 = collectFilesNest(vfDirS1);
		assertEquals(3, virtualFiles3.size());
		assertTrue(virtualFiles3.contains(fromOsFile(fileS1.toString())));
		assertTrue(virtualFiles3.contains(fromOsFile(fileS1S1.toString())));
		assertTrue(virtualFiles3.contains(fromOsFile(fileS1S2.toString())));
		// root is file
		final VirtualFile vfFileS2 = fromOsFile(fileS2.toString());
		final Collection<VirtualFile> virtualFiles4 = collectFilesNest(vfFileS2);
		assertEquals(1, virtualFiles4.size());
		assertTrue(virtualFiles4.contains(fromOsFile(fileS2.toString())));
		// root not exists
		final VirtualFile vfDirNoExists = fromOsFile("/not_exists_for_test_" + ThreadLocalRandom.current().nextLong());
		assertEmpty(collectFilesNest(vfDirNoExists));
	}


}
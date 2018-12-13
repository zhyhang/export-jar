package org.yanhuang.plugins.intellij.exportjar;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.TreeMap;

/**
 * operation history data
 */
public class HistoryData {

	private static final int maxHistory = 100;

	private SavedJarInfo[] savedJarInfos;

	public SavedJarInfo[] getSavedJarInfos() {
		return savedJarInfos;
	}

	public void setSavedJarInfos(SavedJarInfo[] savedJarInfos) {
		this.savedJarInfos = savedJarInfos;
	}

	public void addSavedJarInfo(SavedJarInfo savedJarInfo) {
		if (savedJarInfo == null) {
			return;
		}
		Comparator<Long> comparator = Long::compareTo;
		TreeMap<Long, SavedJarInfo> savedTree = new TreeMap<>(comparator.reversed());
		if (savedJarInfos != null) {
			Arrays.stream(savedJarInfos).forEach(s -> {
				if (savedJarInfo.equals(s)) {
					savedTree.put(savedJarInfo.getCreation(), savedJarInfo);
				} else {
					savedTree.put(s.getCreation(), s);
				}
			});
		}
		savedTree.put(savedJarInfo.getCreation(), savedJarInfo);
		this.savedJarInfos = savedTree.values().toArray(new SavedJarInfo[0]);
		if (savedJarInfos.length > maxHistory) {
			savedJarInfos = Arrays.copyOf(savedJarInfos, maxHistory);
		}
	}

	public static class SavedJarInfo implements Comparable {
		private String path;
		private long creation;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public long getCreation() {
			return creation;
		}

		public void setCreation(long creation) {
			this.creation = creation;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SavedJarInfo that = (SavedJarInfo) o;
			return path.equals(that.path);
		}

		@Override
		public int hashCode() {
			return Objects.hash(path);
		}

		@Override
		public int compareTo(@NotNull Object o) {
			if (this == o) {
				return 0;
			}
			if (getClass() != o.getClass()) return 1;
			SavedJarInfo that = (SavedJarInfo) o;
			return Long.compare(this.getCreation(), that.getCreation());
		}
	}
}

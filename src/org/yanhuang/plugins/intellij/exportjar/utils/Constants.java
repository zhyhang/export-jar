package org.yanhuang.plugins.intellij.exportjar.utils;

import org.apache.commons.lang3.SystemUtils;

import java.nio.file.Path;

public interface Constants {
	String actionName = "Export Jar";
	String infoTabName = "Packing Export Jar";
	String creator = "yanhuang.org";
	Path cachePath = SystemUtils.getUserHome().toPath().resolve(".IntelliJIdea_export_jar");
	Path historyFilePath = cachePath.resolve("select_history.json");
}

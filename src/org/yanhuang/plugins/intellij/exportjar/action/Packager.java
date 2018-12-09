package org.yanhuang.plugins.intellij.exportjar.action;

import com.intellij.openapi.compiler.CompileStatusNotification;

public abstract class Packager implements CompileStatusNotification {
	public abstract void pack();
}

package org.yanhuang.plugins.intellij.exportjar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Pure resolution/validation of the user-entered output JAR path, extracted from
 * {@code SettingDialog.doExport} so the logic is unit-testable and free of UI concerns.
 * <p>
 * The caller maps the {@link Status} to user-facing dialogs and performs the
 * overwrite confirmation (the resolver only reports the resolved path).
 */
public final class ExportJarPathResolver {

    public enum Status {
        /** input path is null/blank */
        EMPTY_PATH,
        /** input points to an existing directory, not a jar file name */
        IS_DIRECTORY,
        /** the (possibly project-relative) parent directory does not exist */
        PARENT_NOT_EXISTS,
        /** resolved successfully; {@link Result#getResolvedPath()} is the final jar path */
        RESOLVED
    }

    public static final class Result {
        private final Status status;
        private final Path resolvedPath;

        private Result(Status status, Path resolvedPath) {
            this.status = status;
            this.resolvedPath = resolvedPath;
        }

        public Status getStatus() {
            return status;
        }

        /** @return final jar path, non-null only when status == RESOLVED */
        @Nullable
        public Path getResolvedPath() {
            return resolvedPath;
        }
    }

    private ExportJarPathResolver() {
    }

    /**
     * Resolve and validate the selected output jar path.
     *
     * @param selectedOutputJarPath raw value selected in the output combo box (may be null/blank)
     * @param projectBasePath       project base path, used as parent when input has no parent dir (may be null)
     * @return resolution result
     */
    @NotNull
    public static Result resolve(@Nullable String selectedOutputJarPath, @Nullable String projectBasePath) {
        if (selectedOutputJarPath == null || selectedOutputJarPath.trim().isEmpty()) {
            return new Result(Status.EMPTY_PATH, null);
        }
        Path exportJarFullPath = Paths.get(selectedOutputJarPath.trim());
        if (Files.isDirectory(exportJarFullPath)) {
            return new Result(Status.IS_DIRECTORY, null);
        }
        Path exportJarParentPath = exportJarFullPath.getParent();
        if (exportJarParentPath == null) {
            // input file without parent dir: use project base dir (or current dir) as parent
            exportJarParentPath = Paths.get(projectBasePath != null ? projectBasePath : "./");
            exportJarFullPath = exportJarParentPath.resolve(exportJarFullPath);
        }
        if (!Files.exists(exportJarParentPath)) {
            return new Result(Status.PARENT_NOT_EXISTS, null);
        }
        final String exportJarName = exportJarFullPath.getFileName().toString();
        if (!exportJarName.endsWith(".jar")) {
            exportJarFullPath = Paths.get(exportJarFullPath + ".jar");
        }
        return new Result(Status.RESOLVED, exportJarFullPath);
    }
}

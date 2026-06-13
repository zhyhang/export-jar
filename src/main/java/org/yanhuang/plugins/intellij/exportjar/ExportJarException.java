package org.yanhuang.plugins.intellij.exportjar;

/**
 * Unchecked exception for export-jar domain errors.
 * <p>
 * Use this instead of raw {@link RuntimeException} so export failures carry a
 * clear domain meaning and can be distinguished from unexpected platform errors
 * when caught (e.g. in {@code ExportPacker.whenFinishSuccess}).
 */
public class ExportJarException extends RuntimeException {

    public ExportJarException(String message) {
        super(message);
    }

    public ExportJarException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExportJarException(Throwable cause) {
        super(cause);
    }
}

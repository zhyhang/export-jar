package org.yanhuang.plugins.intellij.exportjar.utils;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.EDT;
import org.jetbrains.annotations.NotNull;
import org.yanhuang.plugins.intellij.exportjar.ExportJarException;

import java.util.concurrent.Callable;

/**
 * Threading helpers that comply with the IntelliJ Platform threading model.
 * <p>
 * Centralizes the EDT-vs-background-thread handling so callers don't repeat the
 * branching logic. See <a href="https://plugins.jetbrains.com/docs/intellij/threading-model.html">threading model</a>.
 */
public final class TaskExecutors {

    private TaskExecutors() {
    }

    /**
     * Executes a task under a read lock on a background thread and waits for its completion.
     * When invoked on the EDT it submits a non-blocking read action and waits for the promise;
     * otherwise it runs the read action synchronously on the current background thread.
     *
     * @param task    callable task to execute
     * @param project current project
     * @param <T>     return type of the task
     * @return result of the task execution
     * @throws ExportJarException if any exception occurs during execution
     */
    public static <T> T runInBgtWithReadLockAndWait(Callable<? extends T> task, Project project) {
        try {
            if (EDT.isCurrentThreadEdt()) {
                final var promise = ReadAction.nonBlocking(task)
                        .inSmartMode(project)
                        .withDocumentsCommitted(project)
                        .submit(AppExecutorUtil.getAppExecutorService());
                return promise.get();
            } else {
                return ReadAction.nonBlocking(task)
                        .inSmartMode(project)
                        .withDocumentsCommitted(project)
                        .executeSynchronously();
            }
        } catch (Exception e) {
            throw new ExportJarException(e);
        }
    }

    /**
     * Runs a task in the background without holding a read lock.
     * Used to avoid throwing SLOW warning exceptions when performing potentially lengthy operations.
     *
     * @param runnable  the task to run
     * @param project   current project
     * @param taskTitle title of the background task to be displayed in the progress indicator
     */
    public static void backgroundRunWithoutLock(final Runnable runnable, final Project project, final String taskTitle) {
        // move export action to BGT, avoid throwing SLOW warning exception
        if (EDT.isCurrentThreadEdt()) {
            Task.Backgroundable task = new Task.Backgroundable(project, taskTitle) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    runnable.run();
                }
            };
            ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, new BackgroundableProcessIndicator(task));
        } else {
            runnable.run();
        }
    }
}

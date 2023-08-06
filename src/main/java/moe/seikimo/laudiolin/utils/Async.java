package moe.seikimo.laudiolin.utils;

import moe.seikimo.laudiolin.Laudiolin;

public interface Async {
    /**
     * Submits a task to the thread pool.
     *
     * @param runnable The runnable to submit.
     */
    static void run(Runnable runnable) {
        Laudiolin.getThreadPool().submit(runnable);
    }

    /**
     * Submits a task to the thread pool.
     * Runs the task after the specified duration.
     *
     * @param runnable The runnable to submit.
     * @param duration The duration to wait before running the task.
     */
    static void runAfter(Runnable runnable, float duration) {
        var sleep = (long) Math.floor(duration * 1000);
        Async.run(() -> {
            try { Thread.sleep(sleep); }
            catch (InterruptedException ignored) { }

            runnable.run(); // Run the task.
        });
    }
}

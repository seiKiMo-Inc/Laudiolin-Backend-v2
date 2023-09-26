package moe.seikimo.laudiolin.utils;

public interface Assertions {
    /**
     * Checks if a condition is true.
     *
     * @param condition The condition to check.
     * @param message The message to throw if the condition is false.
     */
    static void check(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}

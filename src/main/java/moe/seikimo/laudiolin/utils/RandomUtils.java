package moe.seikimo.laudiolin.utils;

import java.util.Random;

public interface RandomUtils {
    Random RANDOM = new Random();
    String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * Generates a random integer between the given minimum and maximum values.
     *
     * @param min The minimum value.
     * @param max The maximum value.
     * @return The random integer.
     */
    static int random(int min, int max) {
        return RANDOM.nextInt(max - min) + min;
    }

    /**
     * Generates a random number.
     *
     * @param length The length of the number.
     * @return The random number.
     */
    static long random(long length) {
        var builder = new StringBuilder();
        for (var i = 0; i < length; i++) {
            builder.append(RandomUtils.random(0, 9));
        }

        return Long.parseLong(builder.toString());
    }

    /**
     * Generates a random string.
     *
     * @param length The length of the string.
     * @return The random string.
     */
    static String randomString(int length) {
        var builder = new StringBuilder();
        for (var i = 0; i < length; i++) {
            builder.append(CHARSET.charAt(
                    (int) (Math.random() * CHARSET.length())));
        }
        return builder.toString();
    }
}

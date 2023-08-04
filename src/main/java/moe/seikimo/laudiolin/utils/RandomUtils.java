package moe.seikimo.laudiolin.utils;

import java.util.Random;

public interface RandomUtils {
    Random RANDOM = new Random();

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
}

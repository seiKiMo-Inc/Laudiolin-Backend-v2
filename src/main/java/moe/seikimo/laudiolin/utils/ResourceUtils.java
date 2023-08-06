package moe.seikimo.laudiolin.utils;

import moe.seikimo.laudiolin.Laudiolin;

import java.io.IOException;

public interface ResourceUtils {
    /**
     * Reads a resource from the specified path.
     *
     * @param path The path.
     * @return The resource.
     */
    static byte[] getResource(String path) {
        try (var resource = Laudiolin.class.getClassLoader()
                .getResourceAsStream(path)) {
            return resource == null ? new byte[0] : resource.readAllBytes();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }
}

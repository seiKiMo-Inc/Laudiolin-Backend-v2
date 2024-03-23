package moe.seikimo.laudiolin.utils;

import moe.seikimo.laudiolin.Laudiolin;

import java.io.File;
import java.io.IOException;

public interface FileUtils {
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

    /**
     * Retrieves the name of a file.
     *
     * @param file The file.
     * @return The name of the file.
     */
    static String fileName(File file) {
        var name = file.getName();
        return !name.contains(".") ? name :
                name.substring(0, name.lastIndexOf('.'));
    }
}

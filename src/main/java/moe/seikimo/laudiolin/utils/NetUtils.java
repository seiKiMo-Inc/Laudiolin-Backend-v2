package moe.seikimo.laudiolin.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;

public interface NetUtils {
    /**
     * @return A free port.
     */
    static int findFreePort() {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates a given URL.
     *
     * @param url The URL to validate.
     * @return Whether the URL is valid.
     */
    static boolean validUrl(String url) {
        if (!url.startsWith("http")) return false;

        try {
            new URL(url).toURI();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}

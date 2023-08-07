package moe.seikimo.laudiolin.utils;

import java.io.IOException;
import java.net.ServerSocket;

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
}

package moe.seikimo.laudiolin.routers;

import io.javalin.http.Context;
import moe.seikimo.laudiolin.Laudiolin;

public interface SiteRouter {
    /**
     * Handles Javalin/HTTP exceptions.
     *
     * @param exception The exception.
     * @param ctx The Javalin instance.
     */
    static void handleException(Exception exception, Context ctx) {
        ctx.status(500).result("Internal Server Error");
        Laudiolin.getLogger().warn("An exception occurred while handling a request.", exception);
    }
}

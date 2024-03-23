package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.files.LocalFileManager;

import java.io.FileInputStream;

import static moe.seikimo.laudiolin.utils.HttpUtils.INTERNAL_ERROR;
import static moe.seikimo.laudiolin.utils.HttpUtils.INVALID_ARGUMENTS;

public interface FileRouter {
    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        javalin.get("/track/{id}", FileRouter::fetchTrack);
        javalin.get("/icon/{id}", FileRouter::fetchCover);
    }

    /**
     * Fetches a local track's data.
     *
     * @param ctx The context.
     */
    static void fetchTrack(Context ctx) {
        try {
            // Pull arguments.
            var id = ctx.pathParam("id");

            // Check if the arguments are valid.
            if (id.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }

            // Check if the track exists in the database.
            var track = LocalFileManager.getLocalTracks().get(id);
            if (track == null) {
                ctx.status(404).json(INVALID_ARGUMENTS());
            } else {
                ctx.json(track.data());
            }
         } catch (Exception exception) {
            ctx.status(500).json(INTERNAL_ERROR(exception.getMessage()));
        }
    }

    /**
     * Fetches a local track's cover.
     *
     * @param ctx The context.
     */
    static void fetchCover(Context ctx) {
        try {
            // Pull arguments.
            var id = ctx.pathParam("id");

            // Check if the arguments are valid.
            if (id.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }

            // Check if the track exists in the database.
            var track = LocalFileManager.getLocalTracks().get(id);
            if (track == null) {
                ctx.status(404).json(INVALID_ARGUMENTS());
            } else {
                ctx
                        .contentType(ContentType.IMAGE_PNG)
                        .header("Cache-Control", "public, max-age=604800, immutable")
                        .result(new FileInputStream(track.coverFile()));
            }
        } catch (Exception exception) {
            ctx.status(500).json(INTERNAL_ERROR(exception.getMessage()));
        }
    }
}

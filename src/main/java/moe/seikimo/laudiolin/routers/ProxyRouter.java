package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.utils.HttpUtils;
import moe.seikimo.laudiolin.utils.ImageUtils;
import moe.seikimo.laudiolin.utils.NetUtils;

import java.util.Base64;

import static moe.seikimo.laudiolin.utils.HttpUtils.INTERNAL_ERROR;
import static moe.seikimo.laudiolin.utils.HttpUtils.INVALID_ARGUMENTS;

public interface ProxyRouter {
    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        javalin.get("/proxy/icon", ProxyRouter::iconProxy);
        javalin.get("/proxy/{arg}", ProxyRouter::proxy);
    }

    /**
     * Handles requests for external image proxies.
     * This handles requests for playlist and user icons.
     *
     * @param ctx The context.
     */
    static void iconProxy(Context ctx) {
        try {
            // Pull arguments.
            var url = ctx.queryParam("url");

            // Check if the arguments are valid.
            if (url == null || url.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }

            // Attempt to Base64 decode the URL.
            var decodedUrl = Base64.getUrlDecoder().decode(url);
            url = new String(decodedUrl);

            // Validate the URL.
            if (!NetUtils.validUrl(url)) {
                ctx.status(400).json(INVALID_ARGUMENTS("Invalid URL provided."));
                return;
            }

            // Make the request.
            ctx.status(200).result(HttpUtils.makeRequest(url))
                    .contentType(ContentType.IMAGE_JPEG)
                    .header("Cache-Control", "public, max-age=604800, immutable");
        } catch (Exception exception) {
            ctx.status(500).json(INTERNAL_ERROR(exception.getMessage()));
        }
    }

    /**
     * Handles requests for image proxies.
     *
     * @param ctx The context.
     */
    static void proxy(Context ctx) {
        try {
            // Pull arguments.
            var url = ctx.pathParam("arg");
            var from = ctx.queryParam("from");

            // Check if the arguments are valid.
            if (url.isEmpty() || from == null || from.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }

            ctx
                    .status(200)
                    .contentType(ContentType.IMAGE_JPEG)
                    .header("Cache-Control", "public, max-age=604800, immutable")
                    .result(switch (from) {
                        default -> throw new IllegalArgumentException();
                        case "cart" -> {
                            // Adjust the URL.
                            url = url.substring(0, url.indexOf("=w"));
                            url += "=w512-h512-l90-rj?from=cart";
                            yield HttpUtils.makeRequest("https://lh3.googleusercontent.com/" + url);
                        }
                        case "spot" -> HttpUtils.makeRequest("https://i.scdn.co/image/" + url);
                        case "yt" -> ImageUtils.getYouTubeThumbnail(url);
                    });
        } catch (Exception ignored) {
            ctx.status(400).json(INVALID_ARGUMENTS());
        }
    }
}

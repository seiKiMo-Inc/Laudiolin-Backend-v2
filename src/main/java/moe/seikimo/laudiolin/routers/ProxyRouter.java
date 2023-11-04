package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.utils.HttpUtils;

import static moe.seikimo.laudiolin.utils.HttpUtils.INVALID_ARGUMENTS;

public interface ProxyRouter {
    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        javalin.get("/proxy/{arg}", ProxyRouter::proxy);
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

            ctx.status(200).result(switch (from) {
                default -> throw new IllegalArgumentException();
                case "cart" -> {
                    // Adjust the URL.
                    url = url.substring(0, url.indexOf("=w"));
                    url += "=w512-h512-l90-rj?from=cart";
                    yield HttpUtils.makeRequest("https://lh3.googleusercontent.com/" + url);
                }
                case "spot" -> HttpUtils.makeRequest("https://i.scdn.co/image/" + url);
                case "yt" -> HttpUtils.makeRequest("https://i.ytimg.com/vi/" + url + "/hq720.jpg");
            }).contentType(ContentType.IMAGE_JPEG)
                    .header("Cache-Control", "public, max-age=604800, immutable");
        } catch (Exception ignored) {
            ctx.status(400).json(INVALID_ARGUMENTS());
        }
    }
}

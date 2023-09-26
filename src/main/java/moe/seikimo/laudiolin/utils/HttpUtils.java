package moe.seikimo.laudiolin.utils;

import com.google.gson.JsonObject;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.objects.Pair;
import okhttp3.*;

import java.io.IOException;

@SuppressWarnings("KotlinInternalInJava")
public interface HttpUtils {
    byte[] DIVIDER = "\r\n".getBytes();

    OkHttpClient CLIENT
            = new OkHttpClient.Builder()
            .addInterceptor(HttpUtils::interceptRequest)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();
    MediaType JSON_MEDIA_TYPE = MediaType.parse(
            "application/json; charset=utf-8");

    /**
     * Intercepts an HTTP request made by the HTTP client.
     * This is used for adding headers to the request.
     *
     * @param chain The interceptor chain.
     * @return The response.
     */
    static Response interceptRequest(Interceptor.Chain chain) throws IOException {
        var request = chain.request();
        var builder = request.newBuilder();
        builder.addHeader("User-Agent", "https://github.com/seiKiMo-Inc");

        return chain.proceed(builder.build());
    }

    /**
     * Makes an HTTP request.
     *
     * @param request The request to make.
     * @return The response.
     */
    static Response makeRequest(Request request) {
        try {
            return CLIENT.newCall(request).execute();
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Performs an HTTP request.
     *
     * @param url The URL to request.
     * @return The response body as bytes.
     */
    static byte[] makeRequest(String url) {
        var request = new Request.Builder()
                .url(url).build();
        try (var response = CLIENT.newCall(request).execute()) {
            // Validate the response body.
            var body = response.body();
            if (body == null) return new byte[0];

            return body.bytes();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    /**
     * Creates an HTTP chunk based on the length of the array.
     *
     * @param bytes The bytes to chunk.
     * @return The chunked bytes.
     */
    static byte[] createChunk(byte[] bytes) {
        var length = String.valueOf(
                bytes.length).getBytes();
        var chunk = new byte[length.length +
                bytes.length +
                (DIVIDER.length * 2)];

        // Write the data to the chunk.
        System.arraycopy(length, 0, chunk, 0, length.length);
        System.arraycopy(DIVIDER, 0, chunk, length.length, DIVIDER.length);
        System.arraycopy(bytes, 0, chunk, length.length + DIVIDER.length, bytes.length);
        System.arraycopy(DIVIDER, 0, chunk, length.length + DIVIDER.length + bytes.length, DIVIDER.length);

        return chunk;
    }

    /**
     * Attempts to pull authorization.
     *
     * @param ctx The context to pull from.
     * @return The authorization token.
     */
    static String getToken(Context ctx) {
        var token = ctx.header("authorization");
        if (token == null) return null;

        // TODO: Check token length here.
        return token.isEmpty() ? null : token;
    }

    /**
     * Fetches the IP of a request.
     *
     * @param ctx The context.
     * @return The IP.
     */
    static String ip(Context ctx) {
        // Check headers.
        var address = ctx.header("CF-Connecting-IP");
        if (address != null) return address;

        address = ctx.header("X-Forwarded-For");
        if (address != null) return address;

        address = ctx.header("X-Real-IP");
        if (address != null) return address;

        // Return the request IP.
        return ctx.ip();
    }

    /**
     * Parses the range header.
     *
     * @param ctx The context.
     * @return The range.
     */
    static Pair<Integer, Integer> range(Context ctx) {
        var range = ctx.header("Range");
        if (range == null) return null;

        // Check if the range is valid.
        if (!range.startsWith("bytes=")) return null;

        // Remove the bytes= part.
        range = range.substring(6);

        // Check if the range is valid.
        if (!range.contains("-")) return null;

        // Split the range.
        var split = range.split("-");

        // Parse the range.
        var start = Integer.parseInt(split[0]);
        var end = split.length == 1 ? -1 :
                Integer.parseInt(split[1]);

        return new Pair<>(start, end);
    }

    /**
     * @return A 200 success.
     */
    static JsonObject SUCCESS() {
        return JObject.c()
                .add("timestamp", System.currentTimeMillis())
                .add("code", 200)
                .add("message", "Success.")
                .gson();
    }

    /**
     * @return A 200 success.
     */
    static JsonObject SUCCESS(JsonObject data) {
        return JObject.c()
                .add("timestamp", System.currentTimeMillis())
                .add("code", 200)
                .add("message", "Success.")
                .addAll(data)
                .gson();
    }

    /**
     * @return A 404 error.
     */
    static JsonObject NO_RESULTS() {
        return JObject.c()
                .add("timestamp", System.currentTimeMillis())
                .add("code", 404)
                .add("message", "No results were found.")
                .gson();
    }

    /**
     * @return A 400 error.
     */
    static JsonObject INVALID_ARGUMENTS() {
        return JObject.c()
                .add("timestamp", System.currentTimeMillis())
                .add("code", 400)
                .add("message", "Invalid arguments were provided.")
                .gson();
    }

    /**
     * @param reason The reason for the error.
     * @return A 400 error.
     */
    static JsonObject INVALID_ARGUMENTS(String reason) {
        return JObject.c()
                .add("timestamp", System.currentTimeMillis())
                .add("code", 400)
                .add("message", "Invalid arguments were provided.")
                .add("reason", reason)
                .gson();
    }

    /**
     * @return A 400 error.
     */
    static JsonObject INVALID_TOKEN() {
        return JObject.c()
                .add("timestamp", System.currentTimeMillis())
                .add("code", 400)
                .add("message", "Invalid token provided.")
                .gson();
    }

    /**
     * @return A 401 error.
     */
    static JsonObject NO_AUTHORIZATION() {
        return JObject.c()
                .add("timestamp", System.currentTimeMillis())
                .add("code", 401)
                .add("message", "No authorization provided.")
                .gson();
    }

    /**
     * @return A 500 error.
     */
    static JsonObject INTERNAL_ERROR() {
        return JObject.c()
                .add("timestamp", System.currentTimeMillis())
                .add("code", 500)
                .add("message", "Internal server error.")
                .gson();
    }

    /**
     * @return A 429 error.
     */
    static JsonObject RATE_LIMITED() {
        return JObject.c()
                .add("timestamp", System.currentTimeMillis())
                .add("code", 429)
                .add("message", "You are being rate limited.")
                .gson();
    }
}

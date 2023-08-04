package moe.seikimo.laudiolin.utils;

import com.google.gson.JsonObject;
import lombok.Getter;
import moe.seikimo.laudiolin.objects.JObject;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@SuppressWarnings("KotlinInternalInJava")
public interface HttpUtils {
    OkHttpClient CLIENT
            = new OkHttpClient.Builder()
            .addInterceptor(HttpUtils::interceptRequest)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

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
}

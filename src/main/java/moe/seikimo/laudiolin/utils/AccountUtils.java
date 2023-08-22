package moe.seikimo.laudiolin.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.gateway.Gateway;
import moe.seikimo.laudiolin.models.data.User;
import moe.seikimo.laudiolin.objects.JObject;
import okhttp3.Request;
import okhttp3.RequestBody;

import javax.annotation.Nullable;
import java.io.IOException;

import static moe.seikimo.laudiolin.utils.HttpUtils.NO_AUTHORIZATION;

public interface AccountUtils {
    /**
     * Fetches an account's information by token.
     * This method uses the seiKiMo account API.
     *
     * @param token The token.
     * @return The account information, or null if not found.
     */
    @Nullable
    static JsonObject accountInfo(String token) {
        var request = new Request.Builder()
                .url(Config.get().seikimo.getBaseUrl() + "/account")
                .header("Authorization", token)
                .build();

        try (var response = HttpUtils.CLIENT
                .newCall(request).execute()) {
            // Check if the response was successful.
            if (!response.isSuccessful()) {
                return null;
            }

            // Parse the response body.
            var body = response.body();
            if (body == null) return null;

            return EncodingUtils.jsonDecode(
                    body.string(), JsonObject.class);
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Fetches an account's friends list by token.
     * This method uses the seiKiMo admin account API.
     *
     * @param token The token.
     * @return The friends list, or null if not found.
     */
    @Nullable
    static JsonArray friends(String token) {
        // Prepare the request body.
        var body = JObject.c()
                .add("token", token)
                .toString();

        // Prepare the backend request.
        var seikimo = Config.get().seikimo;
        var requestBody = RequestBody.create(
                body, HttpUtils.JSON_MEDIA_TYPE);
        var request = new Request.Builder()
                .url(seikimo.getBaseUrl() + "/account/friends")
                .method("POST", requestBody)
                .header("Authorization", seikimo.getAdminToken())
                .build();

        // Execute the request.
        try (var response = HttpUtils.makeRequest(request)) {
            // Check if the response executed.
            if (response == null) return null;
            // Check the response code.
            if (!response.isSuccessful()) return null;

            // Parse the response body.
            var responseBody = response.body();
            if (responseBody == null) return null;

            return EncodingUtils.jsonDecode(
                    responseBody.string(), JsonArray.class);
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Fetches an account's friends list by token.
     * This method uses the seiKiMo admin account API.
     *
     * @param token The token.
     * @return The friends list, or null if not found.
     */
    @Nullable
    static JsonArray guilds(String token) {
        // Prepare the request body.
        var body = JObject.c()
                .add("token", token)
                .toString();

        // Prepare the backend request.
        var seikimo = Config.get().seikimo;
        var requestBody = RequestBody.create(
                body, HttpUtils.JSON_MEDIA_TYPE);
        var request = new Request.Builder()
                .url(seikimo.getBaseUrl() + "/account/guilds")
                .method("POST", requestBody)
                .header("Authorization", seikimo.getAdminToken())
                .build();

        // Execute the request.
        try (var response = HttpUtils.makeRequest(request)) {
            // Check if the response executed.
            if (response == null) return null;
            // Check the response code.
            if (!response.isSuccessful()) return null;

            // Parse the response body.
            var responseBody = response.body();
            if (responseBody == null) return null;

            // De-serialize the response body.
            var guilds = EncodingUtils.jsonDecode(
                    responseBody.string(), JsonArray.class);
            if (guilds == null) return null;

            // Mark which guilds have an Elixir.
            var finalList = new JsonArray();
            for (var guild : guilds) {
                // Get the guild object.
                var guildObj = guild.getAsJsonObject();
                var guildId = guildObj.get("id").getAsString();

                // Add a list of all bots.
                var bots = new JsonArray();
                for (var bot : Gateway.getGuilds().entrySet()) {
                    if (bot.getValue().contains(guildId))
                        bots.add(bot.getKey());
                }
                guildObj.add("bots", bots);
                finalList.add(guildObj);
            }

            return finalList;
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Attempts to fetch a user by their token.
     * This method pulls the token from the HTTP context.
     *
     * @param ctx The Javalin context.
     * @return The user, or null if not found.
     */
    @Nullable
    static User getUser(Context ctx) {
        // Try reading the token.
        var token = HttpUtils.getToken(ctx);
        if (token == null) {
            ctx.status(401).json(NO_AUTHORIZATION());
            return null;
        }

        // Try fetching the user.
        var user = AccountUtils.getUser(token);
        if (user == null) {
            ctx.status(401).json(NO_AUTHORIZATION());
            return null;
        }

        return user;
    }

    /**
     * Attempts to fetch a user by their token.
     *
     * @param token The token.
     * @return The user, or null if not found.
     */
    @Nullable
    static User getUser(String token) {
        // Attempt to get the account information.
        var accountInfo = AccountUtils.accountInfo(token);
        if (accountInfo == null) return null;

        // Perform a database lookup for the user's ID.
        var userId = accountInfo.get("id").getAsString();
        return User.getUserById(userId);
    }
}

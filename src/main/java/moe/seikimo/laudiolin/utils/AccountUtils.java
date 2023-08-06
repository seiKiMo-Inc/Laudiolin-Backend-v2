package moe.seikimo.laudiolin.utils;

import com.google.gson.JsonObject;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.models.data.User;
import okhttp3.Request;

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

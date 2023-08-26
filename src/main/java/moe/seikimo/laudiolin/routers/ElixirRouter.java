package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.gateway.Gateway;
import moe.seikimo.laudiolin.models.ElixirMessages;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.utils.AccountUtils;
import moe.seikimo.laudiolin.utils.EncodingUtils;

import static io.javalin.apibuilder.ApiBuilder.*;
import static moe.seikimo.laudiolin.utils.HttpUtils.SUCCESS;

public interface ElixirRouter {
    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        javalin.routes(() -> path("/elixir", () -> {
            get("guilds", ElixirRouter::fetchGuilds);
            post("guilds", ElixirRouter::applyGuilds);
        }));
    }

    /**
     * Fetches all guilds the user is in.
     *
     * @param ctx The Javalin context.
     */
    static void fetchGuilds(Context ctx) {
        // Pull arguments.
        var token = ctx.header("authorization");
        if (token == null || token.isEmpty()) {
            ctx.status(401);
            return;
        }

        // Get the user's guilds.
        var guilds = AccountUtils.guilds(token);
        ctx.status(200).json(SUCCESS(JObject.c()
                .add("guilds", guilds)
                .gson()));
    }

    /**
     * Sets the guilds the Elixir is in.
     *
     * @param ctx The Javalin context.
     */
    static void applyGuilds(Context ctx) {
        // Pull arguments.
        var token = ctx.header("authorization");
        if (token == null || token.isEmpty()) {
            ctx.status(401);
            return;
        }

        // Check the token.
        if (!token.equals(Config.get().elixir.getToken())) {
            ctx.status(401);
            return;
        }

        // Set the guilds.
        var body = ctx.bodyAsClass(ElixirMessages.Guilds.class);
        Gateway.getGuilds().put(body.getBotId(), body.getInGuilds());
        Gateway.getConnected().put(body.getBotId(), body.getConnectedGuilds());

        ctx.json(SUCCESS(JObject.c()
                .add("message", "Guilds updated.")
                .gson()));
    }
}

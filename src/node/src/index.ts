import { existsSync, readFileSync, writeFileSync } from "node:fs";

import Innertube, { ClientType, UniversalCache } from "youtubei.js";
import { Music } from "youtubei.js/dist/src/core/clients";

import { Logger } from "tslog";

import { initialize } from "@app/java";
import { generatePoToken } from "@app/utils";

console.clear(); // Clear the terminal screen.

// Create an application logger.
export const logger = new Logger();
// Define logger methods as console methods.
console.info = logger.info.bind(logger);
console.warn = logger.warn.bind(logger);
console.error = logger.error.bind(logger);
console.debug = logger.debug.bind(logger);
console.trace = logger.trace.bind(logger);

// Initialize the Java pipe.
initialize(parseInt(process.env["PORT"] ?? "47599"));

// Define constants.
export const storagePath = process.env["STORAGE_PATH"] ?? `${process.cwd()}/files`;

// Create a YouTube application.
export let youtube: Innertube;
export let ytMusic: Music;

export let search: Innertube;
export let searchMusic: Music;

(async () => {
    // Generate an origin token.
    const [poToken, visitorData] = await generatePoToken();

    // Initialize YouTube instance.
    youtube = await Innertube.create({
        cache: new UniversalCache(true, storagePath),
        // client_type: ClientType.WEB_CREATOR,
        po_token: poToken,
        visitor_data: visitorData,
        retrieve_player: true,
        enable_session_cache: false,
        generate_session_locally: false
    });
    ytMusic = youtube.music;

    search = await Innertube.create({
        cache: new UniversalCache(true, storagePath),
        // client_type: ClientType.WEB_CREATOR
    });
    searchMusic = search.music;

    // Prepare to do YouTube authentication.
    youtube.session.on("auth-pending", (data) => {
        logger.info(`Go to ${data.verification_url} in your browser and enter the code ${data.user_code} to authenticate.`)
    });
    youtube.session.on("auth", ({ credentials }) => {
        logger.info("Successfully authenticated.");

        // Write the credentials to the cache.
        writeFileSync("credentials.json", JSON.stringify(credentials));
    });
    youtube.session.on("update-credentials", async ({ credentials }) => {
        await youtube.session.oauth.cacheCredentials();

        // Write the credentials to the cache.
        writeFileSync("credentials.json", JSON.stringify(credentials));
    });

    // Do the authentication.
    let credentials: any | undefined = undefined;
    if (existsSync("credentials.json")) {
        credentials = JSON.parse(readFileSync("credentials.json", "utf-8"));
    }

    await youtube.session.signIn(credentials);
})();

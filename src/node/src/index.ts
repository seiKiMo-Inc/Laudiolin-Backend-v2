import { writeFileSync, readFileSync, existsSync } from "node:fs";

import Innertube, { UniversalCache } from "youtubei.js";
import { Music } from "youtubei.js/dist/src/core/clients";

import { Logger } from "tslog";

import { initialize } from "./java";

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
(async () => {
    youtube = await Innertube.create({
        cache: new UniversalCache(true, storagePath)
    });
    ytMusic = youtube.music;

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

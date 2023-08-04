import Innertube from "youtubei.js";
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
initialize(process.env["PIPE_NAME"] ?? "laudiolin");

// Define constants.
export const storagePath = process.env["STORAGE_PATH"] ?? `${process.cwd()}/files`;

// Create a YouTube application.
export let youtube: Innertube;
export let ytMusic: Music;
(async () => {
    youtube = await Innertube.create();
    ytMusic = youtube.music;
})();

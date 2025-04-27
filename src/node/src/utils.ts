import { Track } from "@app/Messages";

import Innertube, { YTNodes } from "youtubei.js";

import { JSDOM } from "jsdom";
import { BG } from "bgutils-js";
import type { BgConfig } from "bgutils-js/dist/utils";

/**
 * Extracts the YouTube video ID from a URL.
 * @param url The URL to extract the ID from.
 */
export function extractId(url: string): string {
    return url.split("/")[3];
}

/**
 * Converts a stream to an iterable.
 *
 * @param stream The stream to convert.
 */
export async function streamToIterable(stream: ReadableStream<Uint8Array>): Promise<Uint8Array> {
    const reader = stream.getReader();
    const chunks: Uint8Array[] = [];

    try {
        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            chunks.push(value);
        }
    } finally {
        reader.releaseLock();
    }

    return Uint8Array.from(chunks.flatMap(
        chunk => Array.from(chunk)));
}

/**
 * Converts a YouTube video to a search result.
 *
 * @param video The video to convert.
 */
export function parseVideo(video: YTNodes.Video | YTNodes.PlaylistVideo): Track {
    return {
        title: video.title.text ?? "",
        artists: [video.author.name],
        icon: video.thumbnails[0].url ?? "",
        url: "https://youtu.be/" + (video.id ?? ""),
        id: video.id,
        duration: video.duration.seconds
    };
}

/**
 * Generates a proof of origin token.
 */
export async function generatePoToken(): Promise<[string, string]> {
    // Generate an origin token and accompanying visitor data.
    const client = await Innertube.create({ retrieve_player: false });
    const visitorData = client.session.context.client.visitorData;
    if (!visitorData) {
        throw new Error("Failed to generate visitor data.");
    }

    const dom = new JSDOM();
    Object.assign(globalThis, {
        window: dom.window,
        document: dom.window.document
    });

    const config: BgConfig = {
        fetch: (url, options) => fetch(url, options),
        globalObj: globalThis,
        identifier: visitorData,
        requestKey: "O43z0dpjhgX20SCx4KAo"
    };

    const challenge = await BG.Challenge.create(config);

    if (!challenge || !challenge.interpreterJavascript.privateDoNotAccessOrElseSafeScriptWrappedValue) {
        throw new Error("Failed to generate PoToken.");
    }

    const script = challenge.interpreterJavascript.privateDoNotAccessOrElseSafeScriptWrappedValue;
    if (script) {
        new Function(script)();
    } else {
        throw new Error("Could not load VM.");
    }

    const { poToken } = await BG.PoToken.generate({
        program: challenge.program,
        globalName: challenge.globalName,
        bgConfig: config
    });

    if (!poToken) {
        throw new Error("Failed to generate PoToken.");
    }

    return [poToken, visitorData];
}

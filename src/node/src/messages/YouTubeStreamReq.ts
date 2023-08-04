import { YouTubeStreamReq, YouTubeStreamRsp } from "@app/Messages";

import { Socket } from "net";

import { sendPacket } from "@app/java";
import { extractId } from "@app/utils";

import { youtube } from "@app/index";
import { streamToIterable } from "@app/utils";

import { DownloadOptions } from "youtubei.js";

const downloadOptions: DownloadOptions = {
    type: "audio", quality: "best", format: "any"
};

/**
 * Downloads a portion of a YouTube video.
 *
 * @param id The ID of the video to download.
 * @param quality The quality of the video to download.
 * @param min The minimum byte to download.
 * @param max The maximum byte to download.
 */
async function streamInternal(
    id: string, quality: string,
    min: number, max: number
): Promise<{ buffer: Uint8Array, length: number }> {
    // Fetch the data required for streaming.
    const streamingData =
        await youtube.getStreamingData(id, downloadOptions);
    const length = streamingData.content_length ?? 0;
    if (length == 0) throw new Error("Invalid content length.");

    // Download the video.
    const options = {
        ...downloadOptions,
        quality: quality == "High" ? "best" : "bestefficiency",
        range: { start: min, end: Math.min(max, length) }
    };
    const stream =
        await youtube.download(id, options);

    // Convert the stream into a buffer.
    const buffer = await streamToIterable(stream);
    // Return the buffer.
    return { length, buffer };
}

export default async function(socket: Socket, retcode: number, req: Buffer) {
    const { videoId, quality, start, end } = YouTubeStreamReq.fromBinary(req);

    // Parse the video ID.
    let id = videoId;
    if (id.includes("http"))
        id = extractId(id);

    // Attempt to stream the video.
    const { buffer, length } =
        await streamInternal(id, quality, start, end);

    // Send the response packet.
    sendPacket(socket, retcode, YouTubeStreamRsp.toBinary(
        { data: buffer, contentLength: length }));
}

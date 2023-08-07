import { YouTubeDownloadReq, YouTubeDownloadRsp } from "@app/Messages";
import { sendPacket } from "@app/java";

import { WebSocket } from "ws";
import { existsSync, createWriteStream, rmSync } from "fs";
import ffmpeg from "fluent-ffmpeg";
import { join } from "path";

import { youtube, storagePath } from "@app/index";
import { extractId, streamToIterable } from "@app/utils";

/**
 * Handles the downloading of a YouTube video.
 *
 * @param id The ID of the video to download.
 */
async function downloadInternal(id: string): Promise<string> {
    // Save the file to the disk.
    const filePath = join(storagePath, `${id}.mp3`);

    // Check if the file already exists.
    if (existsSync(filePath)) {
        return filePath; // Return the path to the file.
    }

    // Create a stream for the video.
    const stream = await youtube.download(id, {
        type: "audio",
        quality: "best",
        format: "any"
    });

    // Write the stream to a temporary file.
    const temporary = `${filePath}.tmp`;
    const fileStream = createWriteStream(temporary);
    const chunks = await streamToIterable(stream);
    fileStream.write(chunks);
    fileStream.end();

    // Convert the data with ffmpeg and pipe to the file.
    await new Promise<string>((resolve, reject) => {
        ffmpeg(temporary)
            .on("end", () => {
                resolve(filePath);

                // Delete the temporary file.
                rmSync(temporary, { force: true });
            })
            .on("error", err => {
                reject(err); console.error("Error: ", err);
            })
            .audioBitrate(128)
            .audioFrequency(44100)
            .audioChannels(2)
            .save(filePath)
    });

    return filePath;
}

export default async function(socket: WebSocket, retcode: number, req: Buffer){
    const request = YouTubeDownloadReq.fromBinary(req);

    // Parse the video ID.
    let id = request.videoId;
    if (id.includes("http"))
        id = extractId(id);

    // Get the file path.
    const filePath = await downloadInternal(id);

    // Send the response packet.
    sendPacket(socket, retcode,
        YouTubeDownloadRsp.toBinary({ filePath }));
}

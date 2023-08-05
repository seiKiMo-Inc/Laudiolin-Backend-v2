import { Track } from "@app/Messages";
import { YTNodes } from "youtubei.js";

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

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        chunks.push(value);
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

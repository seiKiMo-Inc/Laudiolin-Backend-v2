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

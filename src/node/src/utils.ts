/**
 * Extracts the YouTube video ID from a URL.
 * @param url The URL to extract the ID from.
 */
export function extractId(url: string): string {
    return url.split("/")[3];
}

import { Track, YouTubeSearchReq, YouTubeSearchRsp } from "@app/Messages";
import { sendPacket } from "@app/java";

import { Socket } from "net";

import { youtube, ytMusic } from "@app/index";
import { parseVideo } from "@app/utils";

import { YTNodes, YTMusic } from "youtubei.js";
import { ObservedArray } from "youtubei.js/dist/src/parser/helpers";

/**
 * Handles the searching of a YouTube video.
 * This searches YouTube Music.
 *
 * @param query The query to search for.
 */
async function searchMusic(query: string): Promise<Track[]> {
    const search = await ytMusic.search(query);
    return await parseTracks(search);
}

/**
 * Parses a YouTube Music search into a collection of tracks.
 * @param search The search to parse.
 * @return Parsed search results.
 */
async function parseTracks(search: YTMusic.Search): Promise<Track[]> {
    // Extract different search results.
    // These are sorted from high -> low priority.
    let songs, albums, videos;
    try { songs = await search.getMore(search.songs); } catch { }
    try { albums = await search.getMore(search.albums); } catch { }
    try { videos = await search.getMore(search.videos); } catch { }

    // Parse each search result into a collection of tracks.
    let songTracks: Track[] = [],
        albumTracks: Track[] = [],
        videoTracks: Track[] = [];
    if (songs && songs.contents) songTracks = await parseShelf(songs.contents);
    if (albums && albums.contents) albumTracks = await parseShelf(albums.contents);
    if (videos && videos.contents) videoTracks = await parseShelf(videos.contents);

    // Merge all tracks into a single collection.
    return [...songTracks, ...albumTracks, ...videoTracks];
}

/**
 * Returns search results from within an album.
 * @param album The album to search within.
 */
async function parseAlbum(album: YTNodes.MusicResponsiveListItem): Promise<Track[]> {
    // Check the item type.
    if (album.item_type != "album") return [];

    // Get the album ID.
    const id = album.id;
    if (!id) return [];

    // Get the album tracks.
    const result = await ytMusic.getAlbum(id);
    const tracks = result.contents;

    // Get album data.
    const icon = album.thumbnails[0].url;
    const artist = album.author ? album.author.name : "Unknown";

    const results: Track[] = [];
    for (const track of tracks) {
        const result = parseItem(track, icon, artist);
        if (result) results.push(result);
    }

    return results;
}

/**
 * Parses a music shelf object into search results.
 * @param shelf The shelf to parse. (should be a "getMore" result)
 */
async function parseShelf(shelf: ObservedArray<YTNodes.MusicShelf | YTNodes.MusicCardShelf | YTNodes.ItemSection>): Promise<Track[]> {
    if (!shelf) return [];

    const results: Track[] = [];
    for (const node of shelf) {
        if (!(node instanceof YTNodes.MusicShelf)) continue;

        // Get the shelf content.
        const content = node.contents;
        if (!content) continue;

        // Parse the shelf content.
        for (const item of content) {
            // Check the item type.
            if (item.item_type == "album") results.push(...(await parseAlbum(item)));
            else {
                const result = parseItem(item);
                if (result) results.push(result);
            }
        }
    }

    return results;
}

/**
 * Parses an item into a search result.
 * @param item The item to parse.
 * @param icon (optional) The icon to use.
 * @param artist (optional) The artist to use.
 */
function parseItem(
    item: YTNodes.MusicResponsiveListItem,
    icon: string | null = null,
    artist: string | null = null
): Track | undefined {
    if (!item.id) return undefined;

    let artists: string[] = [];
    if (artist) artists.push(artist);

    // Check the artists type.
    if (item.artists) {
        for (const artist of item.artists) {
            if (artist.name) artists.push(artist.name);
        }
    } else if (item.author) {
        artists.push(item.author.name);
    }

    return {
        title: item.title ?? "",
        artists: artists,
        icon: icon ?? item.thumbnails[0].url,
        url: "https://youtu.be/" + item.id,
        id: item.id,
        duration: item.duration?.seconds ?? -1
    };
}

/**
 * Handles the searching of a YouTube video.
 * This searches regular YouTube.
 *
 * @param query The query to search for.
 */
async function searchRegular(query: string): Promise<Track[]> {
    const search = await youtube.search(query);
    const tracks = search.videos;

    // Parse the search results.
    const results: Track[] = [];
    for (const track of tracks) {
        if (track != null && track instanceof YTNodes.Video)
            results.push(parseVideo(track));
    }

    if (results.length > 9) {
        return results.slice(0, 9);
    } else {
        return results;
    }
}

export default async function(socket: Socket, retcode: number, req: Buffer){
    const { query, youtubeMusic } = YouTubeSearchReq.fromBinary(req);

    try {
        // Perform the search.
        let results: Track[];
        if (youtubeMusic) {
            results = await searchMusic(query);
        } else {
            results = await searchRegular(query);
        }

        // Send the response packet.
        sendPacket(socket, retcode,
            YouTubeSearchRsp.toBinary({ successful: true, results }));
    } catch (error) {
        // Log the error to the console.
        console.error(error);

        // Send the response packet.
        sendPacket(socket, retcode,
            YouTubeSearchRsp.toBinary({ successful: false, results: [] }));
    }
}

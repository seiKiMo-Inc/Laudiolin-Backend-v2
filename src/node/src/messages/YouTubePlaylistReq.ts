import { Playlist, YouTubePlaylistReq, YouTubePlaylistRsp } from "@app/Messages";
import { sendPacket } from "@app/java";

import { Socket } from "net";

import { youtube } from "@app/index";
import { parseVideo } from "@app/utils";

import { YTNodes } from "youtubei.js";

export default async function(socket: Socket, retcode: number, req: Buffer) {
    try {
        const { playlistUrl } = YouTubePlaylistReq.fromBinary(req);

        // Extract the playlist ID.
        const playlistId = playlistUrl.includes("playlist?list=") ?
            playlistUrl.split("playlist?list=")[1] :
            playlistUrl.split("https://youtu.be/")[1];

        // Get the playlist information.
        const { items, info } =
            await youtube.getPlaylist(playlistId);

        // Create the playlist data.
        const playlist: Playlist = {
            name: info.title ?? "",
            description: info.description ?? "",
            icon: info.thumbnails[0].url ?? "",
            isPrivate: info.privacy != "PUBLIC",
            tracks: []
        };

        // Parse the playlist tracks.
        for (const item of items) {
            if (item instanceof YTNodes.PlaylistVideo)
                playlist.tracks.push(parseVideo(item));
        }

        // Send the response.
        sendPacket(socket, retcode,
            YouTubePlaylistRsp.toBinary({ successful: true, playlist: playlist }));
    } catch (error) {
        // Log the error.
        console.error(error);

        // Send the response packet.
        sendPacket(socket, retcode,
            YouTubePlaylistRsp.toBinary({ successful: false, playlist: undefined }))
    }
}

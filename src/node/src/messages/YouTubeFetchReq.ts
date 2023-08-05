import { SearchResult, YouTubeFetchReq, YouTubeFetchRsp } from "@app/Messages";
import { sendPacket } from "@app/java";

import { Socket } from "net";

import { youtube } from "@app/index";

export default async function(socket: Socket, retcode: number, req: Buffer) {
    const { videoId } = YouTubeFetchReq.fromBinary(req);

    try {
        // Perform the search.
        const video = await youtube.getInfo(videoId);
        const basicInfo = video.basic_info;

        // Parse the info.
        if (!basicInfo.id || !basicInfo.duration)
            throw new Error("Invalid video result.");

        const result: SearchResult = {
            title: basicInfo.title ?? "",
            artists: [basicInfo.author ?? ""],
            icon: basicInfo.thumbnail?.[0].url ?? "",
            url: `https://youtu.be/${basicInfo.id}`,
            duration: basicInfo.duration ?? 0,
            id: basicInfo.id ?? ""
        };

        // Send the response packet.
        sendPacket(socket, retcode,
            YouTubeFetchRsp.toBinary({ successful: true, result }));
    } catch (error) {
        // Log the error to the console.
        console.error(error);

        // Send the response packet.
        sendPacket(socket, retcode,
            YouTubeFetchRsp.toBinary({ successful: false, result: undefined }));
    }
}

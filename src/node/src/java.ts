import { WebSocket, WebSocketServer } from "ws";
import { createServer } from "http";

import { PacketIds } from "./Messages";

const messages: { [key: number]: any } = {
    [PacketIds._YouTubeSearchReq]: require("@messages/YouTubeSearchReq").default,
    [PacketIds._YouTubeDownloadReq]: require("@messages/YouTubeDownloadReq").default,
    [PacketIds._YouTubeStreamReq]: require("@messages/YouTubeStreamReq").default,
    [PacketIds._YouTubeFetchReq]: require("@messages/YouTubeFetchReq").default,
    [PacketIds._YouTubePlaylistReq]: require("@messages/YouTubePlaylistReq").default,
};


/**
 * Initializes the Java websocket server.
 *
 * @param port The port to host the server on.
 */
export function initialize(port: number): void {
    const server = createServer();
    const wss = new WebSocketServer({ server });

    // Listen for connections.
    wss.on("connection", handleConnection);
    server.listen(port, () => console.info(
        `Listening for connections on ${port}.`));
}

/**
 * Handles a new socket connecting.
 *
 * @param socket The socket that connected.
 */
function handleConnection(socket: WebSocket): void {
    // Listen for messages.
    socket.on("message", data => handleMessage(socket, data as Buffer));
    socket.on("connect", () => console.info("Connected to Java."));
}

/**
 * Handles a message from Java.
 *
 * @param socket The socket that sent the message.
 * @param data The message data.
 */
async function handleMessage(socket: WebSocket, data: Buffer): Promise<void> {
    if (data.length < 2) return; // Ignore empty messages.
    if (data[0] != 0x2) return; // Ignore missed-origin messages.

    // Read the packet ID.
    const retcode = data.readInt32BE(1);
    const id = data.readInt32BE(5);
    const packetLength = data.readInt32BE(9);
    const packetData = data.subarray(13, 13 + packetLength);

    // Handle the packet.
    const handler = messages[id];
    if (!handler) return console.error(`No handler for packet ${id}.`);

    try {
        await handler(socket, retcode, packetData);
    } catch (error) {
        console.error(`Error handling packet ${id}. ${error}`);
    }
}

/**
 * Sends a packet to Java.
 *
 * @param socket The socket to send the packet on.
 * @param retcode The response code to send.
 * @param data The packet data.
 */
export function sendPacket(socket: WebSocket, retcode: number, data: Uint8Array): void {
    // Wrap the data as a buffer.
    const dataBuf = Buffer.from(data);

    // Create the packet buffer.
    const buffer = Buffer.alloc(9 + data.length);
    buffer[0] = 0x1; // Write the origin byte.
    buffer.writeInt32BE(retcode, 1); // Write the response code.
    buffer.writeInt32BE(data.length, 5); // Write the packet length.
    dataBuf.copy(buffer, 9); // Write the packet data.

    // Send the packet.
    socket.send(buffer);
}

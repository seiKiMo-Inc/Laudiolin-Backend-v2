import { createServer, Socket } from "net";
import { join } from "path";

import { PacketIds } from "./Messages";

const messages: { [key: number]: any } = {
    [PacketIds._YouTubeSearchReq]: require("@messages/YouTubeSearchReq").default,
    [PacketIds._YouTubeDownloadReq]: require("@messages/YouTubeDownloadReq").default
};


/**
 * Initializes the Java pipe.
 *
 * @param pipe The pipe to initialize.
 */
export function initialize(pipe: string): void {
    const path = join("\\\\?\\pipe", pipe);
    const server = createServer();

    // Listen for connections.
    server.listen(path, () => console.info(
        `Listening for connections on ${path}.`));
    server.on("connection", handleConnection);
}

/**
 * Handles a new socket connecting.
 *
 * @param socket The socket that connected.
 */
function handleConnection(socket: Socket): void {
    // Listen for messages.
    socket.on("data", data => handleMessage(socket, data));
    socket.on("connect", () => console.info("Connected to Java."));
}

/**
 * Handles a message from Java.
 *
 * @param socket The socket that sent the message.
 * @param data The message data.
 */
async function handleMessage(socket: Socket, data: Buffer): Promise<void> {
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
export function sendPacket(socket: Socket, retcode: number, data: Uint8Array): void {
    // Wrap the data as a buffer.
    const dataBuf = Buffer.from(data);

    // Create the packet buffer.
    const buffer = Buffer.alloc(9 + data.length);
    buffer[0] = 0x1; // Write the origin byte.
    buffer.writeInt32BE(retcode, 1); // Write the response code.
    buffer.writeInt32BE(data.length, 5); // Write the packet length.
    dataBuf.copy(buffer, 9); // Write the packet data.

    // Send the packet.
    socket.write(buffer);
}

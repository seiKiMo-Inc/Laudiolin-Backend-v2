package moe.seikimo.laudiolin.utils;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface ImageUtils {
    String THUMBNAIL_URL = "https://i.ytimg.com/vi/%s/hq720.jpg";

    /**
     * Gets the YouTube thumbnail for a video.
     *
     * @param videoId The video ID.
     * @return The thumbnail.
     * @throws IOException If an error occurs while making the request.
     */
    static byte[] getYouTubeThumbnail(String videoId) throws IOException {
        var imageBytes = HttpUtils.makeRequest(THUMBNAIL_URL.formatted(videoId));
        if (imageBytes == null) {
            return null;
        }

        // Check if the thumbnail is a bordered YouTube thumbnail.
        var image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        var points = image.getRGB(
                0, 0, 250, 720,
                null, 0, 250);
        if (points.length == 0) return imageBytes;

        // Check if every single point in this image is the same color.
        var referenceColor = points[0];
        for (var point : points) {
            if (point != referenceColor) {
                return imageBytes;
            }
        }

        // If the thumbnail is a bordered YouTube thumbnail, crop it.
        var croppedImage = image.getSubimage(280, 0, 720, 720);
        var outputStream = new ByteArrayOutputStream();
        ImageIO.write(croppedImage, "jpg", outputStream);

        return outputStream.toByteArray();
    }
}

package moe.seikimo.laudiolin.files;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import lombok.Getter;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.Constants;
import moe.seikimo.laudiolin.utils.EncodingUtils;
import moe.seikimo.laudiolin.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public final class LocalFileManager {
    @Getter private static final Logger logger
            = LoggerFactory.getLogger("File Manager");

    @Getter private static final Map<String, LocalTrack> localTracks
            = new HashMap<>();

    private static File coversDir, metadataDir;

    /**
     * Loads all local tracks from the file system.
     */
    public static void initialize() {
        var tracksPath = Constants.TRACKS_PATH;
        if (!tracksPath.exists() || !tracksPath.canRead()) {
            throw new RuntimeException("Unable to read the tracks directory.");
        }

        // Check if the subdirectories exist.
        LocalFileManager.coversDir = new File(tracksPath, "covers");
        if (!coversDir.exists() && !coversDir.mkdirs()) {
            throw new RuntimeException("Unable to make the 'covers' directory.");
        }
        LocalFileManager.metadataDir = new File(tracksPath, "metadata");
        if (!metadataDir.exists() && !metadataDir.mkdirs()) {
            throw new RuntimeException("Unable to make the 'metadata' directory.");
        }

        // Get all files in the directory.
        var files = tracksPath.listFiles();
        if (files == null) return;

        for (var file : files) try {
            if (file.isDirectory()) continue;
            LocalFileManager.readTrackData(file);
        } catch (Exception exception) {
            LocalFileManager.getLogger().warn("Unable to read local file.", exception);
        }
    }

    /**
     * Reads track metadata and stores it in memory.
     *
     * @param track The path to the track.
     */
    private static void readTrackData(File track)
            throws InvalidDataException, UnsupportedTagException, IOException {
        var baseUrl = Config.get().getAppTarget();

        // Read the file's data.
        var mp3 = new Mp3File(track);
        var fileName = FileUtils.fileName(track);
        var fileData = Files.readAllBytes(track.toPath());

        // Determine details about the track.
        var fileHash = EncodingUtils.sha256Hash(fileData, 16);
        if (fileHash == null || fileHash.length() != 16)
            throw new RuntimeException("Unable to hash track.");

        var coverFile = new File(coversDir, track.getName() + ".png");

        var duration = (int) mp3.getLengthInSeconds();
        var trackUrl = baseUrl + "/track/" + fileHash;
        var coverUrl = baseUrl + "/icon/" + fileHash;

        TrackData data;
        if (!mp3.hasId3v2Tag()) {
            // Check if metadata exists for the track.
            var metadataFile = new File(metadataDir, fileName + ".json");
            if (!metadataFile.exists()) {
                throw new RuntimeException("No metadata found for '%s'.".formatted(fileName));
            }

            // Load the metadata.
            var metadata = EncodingUtils.jsonDecode(
                    new FileReader(metadataFile), TrackData.class);
            // Set runtime fields.
            metadata
                    .setId(fileHash)
                    .setDuration(duration);

            // Check if an icon URL was provided.
            if (metadata.getIcon() == null || metadata.getIcon().isEmpty()) {
                // Check if album art exists for the track.
                if (!coverFile.exists()) {
                    throw new RuntimeException("No album art found for '%s'.".formatted(fileName));
                }

                // Set the path to the icon.
                metadata.setIcon(coverUrl);
            }

            // Check if a source URL was provided.
            if (metadata.getUrl() == null || metadata.getUrl().isEmpty()) {
                // Set the path to the file.
                metadata.setUrl(trackUrl);
            }

            // Validate the remaining metadata.
            if (!TrackData.valid(metadata)) {
                throw new RuntimeException("Invalid metadata for track '%s'.".formatted(fileName));
            }

            data = metadata;
        } else {
            var tag = mp3.getId3v2Tag();

            // Copy out the album art of the track.
            var albumImage = tag.getAlbumImage();
            if (albumImage == null && !coverFile.exists()) {
                throw new RuntimeException("No album image provided");
            } else if (albumImage != null && !coverFile.exists()) {
                Files.write(coverFile.toPath(), albumImage);
            }

            data = TrackData.builder()
                    .id(fileHash)
                    .title(tag.getTitle())
                    .artist(tag.getArtist())
                    .url(trackUrl)
                    .icon(coverUrl)
                    .duration(duration)
                    .build();
        }

        LocalFileManager.getLocalTracks().put(fileHash,
                new LocalTrack(data, track, coverFile));
    }
}

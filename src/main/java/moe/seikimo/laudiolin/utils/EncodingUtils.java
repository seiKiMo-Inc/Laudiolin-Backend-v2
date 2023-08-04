package moe.seikimo.laudiolin.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import moe.seikimo.laudiolin.interfaces.DatabaseObject;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public interface EncodingUtils {
    Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    /**
     * Converts an object to JSON.
     *
     * @param object The object to convert.
     * @return The JSON.
     */
    static JsonElement toJson(Object object) {
        return GSON.toJsonTree(object);
    }

    /**
     * Decodes an object from JSON.
     *
     * @param reader The reader to read from.
     * @param type The type of the object.
     * @return The decoded object.
     */
    static <T> T jsonDecode(Reader reader, Class<T> type) {
        return GSON.fromJson(reader, type);
    }

    /**
     * Decodes an object from JSON.
     *
     * @param json The JSON to decode.
     * @param type The type of the object.
     * @return The decoded object.
     */
    static <T> T jsonDecode(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    /**
     * Encodes an object to JSON.
     *
     * @param object The object to encode.
     * @return The encoded JSON.
     */
    static String jsonEncode(Object object) {
        return GSON.toJson(object);
    }

    /**
     * Encodes an object to JSON.
     *
     * @param object The object to encode.
     * @return The encoded JSON.
     */
    static String toString(Object object) {
        return EncodingUtils.jsonEncode(object);
    }

    /**
     * Encodes bytes to Base64.
     *
     * @param bytes The bytes to encode.
     * @return The encoded string.
     */
    static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Attempts to call the 'explain' method on all objects in the list.
     *
     * @param objects The objects to explain.
     * @return The list of JSON objects.
     */
    static List<JsonObject> explainAll(List<? extends DatabaseObject<?>> objects) {
        var list = new ArrayList<JsonObject>();
        for (var object : objects) {
            list.add(object.explain());
        }

        return list;
    }
}

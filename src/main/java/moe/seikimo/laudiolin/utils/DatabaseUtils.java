package moe.seikimo.laudiolin.utils;

import dev.morphia.query.filters.Filters;
import moe.seikimo.laudiolin.Laudiolin;
import moe.seikimo.laudiolin.interfaces.DatabaseObject;

import java.util.function.Supplier;

public interface DatabaseUtils {
    /**
     * Fetches a database object by a parameter.
     *
     * @param type The type of object to fetch.
     * @param param The parameter to search for.
     * @param value The value of the parameter.
     * @return The object, or null if not found.
     */
    static <T extends DatabaseObject<?>> T fetch(
            Class<T> type,
            String param, Object value
    ) {
        return Laudiolin.getDatastore().find(type)
                .filter(Filters.eq(param, value))
                .first();
    }

    /**
     * Creates a unique 20 digit ID.
     *
     * @param type The type of object to create the ID for.
     * @return The unique ID.
     */
    static String uniqueId(Class<? extends DatabaseObject<?>> type) {
        return DatabaseUtils.distinct(
                type, "_id",
                () -> RandomUtils.randomString(24));
    }

    /**
     * Requires a distinct value across all documents.
     *
     * @param type The type of object to search for.
     * @param searchFor The parameter to search for.
     * @param supplier The supplier to get the value from.
     * @return The value.
     */
    static <T> T distinct(Class<? extends DatabaseObject<?>> type, String searchFor, Supplier<T> supplier) {
        // Get the value.
        var value = supplier.get();
        var query = Laudiolin.getDatastore()
                .find(type).filter(Filters.eq(searchFor, value));

        // If the query is not empty, return the value.
        if (query.first() != null) {
            return DatabaseUtils.distinct(type, searchFor, supplier);
        }

        // Otherwise, return the value.
        return value;
    }
}

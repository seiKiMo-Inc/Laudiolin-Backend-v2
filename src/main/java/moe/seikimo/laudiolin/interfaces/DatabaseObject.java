package moe.seikimo.laudiolin.interfaces;

import com.google.gson.JsonObject;
import moe.seikimo.laudiolin.Laudiolin;

@SuppressWarnings("unchecked")
public interface DatabaseObject<T> {
    /**
     * Runs the function.
     * Saves the object to the database.
     * Returns the result of the function.
     *
     * @param function The function to run.
     * @return The result of the function.
     */
    default <R> R function(Function<R> function) {
        var result = function.run();
        this.save();
        return result;
    }

    /**
     * Runs the runnable.
     * Saves the object to the database.
     * Returns the object.
     *
     * @param runnable The runnable to run.
     * @return This object.
     */
    default T save(Runnable runnable) {
        runnable.run();
        return this.save();
    }

    /**
     * Saves this object to the database.
     */
    default T save() {
        Laudiolin.getDatastore().save(this);
        return (T) this;
    }

    /**
     * Deletes this object from the database.
     */
    default boolean delete() {
        return Laudiolin.getDatastore().delete(this).getDeletedCount() > 0;
    }

    /**
     * @return A plain representation of this object.
     */
    JsonObject explain();
}

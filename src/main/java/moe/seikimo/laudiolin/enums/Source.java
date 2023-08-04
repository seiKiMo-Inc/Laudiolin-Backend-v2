package moe.seikimo.laudiolin.enums;

public enum Source {
    ALL,
    YOUTUBE,
    SPOTIFY,
    UNKNOWN;

    /**
     * Identifies a source by the length of the ID.
     *
     * @param source The source.
     * @param id The ID.
     * @return The source.
     */
    public static Source identify(String source, String id) {
        if (id == null) return Source.UNKNOWN;

        try {
            if (source == null) throw new IllegalArgumentException();

            // Try to find a potential value.
            return Source.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // Identify based on ID length.
            return switch (id.length()) {
                default -> Source.UNKNOWN;
                case 11 -> Source.YOUTUBE;
                case 12, 22 -> Source.SPOTIFY;
            };
        }
    }
}

package moe.seikimo.laudiolin;

public interface Properties {
    /**
     * Determines whether the application is running in a headless environment.
     */
    boolean HEADLESS_ENVIRONMENT = "true".equals(System.getProperty("HEADLESS"));
}

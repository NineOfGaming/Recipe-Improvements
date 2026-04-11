package dev.nineofgaming.recipe_fallback.state;

public final class FallbackState {
    private static volatile boolean active;

    private FallbackState() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean activate() {
        boolean changed = !active;
        active = true;
        return changed;
    }

    public static void deactivate() {
        active = false;
    }
}

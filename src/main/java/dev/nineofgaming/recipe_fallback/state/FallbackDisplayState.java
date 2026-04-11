package dev.nineofgaming.recipe_fallback.state;

import dev.nineofgaming.recipe_fallback.recipe.FallbackRecipePayload;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FallbackDisplayState {
    private static final Set<RecipeDisplayId> fallbackDisplayIds = ConcurrentHashMap.newKeySet();

    private FallbackDisplayState() {
    }

    public static void clear() {
        fallbackDisplayIds.clear();
    }

    public static Set<RecipeDisplayId> snapshot() {
        return new LinkedHashSet<>(fallbackDisplayIds);
    }

    public static void setPayload(FallbackRecipePayload payload) {
        setDisplayIds(payload.recipeBookEntries().stream()
                .map(entry -> entry.contents().id())
                .toList());
    }

    public static void setDisplayIds(Collection<RecipeDisplayId> displayIds) {
        fallbackDisplayIds.clear();
        fallbackDisplayIds.addAll(displayIds);
    }

    public static void unmarkDisplayIds(Collection<RecipeDisplayId> displayIds) {
        fallbackDisplayIds.removeAll(displayIds);
    }

    public static boolean isFallbackDisplay(RecipeDisplayId displayId) {
        return displayId != null && fallbackDisplayIds.contains(displayId);
    }
}

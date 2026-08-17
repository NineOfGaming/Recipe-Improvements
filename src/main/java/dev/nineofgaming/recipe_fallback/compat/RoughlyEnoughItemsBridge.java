package dev.nineofgaming.recipe_fallback.compat;

import dev.nineofgaming.recipe_fallback.RecipeFallback;
import dev.nineofgaming.recipe_fallback.recipe.FallbackRecipePayload;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RoughlyEnoughItemsBridge {
    private static final Logger LOGGER = RecipeFallback.createLogger("REI");
    private static final ReiApi API = loadApi();
    private static final Set<RecipeDisplayId> emptySyncFallbackDisplayIds = ConcurrentHashMap.newKeySet();
    private static final Set<RecipeDisplayId> alwaysVisibleFallbackDisplayIds = ConcurrentHashMap.newKeySet();
    private static boolean disabled;
    private static volatile boolean emptySyncCleanupScheduled;

    private RoughlyEnoughItemsBridge() {
    }

    public static void reset() {
        emptySyncFallbackDisplayIds.clear();
        alwaysVisibleFallbackDisplayIds.clear();
        emptySyncCleanupScheduled = false;
    }

    public static boolean applyFallback(FallbackRecipePayload payload) {
        if (disabled || API == null) {
            return false;
        }

        try {
            Object registry = API.displayRegistryGetInstance.invoke(null);
            if (!API.displayRegistryImplClass.isInstance(registry)) {
                return false;
            }

            if (hasSyncedRecipes(registry)) {
                return false;
            }

            List<RecipeDisplayEntry> displays = payload.displays();
            Set<RecipeDisplayId> displayIds = new LinkedHashSet<>();
            displays.stream().map(RecipeDisplayEntry::id).forEach(displayIds::add);
            emptySyncFallbackDisplayIds.clear();
            emptySyncFallbackDisplayIds.addAll(displayIds);
            emptySyncCleanupScheduled = false;

            Runnable job = () -> {
                try {
                    API.removeSyncedRecipes.invoke(registry);
                    API.removeRecipes.invoke(registry, displayIds);
                    API.addRecipes.invoke(registry, displays);
                } catch (ReflectiveOperationException exception) {
                    disable("apply", exception);
                }
            };
            API.addJob.invoke(registry, job);
            return true;
        } catch (ReflectiveOperationException exception) {
            disable("schedule", exception);
            return false;
        }
    }

    public static boolean syncAlwaysVisibleFallback(FallbackRecipePayload payload, Set<RecipeDisplayId> desiredDisplayIds) {
        if (disabled || API == null) {
            return false;
        }

        try {
            Object registry = API.displayRegistryGetInstance.invoke(null);
            if (!API.displayRegistryImplClass.isInstance(registry)) {
                return false;
            }

            Set<RecipeDisplayId> desiredIds = new LinkedHashSet<>(desiredDisplayIds);
            Set<RecipeDisplayId> displayIdsToRemove = new LinkedHashSet<>(alwaysVisibleFallbackDisplayIds);
            displayIdsToRemove.removeAll(desiredIds);

            List<RecipeDisplayEntry> displaysToAdd = payload.displays().stream()
                    .filter(display -> desiredIds.contains(display.id()))
                    .filter(display -> !alwaysVisibleFallbackDisplayIds.contains(display.id()))
                    .toList();

            if (displayIdsToRemove.isEmpty()
                    && displaysToAdd.isEmpty()
                    && alwaysVisibleFallbackDisplayIds.equals(desiredIds)) {
                return false;
            }

            alwaysVisibleFallbackDisplayIds.clear();
            alwaysVisibleFallbackDisplayIds.addAll(desiredIds);

            Runnable job = () -> {
                try {
                    if (!displayIdsToRemove.isEmpty()) {
                        API.removeRecipes.invoke(registry, displayIdsToRemove);
                    }
                    if (!displaysToAdd.isEmpty()) {
                        API.addRecipes.invoke(registry, displaysToAdd);
                    }
                } catch (ReflectiveOperationException exception) {
                    disable("sync always-visible", exception);
                }
            };
            API.addJob.invoke(registry, job);
            return true;
        } catch (ReflectiveOperationException exception) {
            disable("schedule always-visible sync", exception);
            return false;
        }
    }

    public static boolean clearAlwaysVisibleFallback() {
        if (disabled || API == null || alwaysVisibleFallbackDisplayIds.isEmpty()) {
            return false;
        }

        try {
            Object registry = API.displayRegistryGetInstance.invoke(null);
            if (!API.displayRegistryImplClass.isInstance(registry)) {
                return false;
            }

            Set<RecipeDisplayId> displayIds = new LinkedHashSet<>(alwaysVisibleFallbackDisplayIds);
            alwaysVisibleFallbackDisplayIds.clear();
            Runnable job = () -> {
                try {
                    API.removeRecipes.invoke(registry, displayIds);
                } catch (ReflectiveOperationException exception) {
                    disable("clear always-visible", exception);
                }
            };
            API.addJob.invoke(registry, job);
            return true;
        } catch (ReflectiveOperationException exception) {
            disable("schedule always-visible clear", exception);
            return false;
        }
    }

    public static void clearFallbackIfServerSyncPresent() {
        if (disabled || API == null || emptySyncFallbackDisplayIds.isEmpty() || emptySyncCleanupScheduled) {
            return;
        }

        try {
            Object registry = API.displayRegistryGetInstance.invoke(null);
            if (!API.displayRegistryImplClass.isInstance(registry) || !hasSyncedRecipes(registry)) {
                return;
            }

            Set<RecipeDisplayId> displayIds = new LinkedHashSet<>(emptySyncFallbackDisplayIds);
            emptySyncCleanupScheduled = true;
            Runnable job = () -> {
                try {
                    API.removeRecipes.invoke(registry, displayIds);
                    emptySyncFallbackDisplayIds.removeAll(displayIds);
                } catch (ReflectiveOperationException exception) {
                    disable("clear", exception);
                } finally {
                    emptySyncCleanupScheduled = false;
                }
            };
            API.addJob.invoke(registry, job);
        } catch (ReflectiveOperationException exception) {
            emptySyncCleanupScheduled = false;
            disable("schedule cleanup", exception);
        }
    }

    private static ReiApi loadApi() {
        try {
            Class<?> displayRegistryClass = Class.forName("me.shedaniel.rei.api.client.registry.display.DisplayRegistry");
            Class<?> displayRegistryImplClass = Class.forName("me.shedaniel.rei.impl.client.registry.display.DisplayRegistryImpl");
            Class<?> displaysHolderImplClass = Class.forName("me.shedaniel.rei.impl.common.registry.displays.DisplaysHolderImpl");
            Field displaysHolderOriginsMap = displaysHolderImplClass.getDeclaredField("originsMap");
            displaysHolderOriginsMap.setAccessible(true);
            Object syncedOrigin = displayRegistryImplClass.getField("SYNCED").get(null);

            return new ReiApi(
                    displayRegistryImplClass,
                    displayRegistryClass.getMethod("getInstance"),
                    displayRegistryImplClass.getMethod("holder"),
                    displayRegistryImplClass.getMethod("addJob", Runnable.class),
                    displayRegistryImplClass.getMethod("addRecipes", List.class),
                    displayRegistryImplClass.getMethod("removeRecipes", Set.class),
                    displayRegistryImplClass.getMethod("removeSyncedRecipes"),
                    displaysHolderOriginsMap,
                    syncedOrigin
            );
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static boolean hasSyncedRecipes(Object registry) throws ReflectiveOperationException {
        assert API != null;
        Object holder = API.displayRegistryHolder.invoke(registry);
        @SuppressWarnings("unchecked")
        Map<Object, Object> origins = (Map<Object, Object>) API.displaysHolderOriginsMap.get(holder);
        synchronized (origins) {
            return origins.containsValue(API.syncedOrigin);
        }
    }

    private static void disable(String stage, ReflectiveOperationException exception) {
        if (disabled) {
            return;
        }

        disabled = true;
        LOGGER.warn("Disabled REI fallback bridge after {} failure", stage, exception);
    }

    private record ReiApi(Class<?> displayRegistryImplClass, Method displayRegistryGetInstance,
                          Method displayRegistryHolder, Method addJob, Method addRecipes,
                          Method removeRecipes, Method removeSyncedRecipes,
                          Field displaysHolderOriginsMap, Object syncedOrigin) {
    }
}

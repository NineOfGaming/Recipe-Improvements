package dev.nineofgaming.recipe_fallback.compat;

import dev.nineofgaming.recipe_fallback.RecipeFallback;
import dev.nineofgaming.recipe_fallback.recipe.FallbackRecipePayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class JustEnoughItemsBridge {
    private static final Logger LOGGER = RecipeFallback.createLogger("JEI");
    private static final JeiApi API = loadApi();
    private static final Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> alwaysVisibleFallbackRecipes =
            new ConcurrentHashMap<>();
    private static boolean disabled;

    private JustEnoughItemsBridge() {
    }

    public static void reset() {
        alwaysVisibleFallbackRecipes.clear();
    }

    public static boolean applyFallback(FallbackRecipePayload payload) {
        if (disabled || API == null) {
            return false;
        }

        try {
            API.setClientSyncedRecipes().invoke(null, payload.syncedRecipes());
            fireAfterRecipeSync();
            return true;
        } catch (ReflectiveOperationException exception) {
            disable("refresh", exception);
            return false;
        }
    }

    public static boolean syncAlwaysVisibleFallback(
            FallbackRecipePayload payload,
            Set<RecipeDisplayId> desiredDisplayIds
    ) {
        if (disabled || API == null || !API.canReadClientSyncedRecipes()) {
            return false;
        }

        try {
            RecipeMap currentRecipeMap = API.readClientSyncedRecipes();
            Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> baseRecipes =
                    toRecipeMap(currentRecipeMap != null ? currentRecipeMap : RecipeMap.EMPTY);
            stripTrackedFallbackRecipes(baseRecipes);

            Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> mergedRecipes = new LinkedHashMap<>(baseRecipes);
            Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> desiredFallbackRecipes =
                    payload.recipesForDisplayIds(desiredDisplayIds);
            Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> appliedFallbackRecipes = new LinkedHashMap<>();

            for (Map.Entry<ResourceKey<Recipe<?>>, RecipeHolder<?>> fallbackRecipe : desiredFallbackRecipes.entrySet()) {
                if (mergedRecipes.containsKey(fallbackRecipe.getKey())) {
                    continue;
                }

                mergedRecipes.put(fallbackRecipe.getKey(), fallbackRecipe.getValue());
                appliedFallbackRecipes.put(fallbackRecipe.getKey(), fallbackRecipe.getValue());
            }

            alwaysVisibleFallbackRecipes.clear();
            alwaysVisibleFallbackRecipes.putAll(appliedFallbackRecipes);
            API.setClientSyncedRecipes().invoke(null, RecipeMap.create(mergedRecipes.values()));
            fireAfterRecipeSync();
            return true;
        } catch (ReflectiveOperationException exception) {
            disable("always-visible sync", exception);
            return false;
        }
    }

    public static boolean clearAlwaysVisibleFallback() {
        if (disabled || API == null || !API.canReadClientSyncedRecipes() || alwaysVisibleFallbackRecipes.isEmpty()) {
            return false;
        }

        try {
            RecipeMap currentRecipeMap = API.readClientSyncedRecipes();
            Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> currentRecipes =
                    toRecipeMap(currentRecipeMap != null ? currentRecipeMap : RecipeMap.EMPTY);
            stripTrackedFallbackRecipes(currentRecipes);
            alwaysVisibleFallbackRecipes.clear();
            API.setClientSyncedRecipes().invoke(null, RecipeMap.create(currentRecipes.values()));
            fireAfterRecipeSync();
            return true;
        } catch (ReflectiveOperationException exception) {
            disable("clear always-visible", exception);
            return false;
        }
    }

    private static JeiApi loadApi() {
        try {
            Class<?> internalClass = Class.forName("mezz.jei.common.Internal");
            Class<?> lifecycleClass = Class.forName("mezz.jei.fabric.events.JeiLifecycleEvents");

            Method setClientSyncedRecipes = internalClass.getMethod("setClientSyncedRecipes", RecipeMap.class);
            Method getClientSyncedRecipes = null;
            for (Method method : internalClass.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && method.getReturnType() == RecipeMap.class) {
                    method.setAccessible(true);
                    getClientSyncedRecipes = method;
                    if (method.getName().toLowerCase().contains("clientsynced")) {
                        break;
                    }
                }
            }

            Field clientSyncedRecipesField = null;
            if (getClientSyncedRecipes == null) {
                for (Field field : internalClass.getDeclaredFields()) {
                    if (field.getType() != RecipeMap.class) {
                        continue;
                    }

                    field.setAccessible(true);
                    clientSyncedRecipesField = field;
                    if (field.getName().toLowerCase().contains("clientsynced")) {
                        break;
                    }
                }
            }

            Object afterRecipeSyncEvent = lifecycleClass.getField("AFTER_RECIPE_SYNC").get(null);
            Method eventInvoker = afterRecipeSyncEvent.getClass().getMethod("invoker");
            Method runnableRun = Runnable.class.getMethod("run");

            return new JeiApi(
                    setClientSyncedRecipes,
                    getClientSyncedRecipes,
                    clientSyncedRecipesField,
                    afterRecipeSyncEvent,
                    eventInvoker,
                    runnableRun
            );
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> toRecipeMap(RecipeMap recipeMap) {
        Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> recipes = new LinkedHashMap<>();
        for (RecipeHolder<?> recipeHolder : recipeMap.values()) {
            recipes.put(recipeHolder.id(), recipeHolder);
        }
        return recipes;
    }

    private static void stripTrackedFallbackRecipes(Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> recipes) {
        for (Map.Entry<ResourceKey<Recipe<?>>, RecipeHolder<?>> trackedFallbackRecipe
                : alwaysVisibleFallbackRecipes.entrySet()) {
            ResourceKey<Recipe<?>> recipeId = trackedFallbackRecipe.getKey();
            RecipeHolder<?> currentRecipe = recipes.get(recipeId);
            if (trackedFallbackRecipe.getValue().equals(currentRecipe)) {
                recipes.remove(recipeId);
            }
        }
    }

    private static void fireAfterRecipeSync() throws ReflectiveOperationException {
        assert API != null;
        Object invoker = API.eventInvoker().invoke(API.afterRecipeSyncEvent());
        API.runnableRun().invoke(invoker);
    }

    private static void disable(String stage, ReflectiveOperationException exception) {
        if (disabled) {
            return;
        }

        disabled = true;
        LOGGER.warn("Disabled JEI fallback bridge after {} failure", stage, exception);
    }

    private record JeiApi(Method setClientSyncedRecipes,
                          Method getClientSyncedRecipes,
                          Field clientSyncedRecipesField,
                          Object afterRecipeSyncEvent, Method eventInvoker, Method runnableRun) {
        private boolean canReadClientSyncedRecipes() {
            return this.getClientSyncedRecipes != null || this.clientSyncedRecipesField != null;
        }

        private RecipeMap readClientSyncedRecipes() throws ReflectiveOperationException {
            if (this.getClientSyncedRecipes != null) {
                return (RecipeMap) this.getClientSyncedRecipes.invoke(null);
            }

            if (this.clientSyncedRecipesField != null) {
                return (RecipeMap) this.clientSyncedRecipesField.get(null);
            }

            throw new NoSuchFieldException("JEI client synced recipe accessor is unavailable");
        }
    }
}

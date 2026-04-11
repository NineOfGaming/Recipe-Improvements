package dev.nineofgaming.recipe_fallback.recipe;

import dev.nineofgaming.recipe_fallback.RecipeFallback;
import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;

import java.util.List;
import java.util.Optional;

public final class FallbackRecipeLoader {
    private static final Object LOCK = new Object();

    private static CacheKey cachedKey;
    private static Optional<FallbackRecipePayload> cachedPayload;

    private FallbackRecipeLoader() {
    }

    public static void clear() {
        synchronized (LOCK) {
            cachedKey = null;
            cachedPayload = null;
        }
    }

    public static Optional<FallbackRecipePayload> getOrLoad(
            Minecraft minecraft,
            RegistryAccess.Frozen registryAccess,
            FeatureFlagSet enabledFeatures
    ) {
        return getOrLoad(minecraft, registryAccess, enabledFeatures, SourceMode.FALLBACK);
    }

    public static Optional<FallbackRecipePayload> getOrLoadServerOnly(
            Minecraft minecraft,
            RegistryAccess.Frozen registryAccess,
            FeatureFlagSet enabledFeatures
    ) {
        return getOrLoad(minecraft, registryAccess, enabledFeatures, SourceMode.SERVER_ONLY);
    }

    private static Optional<FallbackRecipePayload> getOrLoad(
            Minecraft minecraft,
            RegistryAccess.Frozen registryAccess,
            FeatureFlagSet enabledFeatures,
            SourceMode sourceMode
    ) {
        CacheKey key = CacheKey.create(minecraft, enabledFeatures, sourceMode);
        synchronized (LOCK) {
            if (cachedPayload != null && key.equals(cachedKey)) {
                return cachedPayload;
            }
        }

        Optional<FallbackRecipePayload> loadedPayload = load(minecraft, registryAccess, key);

        synchronized (LOCK) {
            cachedKey = key;
            cachedPayload = loadedPayload;
            return loadedPayload;
        }
    }

    private static Optional<FallbackRecipePayload> load(
            Minecraft minecraft,
            RegistryAccess.Frozen registryAccess,
            CacheKey key
    ) {
        CloseableResourceManager resourceManager = switch (key.sourceMode()) {
            case FALLBACK -> RecipePackResourceFactory.createFallback(minecraft, key.includeModdedRecipes());
            case SERVER_ONLY -> RecipePackResourceFactory.createServerKnownOnly(key.includeModdedRecipes());
        };

        if (resourceManager == null) {
            if (RecipeFallbackConfig.get().verboseLogging && key.sourceMode() == SourceMode.SERVER_ONLY) {
                RecipeFallback.LOGGER.info(
                        "Skipped server-only Show all recipes because no {} were available",
                        RecipePackResourceFactory.serverKnownDescription(key.includeModdedRecipes())
                );
            }
            return Optional.empty();
        }

        try (CloseableResourceManager closeableResourceManager = resourceManager) {
            FallbackRecipeManager recipeManager = new FallbackRecipeManager(registryAccess);
            recipeManager.load(closeableResourceManager, key.enabledFeatures());

            FallbackRecipePayload payload = recipeManager.createPayload();
            if (RecipeFallbackConfig.get().verboseLogging) {
                RecipeFallback.LOGGER.info(
                        "Loaded {} fallback recipe displays from {}",
                        payload.recipeBookEntries().size(),
                        key.description(minecraft)
                );
            }
            return Optional.of(payload);
        } catch (RuntimeException exception) {
            RecipeFallback.LOGGER.error("Failed to build fallback recipes", exception);
            return Optional.empty();
        }
    }

    private record CacheKey(
            FeatureFlagSet enabledFeatures,
            boolean includeModdedRecipes,
            List<String> selectedPackIds,
            SourceMode sourceMode
    ) {
        private static CacheKey create(Minecraft minecraft, FeatureFlagSet enabledFeatures, SourceMode sourceMode) {
            boolean includeModdedRecipes = RecipeFallbackConfig.get().includeModdedRecipes;
            List<String> selectedPackIds = switch (sourceMode) {
                case FALLBACK -> RecipePackResourceFactory.fallbackCacheSignature(minecraft, includeModdedRecipes);
                case SERVER_ONLY -> RecipePackResourceFactory.serverKnownCacheSignature(includeModdedRecipes);
            };
            return new CacheKey(enabledFeatures, includeModdedRecipes, selectedPackIds, sourceMode);
        }

        private String description(Minecraft minecraft) {
            return switch (this.sourceMode) {
                case FALLBACK -> RecipePackResourceFactory.fallbackDescription(minecraft, this.includeModdedRecipes);
                case SERVER_ONLY -> RecipePackResourceFactory.serverKnownDescription(this.includeModdedRecipes);
            };
        }
    }

    private enum SourceMode {
        FALLBACK,
        SERVER_ONLY
    }
}

package dev.nineofgaming.recipe_fallback.recipe;

import dev.nineofgaming.recipe_fallback.RecipeFallback;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModifiedRecipeDisplayLoader {
    private static final Object LOCK = new Object();
    private static final Object SIGNATURE_LOCK = new Object();

    private static CacheKey cachedKey;
    private static Map<String, Integer> cachedVanillaSignatureCounts = Map.of();
    private static final Map<RecipeDisplayEntry, String> cachedDisplaySignatures = new IdentityHashMap<>();

    private ModifiedRecipeDisplayLoader() {
    }

    public static void clear() {
        synchronized (LOCK) {
            cachedKey = null;
            cachedVanillaSignatureCounts = Map.of();
        }
        synchronized (SIGNATURE_LOCK) {
            cachedDisplaySignatures.clear();
        }
    }

    public static Map<String, Integer> getOrLoad(
            Minecraft minecraft,
            RegistryAccess.Frozen registryAccess,
            FeatureFlagSet enabledFeatures
    ) {
        CacheKey key = CacheKey.create(minecraft, enabledFeatures);
        synchronized (LOCK) {
            if (key.equals(cachedKey)) {
                return cachedVanillaSignatureCounts;
            }
        }

        Map<String, Integer> loadedSignatureCounts = load(minecraft, registryAccess, enabledFeatures);
        synchronized (LOCK) {
            cachedKey = key;
            cachedVanillaSignatureCounts = loadedSignatureCounts;
            return loadedSignatureCounts;
        }
    }

    public static String signature(RecipeDisplayEntry displayEntry, RegistryAccess.Frozen registryAccess) {
        synchronized (SIGNATURE_LOCK) {
            String cachedSignature = cachedDisplaySignatures.get(displayEntry);
            if (cachedSignature != null) {
                return cachedSignature;
            }
        }

        String signature = signature(displayEntry, RegistryOps.create(JsonOps.INSTANCE, registryAccess));
        synchronized (SIGNATURE_LOCK) {
            cachedDisplaySignatures.putIfAbsent(displayEntry, signature);
            return cachedDisplaySignatures.get(displayEntry);
        }
    }

    private static Map<String, Integer> load(
            Minecraft minecraft,
            RegistryAccess.Frozen registryAccess,
            FeatureFlagSet enabledFeatures
    ) {
        try {
            return loadSnapshot(minecraft, registryAccess, enabledFeatures, false).signatureCounts();
        } catch (RuntimeException exception) {
            RecipeFallback.LOGGER.error("Failed to classify modified recipe book displays", exception);
            return Map.of();
        }
    }

    private static RecipeSnapshot loadSnapshot(
            Minecraft minecraft,
            RegistryAccess.Frozen registryAccess,
            FeatureFlagSet enabledFeatures,
            boolean includeSelectedClientPacks
    ) {
        try (CloseableResourceManager resourceManager =
                     RecipePackResourceFactory.createLocal(minecraft, includeSelectedClientPacks)) {
            FallbackRecipeManager recipeManager = new FallbackRecipeManager(registryAccess);
            recipeManager.load(resourceManager, enabledFeatures);
            return RecipeSnapshot.capture(recipeManager, registryAccess);
        }
    }

    private record RecipeSnapshot(Map<Identifier, RecipeDefinition> recipes, Map<String, Integer> signatureCounts) {
        private static RecipeSnapshot capture(FallbackRecipeManager recipeManager, RegistryAccess.Frozen registryAccess) {
            RegistryOps<JsonElement> serializationContext = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
            Map<Identifier, RecipeDefinition> recipes = new LinkedHashMap<>();
            Map<String, Integer> signatureCounts = new HashMap<>();

            List<RecipeHolder<?>> sortedRecipes = new ArrayList<>(recipeManager.getRecipes());
            sortedRecipes.sort(Comparator.comparing(recipe -> recipe.id().identifier().toString()));

            for (RecipeHolder<?> recipe : sortedRecipes) {
                List<RecipeDisplayEntry> displays = new ArrayList<>();
                recipeManager.listDisplaysForRecipe(recipe.id(), displays::add);

                List<String> displaySignatures = displays.stream()
                        .map(display -> signature(display, serializationContext))
                        .sorted()
                        .toList();
                for (String displaySignature : displaySignatures) {
                    signatureCounts.merge(displaySignature, 1, Integer::sum);
                }

                recipes.put(
                        recipe.id().identifier(),
                        new RecipeDefinition(recipe.id().identifier(), displaySignatures)
                );
            }

            return new RecipeSnapshot(Map.copyOf(recipes), Map.copyOf(signatureCounts));
        }
    }

    private record RecipeDefinition(
            Identifier recipeId,
            List<String> displaySignatures
    ) {
    }

    private record CacheKey(
            FeatureFlagSet enabledFeatures,
            List<String> selectedPackIds
    ) {
        private static CacheKey create(Minecraft minecraft, FeatureFlagSet enabledFeatures) {
            return new CacheKey(
                    enabledFeatures,
                    RecipePackResourceFactory.localCacheSignature(minecraft, true)
            );
        }
    }

    private static String signature(RecipeDisplayEntry displayEntry, RegistryOps<JsonElement> serializationContext) {
        Identifier categoryId = BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey(displayEntry.category());
        String categoryKey = categoryId != null ? categoryId.toString() : displayEntry.category().toString();
        JsonElement encodedDisplay = encodeDisplay(displayEntry.display(), serializationContext);
        return categoryKey + "|" + displayEntry.group().orElse(-1) + "|" + encodedDisplay;
    }

    private static JsonElement encodeDisplay(RecipeDisplay display, RegistryOps<JsonElement> serializationContext) {
        return RecipeDisplay.CODEC.encodeStart(serializationContext, display)
                .result()
                .orElseGet(() -> new JsonPrimitive(display.toString()));
    }
}

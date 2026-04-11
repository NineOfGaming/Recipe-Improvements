package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.recipe.ModifiedRecipeDisplayLoader;
import dev.nineofgaming.recipe_fallback.ui.ModifiedRecipeBookCategory;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(ClientRecipeBook.class)
abstract class ClientRecipeBookMixin {
    @Inject(method = "categorizeAndGroupRecipes", at = @At("RETURN"), cancellable = true)
    private static void recipe_fallback$ungroupRecipeCollections(
            Iterable<RecipeDisplayEntry> recipes,
            CallbackInfoReturnable<Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>>> callbackInfo
    ) {
        if (!RecipeFallbackConfig.shouldUngroupRecipes()) {
            return;
        }

        Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> groupedRecipes = callbackInfo.getReturnValue();
        Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> ungroupedRecipes = new LinkedHashMap<>();

        groupedRecipes.forEach((category, groups) -> {
            List<List<RecipeDisplayEntry>> flattenedGroups = new ArrayList<>();
            for (List<RecipeDisplayEntry> group : groups) {
                for (RecipeDisplayEntry entry : group) {
                    flattenedGroups.add(List.of(entry));
                }
            }

            ungroupedRecipes.put(category, flattenedGroups);
        });

        callbackInfo.setReturnValue(ungroupedRecipes);
    }

    @Inject(method = "rebuildCollections", at = @At("TAIL"))
    private void recipe_fallback$addModifiedRecipeCollections(CallbackInfo callbackInfo) {
        if (!RecipeFallbackConfig.shouldShowModifiedRecipeBookTab()) {
            ClientRecipeBookAccessor accessor = (ClientRecipeBookAccessor) this;
            Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab =
                    new LinkedHashMap<>(accessor.recipe_fallback$getCollectionsByTab());
            for (ModifiedRecipeBookCategory category : ModifiedRecipeBookCategory.values()) {
                collectionsByTab.remove(category);
            }
            accessor.recipe_fallback$setCollectionsByTab(Map.copyOf(collectionsByTab));
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener listener = minecraft.getConnection();
        if (listener == null) {
            return;
        }

        Map<String, Integer> vanillaSignatureBudget = new HashMap<>(ModifiedRecipeDisplayLoader.getOrLoad(
                minecraft,
                listener.registryAccess(),
                listener.enabledFeatures()
        ));

        ClientRecipeBookAccessor accessor = (ClientRecipeBookAccessor) this;
        Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab =
                new LinkedHashMap<>(accessor.recipe_fallback$getCollectionsByTab());
        List<RecipeCollection> allCollections = new ArrayList<>(accessor.recipe_fallback$getAllCollections());

        for (ModifiedRecipeBookCategory category : ModifiedRecipeBookCategory.values()) {
            List<RecipeCollection> filteredCollections = new ArrayList<>();
            for (RecipeBookCategory baseCategory : category.includedCategories()) {
                List<RecipeCollection> collections = collectionsByTab.getOrDefault(baseCategory, List.of());
                for (RecipeCollection collection : collections) {
                    List<RecipeDisplayEntry> filteredEntries = new ArrayList<>();
                    for (RecipeDisplayEntry entry : collection.getRecipes()) {
                        String signature = ModifiedRecipeDisplayLoader.signature(entry, listener.registryAccess());
                        int remainingVanillaMatches = vanillaSignatureBudget.getOrDefault(signature, 0);
                        if (remainingVanillaMatches > 0) {
                            vanillaSignatureBudget.put(signature, remainingVanillaMatches - 1);
                            continue;
                        }

                        filteredEntries.add(entry);
                    }
                    if (!filteredEntries.isEmpty()) {
                        filteredCollections.add(new RecipeCollection(List.copyOf(filteredEntries)));
                    }
                }
            }

            List<RecipeCollection> immutableFilteredCollections = List.copyOf(filteredCollections);
            collectionsByTab.put(category, immutableFilteredCollections);
            allCollections.addAll(immutableFilteredCollections);
        }

        accessor.recipe_fallback$setCollectionsByTab(Map.copyOf(collectionsByTab));
        accessor.recipe_fallback$setAllCollections(List.copyOf(allCollections));
    }
}

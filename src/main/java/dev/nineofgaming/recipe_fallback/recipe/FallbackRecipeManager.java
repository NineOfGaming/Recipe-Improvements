package dev.nineofgaming.recipe_fallback.recipe;

import net.minecraft.client.multiplayer.ClientRecipeContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FallbackRecipeManager extends RecipeManager {
    FallbackRecipeManager(HolderLookup.Provider registries) {
        super(registries);
    }

    void load(ResourceManager resourceManager, FeatureFlagSet enabledFeatures) {
        RecipeMap preparedRecipes = this.prepare(resourceManager, InactiveProfiler.INSTANCE);
        this.apply(preparedRecipes, resourceManager, InactiveProfiler.INSTANCE);
        this.finalizeRecipeLoading(enabledFeatures);
    }

    void load(RecipeMap syncedRecipes, FeatureFlagSet enabledFeatures) {
        this.apply(syncedRecipes, ResourceManager.Empty.INSTANCE, InactiveProfiler.INSTANCE);
        this.finalizeRecipeLoading(enabledFeatures);
    }

    FallbackRecipePayload createPayload() {
        ClientRecipeContainer container = new ClientRecipeContainer(
                this.getSynchronizedItemProperties(),
                this.getSynchronizedStonecutterRecipes()
        );

        List<RecipeHolder<?>> sortedRecipes = new ArrayList<>(this.getRecipes());
        sortedRecipes.sort(Comparator.comparing(recipe -> recipe.id().identifier().toString()));

        List<ClientboundRecipeBookAddPacket.Entry> recipeBookEntries = new ArrayList<>();
        Map<RecipeDisplayId, ResourceKey<Recipe<?>>> displayRecipeIds = new LinkedHashMap<>();
        for (RecipeHolder<?> recipe : sortedRecipes) {
            this.listDisplaysForRecipe(
                    recipe.id(),
                    display -> {
                        recipeBookEntries.add(new ClientboundRecipeBookAddPacket.Entry(display, false, false));
                        displayRecipeIds.put(display.id(), recipe.id());
                    }
            );
        }

        return new FallbackRecipePayload(container, RecipeMap.create(sortedRecipes), recipeBookEntries, displayRecipeIds);
    }
}

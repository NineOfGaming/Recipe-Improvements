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
    private FallbackRecipeManager(HolderLookup.Provider registries) {
        super(registries);
    }

    static FallbackRecipeManager load(
            HolderLookup.Provider registries,
            ResourceManager resourceManager,
            FeatureFlagSet enabledFeatures
    ) {
        //? if >=26.3 {
        FallbackRecipeManager recipeManager = new FallbackRecipeManager(
                RecipeMapFactory.loadRegistry(resourceManager, registries)
        );
        //?} else {
        /*FallbackRecipeManager recipeManager = new FallbackRecipeManager(registries);
        RecipeMap preparedRecipes = recipeManager.prepare(resourceManager, InactiveProfiler.INSTANCE);
        recipeManager.apply(preparedRecipes, resourceManager, InactiveProfiler.INSTANCE);
        *///?}
        recipeManager.finalizeRecipeLoading(enabledFeatures);
        return recipeManager;
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

        return new FallbackRecipePayload(
                container,
                RecipeMapFactory.create(sortedRecipes),
                recipeBookEntries,
                displayRecipeIds
        );
    }
}

package dev.nineofgaming.recipe_fallback.recipe;

import net.minecraft.client.multiplayer.ClientRecipeContainer;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FallbackRecipePayload(
        ClientRecipeContainer container,
        RecipeMap syncedRecipes,
        List<ClientboundRecipeBookAddPacket.Entry> recipeBookEntries,
        Map<RecipeDisplayId, ResourceKey<Recipe<?>>> displayRecipeIds
) {
    public FallbackRecipePayload {
        recipeBookEntries = List.copyOf(recipeBookEntries);
        displayRecipeIds = Map.copyOf(displayRecipeIds);
    }

    public List<RecipeDisplayEntry> displays() {
        return this.recipeBookEntries.stream()
                .map(ClientboundRecipeBookAddPacket.Entry::contents)
                .toList();
    }

    public Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> recipesForDisplayIds(Collection<RecipeDisplayId> displayIds) {
        Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> recipes = new LinkedHashMap<>();
        for (RecipeDisplayId displayId : displayIds) {
            ResourceKey<Recipe<?>> recipeId = this.displayRecipeIds.get(displayId);
            if (recipeId == null || recipes.containsKey(recipeId)) {
                continue;
            }

            RecipeHolder<?> recipeHolder = this.syncedRecipes.byKey(recipeId);
            if (recipeHolder != null) {
                recipes.put(recipeId, recipeHolder);
            }
        }

        return Map.copyOf(recipes);
    }
}

package dev.nineofgaming.recipe_fallback.ui;

import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;

public final class RecipeBookTabTooltipHelper {
    private RecipeBookTabTooltipHelper() {
    }

    public static Component tooltip(ExtendedRecipeBookCategory category) {
        if (category instanceof SearchRecipeBookCategory searchCategory) {
            return switch (searchCategory) {
                case CRAFTING -> Component.translatable("recipe_fallback.tooltip.recipe_book_tab.all_crafting");
                case FURNACE -> Component.translatable("recipe_fallback.tooltip.recipe_book_tab.all_furnace");
                case BLAST_FURNACE -> Component.translatable("recipe_fallback.tooltip.recipe_book_tab.all_blast_furnace");
                case SMOKER -> Component.translatable("recipe_fallback.tooltip.recipe_book_tab.all_smoker");
            };
        }

        if (category instanceof ModifiedRecipeBookCategory modifiedCategory) {
            return switch (modifiedCategory) {
                case CRAFTING -> Component.translatable("recipe_fallback.tooltip.recipe_book_tab.modified_crafting");
                case FURNACE -> Component.translatable("recipe_fallback.tooltip.recipe_book_tab.modified_furnace");
                case BLAST_FURNACE -> Component.translatable("recipe_fallback.tooltip.recipe_book_tab.modified_blast_furnace");
                case SMOKER -> Component.translatable("recipe_fallback.tooltip.recipe_book_tab.modified_smoker");
            };
        }

        if (category == RecipeBookCategories.CRAFTING_BUILDING_BLOCKS) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.crafting_building_blocks");
        }

        if (category == RecipeBookCategories.CRAFTING_REDSTONE) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.crafting_redstone");
        }

        if (category == RecipeBookCategories.CRAFTING_EQUIPMENT) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.crafting_equipment");
        }

        if (category == RecipeBookCategories.CRAFTING_MISC) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.crafting_misc");
        }

        if (category == RecipeBookCategories.FURNACE_FOOD) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.furnace_food");
        }

        if (category == RecipeBookCategories.FURNACE_BLOCKS) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.furnace_blocks");
        }

        if (category == RecipeBookCategories.FURNACE_MISC) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.furnace_misc");
        }

        if (category == RecipeBookCategories.BLAST_FURNACE_BLOCKS) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.blast_furnace_blocks");
        }

        if (category == RecipeBookCategories.BLAST_FURNACE_MISC) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.blast_furnace_misc");
        }

        if (category == RecipeBookCategories.SMOKER_FOOD) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.smoker_food");
        }

        if (category == RecipeBookCategories.STONECUTTER) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.stonecutter");
        }

        if (category == RecipeBookCategories.SMITHING) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.smithing");
        }

        if (category == RecipeBookCategories.CAMPFIRE) {
            return Component.translatable("recipe_fallback.tooltip.recipe_book_tab.campfire");
        }

        return null;
    }
}

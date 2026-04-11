package dev.nineofgaming.recipe_fallback.ui;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;

import java.util.List;
import java.util.Optional;

public enum ModifiedRecipeBookCategory implements ExtendedRecipeBookCategory {
    CRAFTING(SearchRecipeBookCategory.CRAFTING, Items.KNOWLEDGE_BOOK, Items.CRAFTING_TABLE),
    FURNACE(SearchRecipeBookCategory.FURNACE, Items.KNOWLEDGE_BOOK, Items.FURNACE),
    BLAST_FURNACE(SearchRecipeBookCategory.BLAST_FURNACE, Items.KNOWLEDGE_BOOK, Items.BLAST_FURNACE),
    SMOKER(SearchRecipeBookCategory.SMOKER, Items.KNOWLEDGE_BOOK, Items.SMOKER);

    private final SearchRecipeBookCategory searchCategory;
    private final Item primaryIcon;
    private final Item secondaryIcon;

    ModifiedRecipeBookCategory(SearchRecipeBookCategory searchCategory, Item primaryIcon, Item secondaryIcon) {
        this.searchCategory = searchCategory;
        this.primaryIcon = primaryIcon;
        this.secondaryIcon = secondaryIcon;
    }

    public List<RecipeBookCategory> includedCategories() {
        return this.searchCategory.includedCategories();
    }

    public RecipeBookComponent.TabInfo createTabInfo() {
        return new RecipeBookComponent.TabInfo(
                new ItemStack(this.primaryIcon),
                Optional.of(new ItemStack(this.secondaryIcon)),
                this
        );
    }

    public static Optional<ModifiedRecipeBookCategory> fromTabInfos(List<RecipeBookComponent.TabInfo> tabInfos) {
        for (RecipeBookComponent.TabInfo tabInfo : tabInfos) {
            if (!(tabInfo.category() instanceof SearchRecipeBookCategory searchCategory)) {
                continue;
            }

            return fromSearchCategory(searchCategory);
        }

        return Optional.empty();
    }

    private static Optional<ModifiedRecipeBookCategory> fromSearchCategory(SearchRecipeBookCategory searchCategory) {
        return switch (searchCategory) {
            case CRAFTING -> Optional.of(CRAFTING);
            case FURNACE -> Optional.of(FURNACE);
            case BLAST_FURNACE -> Optional.of(BLAST_FURNACE);
            case SMOKER -> Optional.of(SMOKER);
        };
    }
}

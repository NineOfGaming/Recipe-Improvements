package dev.nineofgaming.recipe_fallback.mixins;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ClientRecipeBook.class)
public interface ClientRecipeBookAccessor {
    @Accessor("known")
    Map<RecipeDisplayId, RecipeDisplayEntry> recipe_fallback$getKnown();

    @Accessor("collectionsByTab")
    Map<ExtendedRecipeBookCategory, List<RecipeCollection>> recipe_fallback$getCollectionsByTab();

    @Accessor("collectionsByTab")
    void recipe_fallback$setCollectionsByTab(Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab);

    @Accessor("allCollections")
    List<RecipeCollection> recipe_fallback$getAllCollections();

    @Accessor("allCollections")
    void recipe_fallback$setAllCollections(List<RecipeCollection> allCollections);
}

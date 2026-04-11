package dev.nineofgaming.recipe_fallback.mixins;

import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent$OverlayRecipeButton")
interface OverlayRecipeButtonAccessor {
    @Accessor("recipe")
    RecipeDisplayId recipe_fallback$getRecipe();
}

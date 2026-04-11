package dev.nineofgaming.recipe_fallback.mixins;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
    @Invoker("getXOrigin")
    int recipe_fallback$getXOrigin();

    @Invoker("getYOrigin")
    int recipe_fallback$getYOrigin();
}

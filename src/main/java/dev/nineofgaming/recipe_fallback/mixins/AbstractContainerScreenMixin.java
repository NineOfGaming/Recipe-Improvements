package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.ui.RecipeBookCloseHandler;
import dev.nineofgaming.recipe_fallback.ui.RecipeBookScrollHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin {
    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void recipe_fallback$scrollRecipeBookPages(
            double mouseX,
            double mouseY,
            double horizontalScroll,
            double verticalScroll,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!RecipeFallbackConfig.shouldEnableRecipeBookMouseWheelScroll() || verticalScroll == 0.0D) {
            return;
        }

        if (!((Object) this instanceof AbstractRecipeBookScreen recipeBookScreen)) {
            return;
        }

        RecipeBookComponent<?> recipeBookComponent =
                ((AbstractRecipeBookScreenAccessor) recipeBookScreen).recipe_fallback$getRecipeBookComponent();
        if (recipeBookComponent == null) {
            return;
        }

        if (((RecipeBookScrollHandler) recipeBookComponent).recipe_fallback$mouseScrolled(mouseX, mouseY, verticalScroll)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void recipe_fallback$closeRecipeBookWhenScreenCloses(CallbackInfo callbackInfo) {
        if (!RecipeFallbackConfig.shouldCloseRecipeBookOnScreenClose()) {
            return;
        }

        if (!((Object) this instanceof AbstractRecipeBookScreen recipeBookScreen)) {
            return;
        }

        RecipeBookComponent<?> recipeBookComponent =
                ((AbstractRecipeBookScreenAccessor) recipeBookScreen).recipe_fallback$getRecipeBookComponent();
        if (recipeBookComponent == null) {
            return;
        }

        ((RecipeBookCloseHandler) recipeBookComponent).recipe_fallback$closeForScreenClose();
    }
}

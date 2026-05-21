package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.ui.RecipeBookTooltipHelper;
import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RecipeButton.class)
abstract class RecipeButtonMixin {
    @Shadow
    public abstract RecipeDisplayId getCurrentRecipe();

    @Shadow
    private float animationTime;

    @Shadow
    private List<?> selectedEntries;

    @Inject(method = "getTooltipText", at = @At("RETURN"), cancellable = true)
    private void recipe_fallback$appendFallbackTooltip(
            ItemStack stack,
            CallbackInfoReturnable<List<Component>> callbackInfo
    ) {
        callbackInfo.setReturnValue(RecipeBookTooltipHelper.appendRecipeDetails(
                Minecraft.getInstance(),
                this.getCurrentRecipe(),
                callbackInfo.getReturnValue()
        ));
    }

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void recipe_fallback$protectAgainstEmptyRecipeEntries(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        if (this.selectedEntries.isEmpty()) {
            callbackInfo.cancel();
            return;
        }

        if (RecipeFallbackConfig.shouldDisableRecipeBookAnimations()) {
            this.animationTime = 0.0F;
        }
    }
}

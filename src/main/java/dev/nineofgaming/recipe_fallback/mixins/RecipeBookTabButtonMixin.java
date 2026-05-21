package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.ui.RecipeBookTabTooltipHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookTabButton.class)
abstract class RecipeBookTabButtonMixin {
    @Shadow
    private float animationTime;

    @Shadow
    public abstract ExtendedRecipeBookCategory getCategory();

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void recipe_fallback$disableRecipeBookTabAnimation(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        if (RecipeFallbackConfig.shouldDisableRecipeBookAnimations()) {
            this.animationTime = 0.0F;
        }
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void recipe_fallback$showRecipeBookTabTooltip(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        if (!RecipeFallbackConfig.shouldShowRecipeBookTabTooltips()) {
            return;
        }

        if (!((AbstractWidget) (Object) this).isHoveredOrFocused()) {
            return;
        }

        Component tooltip = RecipeBookTabTooltipHelper.tooltip(this.getCategory());
        if (tooltip == null) {
            return;
        }

        guiGraphics.setTooltipForNextFrame(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
    }
}

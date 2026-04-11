package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.ui.RecipeBookTooltipHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(OverlayRecipeComponent.class)
abstract class OverlayRecipeComponentMixin {
    @Shadow
    private boolean isVisible;

    @Final
    @Shadow
    private List<?> recipeButtons;

    @Inject(method = "render", at = @At("RETURN"))
    private void recipe_fallback$renderFallbackTooltip(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        if (!this.isVisible) {
            return;
        }

        for (Object recipeButton : this.recipeButtons) {
            if (!(recipeButton instanceof AbstractWidget widget) || !widget.isHoveredOrFocused()) {
                continue;
            }

            RecipeDisplayId recipeId = ((OverlayRecipeButtonAccessor) recipeButton).recipe_fallback$getRecipe();
            List<Component> tooltip = RecipeBookTooltipHelper.buildRecipeTooltip(Minecraft.getInstance(), recipeId);
            if (tooltip.isEmpty()) {
                continue;
            }

            guiGraphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
            return;
        }
    }
}

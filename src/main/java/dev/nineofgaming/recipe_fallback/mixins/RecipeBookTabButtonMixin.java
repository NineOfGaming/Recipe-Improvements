package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.ui.ModifiedRecipeBookCategory;
import dev.nineofgaming.recipe_fallback.ui.RecipeBookTabTooltipHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(RecipeBookTabButton.class)
abstract class RecipeBookTabButtonMixin {
    @Unique
    private static final boolean recipe_fallback$rbipLoaded = FabricLoader.getInstance().isModLoaded("rbip");

    @Unique
    private static final Method recipe_fallback$rbipCreativeTabGetter =
            recipe_fallback$findRbipCreativeTabGetter();

    @Shadow
    private float animationTime;

    @Shadow
    public abstract ExtendedRecipeBookCategory getCategory();

    @Inject(method = "renderContents", at = @At("HEAD"))
    private void recipe_fallback$disableRecipeBookTabAnimation(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        if (RecipeFallbackConfig.shouldDisableRecipeBookAnimations()) {
            this.animationTime = 0.0F;
        }
    }

    @Inject(method = "renderContents", at = @At("TAIL"))
    private void recipe_fallback$showRecipeBookTabTooltip(
            GuiGraphics guiGraphics,
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

        ExtendedRecipeBookCategory category = this.getCategory();
        if (recipe_fallback$usesRbipTooltip(category)) {
            return;
        }

        Component tooltip = RecipeBookTabTooltipHelper.tooltip(category);
        if (tooltip == null) {
            return;
        }

        guiGraphics.setTooltipForNextFrame(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
    }

    @Unique
    private static Method recipe_fallback$findRbipCreativeTabGetter() {
        if (!recipe_fallback$rbipLoaded) {
            return null;
        }

        try {
            return Class.forName("dev.zenfyr.rbip.access.RecipeBookTabButtonDuck")
                    .getMethod("rbip$getCreativeTab");
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    @Unique
    private boolean recipe_fallback$usesRbipTooltip(ExtendedRecipeBookCategory category) {
        if (!recipe_fallback$rbipLoaded || category instanceof ModifiedRecipeBookCategory) {
            return false;
        }

        if (category == SearchRecipeBookCategory.CRAFTING) {
            return true;
        }

        if (recipe_fallback$rbipCreativeTabGetter == null) {
            return false;
        }

        try {
            return recipe_fallback$rbipCreativeTabGetter.invoke(this) != null;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}

package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.ui.RecipeBookScrollHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RecipeBookPage.class)
abstract class RecipeBookPageMixin implements RecipeBookScrollHandler {
    @Final
    @Shadow
    private OverlayRecipeComponent overlay;

    @Shadow
    private Minecraft minecraft;

    @Shadow
    private int totalPages;

    @Shadow
    private int currentPage;

    @Shadow
    private void updateButtonsForPage() {
    }

    @Override
    public boolean recipe_fallback$mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (scrollY == 0.0D || this.totalPages <= 1 || this.overlay.isVisible()) {
            return false;
        }

        if (scrollY < 0.0D) {
            if (this.currentPage >= this.totalPages - 1) {
                return false;
            }

            this.currentPage++;
        } else {
            if (this.currentPage <= 0) {
                return false;
            }

            this.currentPage--;
        }

        this.updateButtonsForPage();
        if (this.minecraft != null) {
            AbstractWidget.playButtonClickSound(this.minecraft.getSoundManager());
        }

        return true;
    }
}

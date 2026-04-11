package dev.nineofgaming.recipe_fallback.mixins;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    void recipe_fallback$setLeftPos(int leftPos);

    @Accessor("imageWidth")
    int recipe_fallback$getImageWidth();
}

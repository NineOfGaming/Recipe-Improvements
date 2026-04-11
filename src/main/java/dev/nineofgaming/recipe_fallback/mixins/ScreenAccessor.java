package dev.nineofgaming.recipe_fallback.mixins;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {
    @Accessor("width")
    int recipe_fallback$getWidth();

    @Invoker("addWidget")
    <T extends GuiEventListener & NarratableEntry> T recipe_fallback$addWidget(T widget);
}

package dev.nineofgaming.recipe_fallback.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ConfigTextureButton extends Button {
    private static final int ICON_SIZE = 16;
    private static final int TEXTURE_SIZE = 16;
    private static final int IDLE_BACKGROUND_COLOR = 0x30000000;
    private static final int HOVER_BACKGROUND_COLOR = 0x60FFFFFF;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "recipe_fallback",
            "textures/gui/configure_button.png"
    );

    public ConfigTextureButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(
                this.getX(),
                this.getY(),
                this.getRight(),
                this.getBottom(),
                this.isHoveredOrFocused() ? HOVER_BACKGROUND_COLOR : IDLE_BACKGROUND_COLOR
        );

        int iconX = this.getX() + (this.getWidth() - ICON_SIZE) / 2;
        int iconY = this.getY() + (this.getHeight() - ICON_SIZE) / 2;
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                iconX,
                iconY,
                0.0F,
                0.0F,
                ICON_SIZE,
                ICON_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );

        if (this.isHoveredOrFocused()) {
            guiGraphics.outline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0xFFFFFFFF);
        }
    }
}

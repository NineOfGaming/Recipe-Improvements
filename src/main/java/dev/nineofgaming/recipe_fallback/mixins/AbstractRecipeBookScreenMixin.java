package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.ui.ConfigTextureButton;
import dev.nineofgaming.recipe_fallback.ui.RecipeBookAutoCloseHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
abstract class AbstractRecipeBookScreenMixin implements RecipeBookAutoCloseHost {
    @Unique
    private static final int recipe_fallback$configButtonSize = 18;

    @Unique
    private static final int recipe_fallback$configButtonLeftMargin = 11;

    @Unique
    private static final int recipe_fallback$configButtonBottomMargin = 12;

    @Final
    @Shadow
    private RecipeBookComponent<?> recipeBookComponent;

    @Shadow
    protected abstract ScreenPosition getRecipeBookButtonPosition();

    @Shadow
    protected abstract void onRecipeBookButtonClick();

    @Unique
    private Button recipe_fallback$recipeBookButton;

    @Unique
    private ConfigTextureButton recipe_fallback$configButton;

    @Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
    private void recipe_fallback$hideVanillaRecipeBookButton(CallbackInfo callbackInfo) {
        if (RecipeFallbackConfig.shouldHideVanillaRecipeBook()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "initButton", at = @At("TAIL"))
    private void recipe_fallback$captureRecipeBookButton(CallbackInfo callbackInfo) {
        ScreenPosition position = this.getRecipeBookButtonPosition();
        this.recipe_fallback$recipeBookButton = null;

        for (GuiEventListener child : ((AbstractRecipeBookScreen<?>) (Object) this).children()) {
            if (child instanceof Button button
                    && button.getX() == position.x()
                    && button.getY() == position.y()
                    && button.getWidth() == 20
                    && button.getHeight() == 18) {
                this.recipe_fallback$recipeBookButton = button;
                break;
            }
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void recipe_fallback$initConfigButton(CallbackInfo callbackInfo) {
        AbstractRecipeBookScreen<?> screen = (AbstractRecipeBookScreen<?>) (Object) this;
        Component tooltip = Component.translatable("recipe_fallback.config.open_button.tooltip");
        this.recipe_fallback$configButton = ((ScreenAccessor) this).recipe_fallback$addWidget(
                new ConfigTextureButton(
                                0,
                                0,
                                recipe_fallback$configButtonSize,
                                recipe_fallback$configButtonSize,
                                tooltip,
                                button -> Minecraft.getInstance().setScreen(RecipeFallbackConfig.createScreen(screen))
                        )
        );
        this.recipe_fallback$configButton.setTooltip(Tooltip.create(tooltip));

        this.recipe_fallback$updateConfigButtonState();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void recipe_fallback$updateConfigButtonBeforeRender(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        this.recipe_fallback$updateConfigButtonState();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void recipe_fallback$renderConfigButtonOnTop(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        if (this.recipe_fallback$configButton == null || !this.recipe_fallback$configButton.visible) {
            return;
        }

        guiGraphics.nextStratum();
        this.recipe_fallback$configButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        if (this.recipe_fallback$configButton.isHoveredOrFocused()) {
            guiGraphics.setTooltipForNextFrame(
                    Minecraft.getInstance().font,
                    Component.translatable("recipe_fallback.config.open_button.tooltip"),
                    mouseX,
                    mouseY
            );
        }
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
    private void recipe_fallback$clickConfigButtonBeforeRecipeBook(
            MouseButtonEvent mouseButtonEvent,
            boolean ignoreTextInputGuard,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        this.recipe_fallback$updateConfigButtonState();
        if (this.recipe_fallback$configButton == null || !this.recipe_fallback$configButton.visible) {
            return;
        }

        if (this.recipe_fallback$configButton.mouseClicked(mouseButtonEvent, ignoreTextInputGuard)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Override
    public void recipe_fallback$autoCloseRecipeBook() {
        if (!this.recipeBookComponent.isVisible()) {
            return;
        }

        AbstractContainerScreenAccessor containerScreen = (AbstractContainerScreenAccessor) (Object) this;
        ScreenAccessor screen = (ScreenAccessor) (Object) this;

        this.recipeBookComponent.toggleVisibility();
        containerScreen.recipe_fallback$setLeftPos(
                this.recipeBookComponent.updateScreenPosition(
                        screen.recipe_fallback$getWidth(),
                        containerScreen.recipe_fallback$getImageWidth()
                )
        );

        if (this.recipe_fallback$recipeBookButton != null) {
            ScreenPosition position = this.getRecipeBookButtonPosition();
            this.recipe_fallback$recipeBookButton.setPosition(position.x(), position.y());
        }

        this.onRecipeBookButtonClick();
    }

    @Unique
    private void recipe_fallback$updateConfigButtonState() {
        if (this.recipe_fallback$configButton == null) {
            return;
        }

        boolean visible = this.recipeBookComponent.isVisible()
                && RecipeFallbackConfig.shouldShowRecipeBookConfigButton();
        this.recipe_fallback$configButton.visible = visible;
        this.recipe_fallback$configButton.active = visible;

        if (!visible) {
            return;
        }

        RecipeBookComponentAccessor recipeBookComponentAccessor =
                (RecipeBookComponentAccessor) this.recipeBookComponent;
        this.recipe_fallback$configButton.setPosition(
                recipeBookComponentAccessor.recipe_fallback$getXOrigin() + recipe_fallback$configButtonLeftMargin,
                recipeBookComponentAccessor.recipe_fallback$getYOrigin()
                        + RecipeBookComponent.IMAGE_HEIGHT
                        - recipe_fallback$configButtonSize
                        - recipe_fallback$configButtonBottomMargin
        );
    }
}

package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.AlwaysVisibleRecipeSync;
import dev.nineofgaming.recipe_fallback.FallbackFeedback;
import dev.nineofgaming.recipe_fallback.compat.JustEnoughItemsBridge;
import dev.nineofgaming.recipe_fallback.compat.RoughlyEnoughItemsBridge;
import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.recipe.FallbackRecipePayload;
import dev.nineofgaming.recipe_fallback.recipe.FallbackRecipeLoader;
import dev.nineofgaming.recipe_fallback.state.FallbackState;
import dev.nineofgaming.recipe_fallback.state.FallbackDisplayState;
import dev.nineofgaming.recipe_fallback.ui.RecipeBookTooltipHelper;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientRecipeContainer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Unique
    private static final Field recipe_fallback$recipeContainerField =
            recipe_fallback$findRecipeContainerField();

    @Inject(method = "handleUpdateRecipes", at = @At("TAIL"))
    private void recipe_fallback$applyContainerFallback(
            ClientboundUpdateRecipesPacket packet,
            CallbackInfo callbackInfo
    ) {
        if (!packet.itemSets().isEmpty()) {
            this.recipe_fallback$clearEmptySyncFallback();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (RecipeFallbackConfig.shouldApplyFallbackForCurrentServer(minecraft)) {
            ClientPacketListener listener = (ClientPacketListener) (Object) this;
            this.recipe_fallback$getFallbackPayload().ifPresent(payload -> {
                FallbackDisplayState.setPayload(payload);
                recipe_fallback$setRecipeContainer(listener, payload.container());
                if (FallbackState.activate()) {
                    FallbackFeedback.onFallbackActivated(minecraft, payload.recipeBookEntries().size());
                }
            });
            return;
        }

        FallbackFeedback.verbose("Skipped empty recipe sync fallback because it is disabled for this server");
    }

    @Inject(method = "handleRecipeBookAdd", at = @At("TAIL"))
    private void recipe_fallback$applyRecipeBookFallback(
            ClientboundRecipeBookAddPacket packet,
            CallbackInfo callbackInfo
    ) {
        if (packet.replace()) {
            if (!packet.entries().isEmpty()) {
                FallbackState.deactivate();
            }
            FallbackDisplayState.clear();
            RecipeBookTooltipHelper.clear();
        } else {
            FallbackDisplayState.unmarkDisplayIds(packet.entries().stream()
                    .map(entry -> entry.contents().id())
                    .toList());
        }

        ClientPacketListener listener = (ClientPacketListener) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || listener.getLevel() == null) {
            return;
        }

        if (packet.replace() && packet.entries().isEmpty()) {
            if (RecipeFallbackConfig.shouldApplyFallbackForCurrentServer(minecraft)) {
                this.recipe_fallback$getFallbackPayload().ifPresent(payload -> {
                    boolean newlyActivated = FallbackState.activate();
                    FallbackDisplayState.setPayload(payload);
                    ClientRecipeBook recipeBook = player.getRecipeBook();
                    payload.recipeBookEntries().forEach(entry -> recipeBook.add(entry.contents()));
                    recipe_fallback$refreshRecipeBook(listener, recipeBook, minecraft);
                    if (newlyActivated) {
                        FallbackFeedback.onFallbackActivated(minecraft, payload.recipeBookEntries().size());
                    }

                    RoughlyEnoughItemsBridge.clearAlwaysVisibleFallback();
                    JustEnoughItemsBridge.clearAlwaysVisibleFallback();
                    if (RecipeFallbackConfig.get().reiBridgeEnabled) {
                        RoughlyEnoughItemsBridge.applyFallback(payload);
                    }
                    if (RecipeFallbackConfig.get().jeiBridgeEnabled) {
                        JustEnoughItemsBridge.applyFallback(payload);
                    }
                });
                return;
            }

            FallbackFeedback.verbose("Skipped empty recipe book fallback because it is disabled for this server");
            return;
        }

        AlwaysVisibleRecipeSync.apply(listener, player.getRecipeBook(), minecraft);
    }

    @Inject(method = "handleRecipeBookRemove", at = @At("TAIL"))
    private void recipe_fallback$restoreAlwaysVisibleFallbackRecipes(
            ClientboundRecipeBookRemovePacket packet,
            CallbackInfo callbackInfo
    ) {
        FallbackDisplayState.unmarkDisplayIds(packet.recipes());

        ClientPacketListener listener = (ClientPacketListener) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || listener.getLevel() == null) {
            return;
        }

        AlwaysVisibleRecipeSync.apply(listener, player.getRecipeBook(), minecraft);
    }

    @Unique
    private void recipe_fallback$clearEmptySyncFallback() {
        ClientPacketListener listener = (ClientPacketListener) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        Set<RecipeDisplayId> fallbackDisplayIds = FallbackDisplayState.snapshot();
        boolean fallbackWasActive = FallbackState.isActive();
        boolean hadFallbackDisplays = !fallbackDisplayIds.isEmpty();

        FallbackState.deactivate();
        if (!fallbackWasActive && !hadFallbackDisplays) {
            return;
        }

        RecipeBookTooltipHelper.clear();
        if (player != null && listener.getLevel() != null && hadFallbackDisplays) {
            ClientRecipeBook recipeBook = player.getRecipeBook();
            fallbackDisplayIds.forEach(recipeBook::remove);
            recipe_fallback$refreshRecipeBook(listener, recipeBook, minecraft);
        }

        FallbackDisplayState.clear();
        if (player != null && listener.getLevel() != null) {
            AlwaysVisibleRecipeSync.apply(listener, player.getRecipeBook(), minecraft);
        }
    }

    @Unique
    private Optional<FallbackRecipePayload> recipe_fallback$getFallbackPayload() {
        ClientPacketListener listener = (ClientPacketListener) (Object) this;
        return FallbackRecipeLoader.getOrLoad(
                Minecraft.getInstance(),
                listener.registryAccess(),
                listener.enabledFeatures()
        );
    }

    @Unique
    private static void recipe_fallback$refreshRecipeBook(
            ClientPacketListener listener,
            ClientRecipeBook recipeBook,
            Minecraft minecraft
    ) {
        recipeBook.rebuildCollections();
        listener.searchTrees().updateRecipes(recipeBook, listener.getLevel());

        //? if >=26.2 {
        Screen screen = minecraft.gui.screen();
        //?} else {
        /*Screen screen = minecraft.screen;
        *///?}
        if (screen instanceof RecipeUpdateListener recipeUpdateListener) {
            recipeUpdateListener.recipesUpdated();
        }
    }

    @Unique
    private static void recipe_fallback$setRecipeContainer(
            ClientPacketListener listener,
            ClientRecipeContainer recipeContainer
    ) {
        try {
            recipe_fallback$recipeContainerField.set(listener, recipeContainer);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("Failed to replace ClientPacketListener recipe container", exception);
        }
    }

    @Unique
    private static Field recipe_fallback$findRecipeContainerField() {
        for (Field field : ClientPacketListener.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            if (field.getType() == ClientRecipeContainer.class) {
                field.setAccessible(true);
                return field;
            }
        }

        throw new IllegalStateException("Failed to locate ClientPacketListener recipe container field");
    }
}

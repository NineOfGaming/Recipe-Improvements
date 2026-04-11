package dev.nineofgaming.recipe_fallback;

import dev.nineofgaming.recipe_fallback.compat.JustEnoughItemsBridge;
import dev.nineofgaming.recipe_fallback.compat.RoughlyEnoughItemsBridge;
import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.recipe.FallbackRecipeLoader;
import dev.nineofgaming.recipe_fallback.recipe.ModifiedRecipeDisplayLoader;
import dev.nineofgaming.recipe_fallback.state.FallbackDisplayState;
import dev.nineofgaming.recipe_fallback.state.FallbackState;
import dev.nineofgaming.recipe_fallback.state.ServerKnownPacksState;
import dev.nineofgaming.recipe_fallback.ui.RecipeBookTooltipHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecipeFallback implements ClientModInitializer {
    @SuppressWarnings("unused")
    public static final String MOD_NAME = "Recipe QoL";
    public static final String MOD_ID = "recipe_fallback";
    @SuppressWarnings("unused")
    public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(() -> new IllegalStateException("Mod not loaded: " + MOD_ID)).getMetadata().getVersion().getFriendlyString();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {}", MOD_NAME);

        RecipeFallbackConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> RoughlyEnoughItemsBridge.clearFallbackIfServerSyncPresent());

        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            FallbackState.deactivate();
            FallbackDisplayState.clear();
            FallbackRecipeLoader.clear();
            ModifiedRecipeDisplayLoader.clear();
            RoughlyEnoughItemsBridge.reset();
            JustEnoughItemsBridge.reset();
            ServerKnownPacksState.clear();
            RecipeBookTooltipHelper.clear();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FallbackState.deactivate();
            FallbackDisplayState.clear();
            FallbackRecipeLoader.clear();
            ModifiedRecipeDisplayLoader.clear();
            RoughlyEnoughItemsBridge.reset();
            JustEnoughItemsBridge.reset();
            ServerKnownPacksState.clear();
            RecipeBookTooltipHelper.clear();
        });
    }
}

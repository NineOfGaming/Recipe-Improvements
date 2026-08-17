package dev.nineofgaming.recipe_fallback;

import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public final class FallbackFeedback {
    private static final Logger LOGGER = RecipeFallback.createLogger("Fallback");

    private FallbackFeedback() {
    }

    public static void onFallbackActivated(Minecraft minecraft, int recipeCount) {
        RecipeFallbackConfig.ConfigData config = RecipeFallbackConfig.get();
        Component message = Component.translatable(
                "recipe_fallback.message.fallback_activated",
                recipeCount
        );

        switch (config.notificationMode) {
            case OFF -> {
                verbose(message.getString());
            }
            case CHAT -> {
                if (minecraft.player != null) {
                    minecraft.player.sendSystemMessage(message);
                }
            }
            case TOAST -> SystemToast.add(
                    //? if >=26.2 {
                    minecraft.gui.toastManager(),
                    //?} else {
                    /*minecraft.getToastManager(),
                    *///?}
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.translatable("recipe_fallback.toast.title"),
                    message
            );
            case LOG_ONLY -> {
                LOGGER.info(message.getString());
            }
        }
    }

    public static void verbose(String message, Object... args) {
        if (RecipeFallbackConfig.get().verboseLogging) {
            LOGGER.info(message, args);
        }
    }
}

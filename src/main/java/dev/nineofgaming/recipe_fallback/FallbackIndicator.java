package dev.nineofgaming.recipe_fallback;

import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.state.FallbackDisplayState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.ArrayList;
import java.util.List;

public final class FallbackIndicator {
    private static final String tooltipTranslationKey = "recipe_fallback.tooltip.fallback_recipe";

    private FallbackIndicator() {
    }

    public static List<Component> appendTooltip(RecipeDisplayId displayId, List<Component> tooltip) {
        if (!shouldShow(displayId)) {
            return tooltip;
        }

        List<Component> updatedTooltip = new ArrayList<>(tooltip.size() + 1);
        updatedTooltip.addAll(tooltip);
        updatedTooltip.add(tooltipLine());
        return updatedTooltip;
    }

    public static List<Component> tooltip(RecipeDisplayId displayId) {
        return shouldShow(displayId) ? List.of(tooltipLine()) : List.of();
    }

    private static boolean shouldShow(RecipeDisplayId displayId) {
        return RecipeFallbackConfig.shouldShowFallbackIndicator()
                && FallbackDisplayState.isFallbackDisplay(displayId);
    }

    private static Component tooltipLine() {
        return Component.translatable(tooltipTranslationKey).withStyle(ChatFormatting.GRAY);
    }
}

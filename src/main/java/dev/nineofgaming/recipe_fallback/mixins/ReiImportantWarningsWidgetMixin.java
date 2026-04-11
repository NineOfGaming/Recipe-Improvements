package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.RecipeFallback;
import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.state.FallbackState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(targets = "me.shedaniel.rei.impl.client.gui.hints.ImportantWarningsWidget", remap = false)
abstract class ReiImportantWarningsWidgetMixin {
    @Unique
    private static Field recipe_fallback$visibleField;
    @Unique
    private static Field recipe_fallback$dirtyField;
    @Unique
    private static boolean recipe_fallback$fieldsResolved;
    @Unique
    private static boolean recipe_fallback$disabled;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void recipe_fallback$hideFallbackWarningOnInit(CallbackInfo callbackInfo) {
        if (recipe_fallback$shouldHideWarning()) {
            recipe_fallback$hideWarning(this);
        }
    }

    @Inject(method = "method_25394", at = @At("HEAD"), cancellable = true, remap = false)
    private void recipe_fallback$hideFallbackWarningOnRender(CallbackInfo callbackInfo) {
        if (recipe_fallback$shouldHideWarning() && recipe_fallback$hideWarning(this)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true, remap = false)
    private void recipe_fallback$hideFallbackWarningClick(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (recipe_fallback$shouldHideWarning() && recipe_fallback$hideWarning(this)) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Unique
    private static boolean recipe_fallback$shouldHideWarning() {
        return FallbackState.isActive() && RecipeFallbackConfig.get().hideReiWarning;
    }

    @Unique
    private static boolean recipe_fallback$hideWarning(Object instance) {
        if (!recipe_fallback$resolveFields()) {
            return false;
        }

        try {
            recipe_fallback$dirtyField.setBoolean(null, false);
            recipe_fallback$visibleField.setBoolean(instance, false);
            return true;
        } catch (IllegalAccessException exception) {
            recipe_fallback$disable("hide warning", exception);
            return false;
        }
    }

    @Unique
    private static boolean recipe_fallback$resolveFields() {
        if (recipe_fallback$disabled) {
            return false;
        }
        if (recipe_fallback$fieldsResolved) {
            return true;
        }

        try {
            Class<?> targetClass = Class.forName("me.shedaniel.rei.impl.client.gui.hints.ImportantWarningsWidget");
            recipe_fallback$visibleField = recipe_fallback$findField(targetClass, "visible");
            recipe_fallback$dirtyField = recipe_fallback$findField(targetClass, "dirty");
            recipe_fallback$fieldsResolved = true;
            return true;
        } catch (ReflectiveOperationException exception) {
            recipe_fallback$disable("resolve fields", exception);
            return false;
        }
    }

    @Unique
    private static Field recipe_fallback$findField(Class<?> targetClass, String name) throws NoSuchFieldException {
        Field field = targetClass.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Unique
    private static void recipe_fallback$disable(String action, ReflectiveOperationException exception) {
        if (recipe_fallback$disabled) {
            return;
        }

        recipe_fallback$disabled = true;
        RecipeFallback.LOGGER.warn("Disabled REI warning suppression after {} failure", action, exception);
    }
}

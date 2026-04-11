package dev.nineofgaming.recipe_fallback.mixins;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public final class RecipeFallbackMixinPlugin implements IMixinConfigPlugin {
    private static final String REI_WARNING_WIDGET = "me.shedaniel.rei.impl.client.gui.hints.ImportantWarningsWidget";
    private static final String REI_RENDER_METHOD = "method_25394";
    private static final String REI_CLICK_METHOD = "method_25402";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("ReiImportantWarningsWidgetMixin")) {
            return hasCompatibleReiWarningWidget();
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean hasCompatibleReiWarningWidget() {
        String resourcePath = REI_WARNING_WIDGET.replace('.', '/') + ".class";
        try (InputStream stream = RecipeFallbackMixinPlugin.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return false;
            }

            ClassNode warningWidgetClass = new ClassNode();
            new ClassReader(stream).accept(
                    warningWidgetClass,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES
            );
            return hasField(warningWidgetClass, "visible")
                    && hasField(warningWidgetClass, "dirty")
                    && hasMethod(warningWidgetClass, REI_RENDER_METHOD)
                    && hasMethod(warningWidgetClass, REI_CLICK_METHOD);
        } catch (IOException exception) {
            return false;
        }
    }

    private static boolean hasField(ClassNode owner, String name) {
        for (FieldNode field : owner.fields) {
            if (field.name.equals(name)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasMethod(ClassNode owner, String name) {
        for (MethodNode method : owner.methods) {
            if (method.name.equals(name)) {
                return true;
            }
        }

        return false;
    }
}

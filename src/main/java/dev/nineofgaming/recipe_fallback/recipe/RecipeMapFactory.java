package dev.nineofgaming.recipe_fallback.recipe;

//? if >=26.3 {
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.resources.ResourceManager;
//?}
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.Collection;
//? if >=26.3 {
import java.util.List;
//?}

public final class RecipeMapFactory {
    private RecipeMapFactory() {
    }

    public static RecipeMap create(Collection<RecipeHolder<?>> recipes) {
        //? if >=26.3 {
        MappedRegistry<Recipe<?>> registry = new MappedRegistry<>(Registries.RECIPE, Lifecycle.stable());
        for (RecipeHolder<?> recipe : recipes) {
            registry.register(recipe.id(), recipe.value(), RegistrationInfo.BUILT_IN);
        }
        return RecipeMap.create(registry.freeze());
        //?} else {
        /*return RecipeMap.create(recipes);
        *///?}
    }

    //? if >=26.3 {
    static HolderLookup.Provider loadRegistry(ResourceManager resourceManager, HolderLookup.Provider registries) {
        List<HolderLookup.RegistryLookup<?>> baseRegistries = registries.listRegistries()
                .filter(registry -> !Registries.RECIPE.equals(registry.key()))
                .toList();
        List<RegistryDataLoader.RegistryData<?>> recipeRegistry = RegistryDataLoader.RELOADABLE_REGISTRIES.stream()
                .filter(registry -> Registries.RECIPE.equals(registry.key()))
                .toList();

        RegistryAccess.Frozen loadedRegistries = RegistryDataLoader.load(
                resourceManager,
                baseRegistries,
                recipeRegistry,
                Runnable::run
        ).join();
        return loadedRegistries;
    }
    //?}
}

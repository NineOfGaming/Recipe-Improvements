package dev.nineofgaming.recipe_fallback;

import dev.nineofgaming.recipe_fallback.compat.JustEnoughItemsBridge;
import dev.nineofgaming.recipe_fallback.compat.RoughlyEnoughItemsBridge;
import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.mixins.ClientRecipeBookAccessor;
import dev.nineofgaming.recipe_fallback.recipe.FallbackRecipeLoader;
import dev.nineofgaming.recipe_fallback.recipe.FallbackRecipePayload;
import dev.nineofgaming.recipe_fallback.recipe.ModifiedRecipeDisplayLoader;
import dev.nineofgaming.recipe_fallback.state.FallbackDisplayState;
import dev.nineofgaming.recipe_fallback.state.FallbackState;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AlwaysVisibleRecipeSync {
    private AlwaysVisibleRecipeSync() {
    }

    public static void refresh(Minecraft minecraft) {
        if (minecraft == null || FallbackState.isActive()) {
            return;
        }

        ClientPacketListener listener = minecraft.getConnection();
        LocalPlayer player = minecraft.player;
        if (listener == null || player == null || listener.getLevel() == null) {
            return;
        }

        apply(listener, player.getRecipeBook(), minecraft);
    }

    public static void apply(
            ClientPacketListener listener,
            ClientRecipeBook recipeBook,
            Minecraft minecraft
    ) {
        if (FallbackState.isActive()) {
            return;
        }

        Map<RecipeDisplayId, RecipeDisplayEntry> knownEntries =
                ((ClientRecipeBookAccessor) recipeBook).recipe_fallback$getKnown();
        Set<RecipeDisplayId> fallbackDisplayIds = FallbackDisplayState.snapshot();
        Map<RecipeDisplayId, String> knownSignatures = getDisplaySignatures(knownEntries, listener);
        Optional<PayloadSelection> payloadSelection = getAlwaysVisiblePayload(
                listener,
                minecraft,
                knownEntries,
                fallbackDisplayIds,
                knownSignatures
        );
        if (payloadSelection.isEmpty()) {
            clear(listener, recipeBook, minecraft);
            return;
        }

        FallbackRecipePayload payload = payloadSelection.get().payload();
        Map<String, Integer> desiredFallbackSignatureCounts =
                payloadSelection.get().desiredFallbackSignatureCounts();

        Set<RecipeDisplayId> desiredFallbackDisplayIds = new LinkedHashSet<>();
        Set<RecipeDisplayId> staleFallbackDisplayIds = new LinkedHashSet<>();
        Map<String, Integer> visibleFallbackSignatureCounts = new java.util.HashMap<>();

        for (Map.Entry<RecipeDisplayId, RecipeDisplayEntry> knownEntry : knownEntries.entrySet()) {
            RecipeDisplayId displayId = knownEntry.getKey();
            if (!fallbackDisplayIds.contains(displayId)) {
                continue;
            }

            String signature = knownSignatures.get(displayId);
            int desiredCount = desiredFallbackSignatureCounts.getOrDefault(signature, 0);
            int visibleCount = visibleFallbackSignatureCounts.getOrDefault(signature, 0);
            if (signature == null || visibleCount >= desiredCount) {
                staleFallbackDisplayIds.add(displayId);
                continue;
            }

            desiredFallbackDisplayIds.add(displayId);
            visibleFallbackSignatureCounts.put(signature, visibleCount + 1);
        }

        List<ClientboundRecipeBookAddPacket.Entry> missingEntries = new ArrayList<>();
        for (ClientboundRecipeBookAddPacket.Entry entry : payload.recipeBookEntries()) {
            RecipeDisplayEntry display = entry.contents();
            RecipeDisplayId displayId = display.id();
            String signature = ModifiedRecipeDisplayLoader.signature(display, listener.registryAccess());
            int desiredCount = desiredFallbackSignatureCounts.getOrDefault(signature, 0);
            int visibleCount = visibleFallbackSignatureCounts.getOrDefault(signature, 0);
            if (visibleCount >= desiredCount) {
                continue;
            }

            if (knownEntries.containsKey(displayId)) {
                continue;
            }

            missingEntries.add(entry);
            desiredFallbackDisplayIds.add(displayId);
            visibleFallbackSignatureCounts.put(signature, visibleCount + 1);
        }

        boolean changed = false;
        if (!staleFallbackDisplayIds.isEmpty()) {
            staleFallbackDisplayIds.forEach(recipeBook::remove);
            changed = true;
        }

        FallbackDisplayState.setDisplayIds(desiredFallbackDisplayIds);

        if (!missingEntries.isEmpty()) {
            missingEntries.forEach(entry -> recipeBook.add(entry.contents()));
            changed = true;
        }

        syncRecipeViewers(payload, desiredFallbackDisplayIds);

        if (changed) {
            refreshRecipeBook(listener, recipeBook, minecraft);
        }
    }

    public static void clear(
            ClientPacketListener listener,
            ClientRecipeBook recipeBook,
            Minecraft minecraft
    ) {
        Set<RecipeDisplayId> fallbackDisplayIds = FallbackDisplayState.snapshot();
        boolean changed = false;
        if (!fallbackDisplayIds.isEmpty()) {
            fallbackDisplayIds.forEach(recipeBook::remove);
            changed = true;
        }

        FallbackDisplayState.clear();
        RoughlyEnoughItemsBridge.clearAlwaysVisibleFallback();
        JustEnoughItemsBridge.clearAlwaysVisibleFallback();

        if (changed) {
            refreshRecipeBook(listener, recipeBook, minecraft);
        }
    }

    private static Optional<PayloadSelection> getAlwaysVisiblePayload(
            ClientPacketListener listener,
            Minecraft minecraft,
            Map<RecipeDisplayId, RecipeDisplayEntry> knownEntries,
            Set<RecipeDisplayId> fallbackDisplayIds,
            Map<RecipeDisplayId, String> knownSignatures
    ) {
        if (RecipeFallbackConfig.shouldForceShowAllServerRecipes(minecraft)) {
            Optional<PayloadSelection> serverOnlyPayload = getPayloadSelection(
                    FallbackRecipeLoader.getOrLoadServerOnly(
                            minecraft,
                            listener.registryAccess(),
                            listener.enabledFeatures()
                    ),
                    knownEntries,
                    fallbackDisplayIds,
                    knownSignatures,
                    listener
            );
            if (serverOnlyPayload.isPresent()) {
                return serverOnlyPayload;
            }
        }

        if (RecipeFallbackConfig.shouldForceShowAllRecipes(minecraft)) {
            return getPayloadSelection(
                    FallbackRecipeLoader.getOrLoad(
                            minecraft,
                            listener.registryAccess(),
                            listener.enabledFeatures()
                    ),
                    knownEntries,
                    fallbackDisplayIds,
                    knownSignatures,
                    listener
            );
        }

        return Optional.empty();
    }

    private static Optional<PayloadSelection> getPayloadSelection(
            Optional<FallbackRecipePayload> payload,
            Map<RecipeDisplayId, RecipeDisplayEntry> knownEntries,
            Set<RecipeDisplayId> fallbackDisplayIds,
            Map<RecipeDisplayId, String> knownSignatures,
            ClientPacketListener listener
    ) {
        return payload.map(loadedPayload -> new PayloadSelection(
                        loadedPayload,
                        getDesiredFallbackSignatureCounts(
                                loadedPayload,
                                knownEntries,
                                fallbackDisplayIds,
                                knownSignatures,
                                listener
                        )
                ))
                .filter(selection -> !selection.desiredFallbackSignatureCounts().isEmpty());
    }

    private static void syncRecipeViewers(
            FallbackRecipePayload payload,
            Set<RecipeDisplayId> desiredFallbackDisplayIds
    ) {
        if (RecipeFallbackConfig.get().reiBridgeEnabled) {
            RoughlyEnoughItemsBridge.syncAlwaysVisibleFallback(payload, desiredFallbackDisplayIds);
        } else {
            RoughlyEnoughItemsBridge.clearAlwaysVisibleFallback();
        }

        if (RecipeFallbackConfig.get().jeiBridgeEnabled) {
            JustEnoughItemsBridge.syncAlwaysVisibleFallback(payload, desiredFallbackDisplayIds);
        } else {
            JustEnoughItemsBridge.clearAlwaysVisibleFallback();
        }
    }

    private record PayloadSelection(
            FallbackRecipePayload payload,
            Map<String, Integer> desiredFallbackSignatureCounts
    ) {
    }

    private static Map<RecipeDisplayId, String> getDisplaySignatures(
            Map<RecipeDisplayId, RecipeDisplayEntry> displayEntries,
            ClientPacketListener listener
    ) {
        return displayEntries.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> ModifiedRecipeDisplayLoader.signature(entry.getValue(), listener.registryAccess())
                ));
    }

    private static Map<String, Integer> getDesiredFallbackSignatureCounts(
            FallbackRecipePayload payload,
            Map<RecipeDisplayId, RecipeDisplayEntry> knownEntries,
            Set<RecipeDisplayId> fallbackDisplayIds,
            Map<RecipeDisplayId, String> knownSignatures,
            ClientPacketListener listener
    ) {
        Map<String, Integer> realSignatureCounts = new java.util.HashMap<>();
        for (Map.Entry<RecipeDisplayId, RecipeDisplayEntry> knownEntry : knownEntries.entrySet()) {
            if (fallbackDisplayIds.contains(knownEntry.getKey())) {
                continue;
            }

            String signature = knownSignatures.get(knownEntry.getKey());
            if (signature != null) {
                realSignatureCounts.merge(signature, 1, Integer::sum);
            }
        }

        Map<String, Integer> payloadSignatureCounts = new LinkedHashMap<>();
        for (ClientboundRecipeBookAddPacket.Entry entry : payload.recipeBookEntries()) {
            String signature = ModifiedRecipeDisplayLoader.signature(entry.contents(), listener.registryAccess());
            payloadSignatureCounts.merge(signature, 1, Integer::sum);
        }

        Map<String, Integer> desiredFallbackSignatureCounts = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> payloadSignatureCount : payloadSignatureCounts.entrySet()) {
            int desiredCount = payloadSignatureCount.getValue()
                    - realSignatureCounts.getOrDefault(payloadSignatureCount.getKey(), 0);
            if (desiredCount > 0) {
                desiredFallbackSignatureCounts.put(payloadSignatureCount.getKey(), desiredCount);
            }
        }

        return desiredFallbackSignatureCounts;
    }

    private static void refreshRecipeBook(
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
}

package dev.nineofgaming.recipe_fallback.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.KnownPacksManager;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import dev.nineofgaming.recipe_fallback.state.ServerKnownPacksState;

import java.util.ArrayList;
import java.util.List;

final class RecipePackResourceFactory {
    private RecipePackResourceFactory() {
    }

    static CloseableResourceManager createFallback(Minecraft minecraft, boolean includeSelectedClientPacks) {
        CloseableResourceManager knownPackResourceManager =
                createKnownPackResourceManager(includeSelectedClientPacks);
        return knownPackResourceManager != null
                ? knownPackResourceManager
                : createLocal(minecraft, includeSelectedClientPacks);
    }

    static CloseableResourceManager createServerKnownOnly(boolean includeSelectedClientPacks) {
        return createKnownPackResourceManager(includeSelectedClientPacks);
    }

    static CloseableResourceManager createLocal(Minecraft minecraft, boolean includeSelectedClientPacks) {
        List<PackResources> resources = createLocalPackResources(minecraft, includeSelectedClientPacks);
        try {
            return new MultiPackResourceManager(PackType.SERVER_DATA, resources);
        } catch (RuntimeException exception) {
            for (PackResources resource : resources) {
                resource.close();
            }
            throw exception;
        }
    }

    static List<String> fallbackCacheSignature(Minecraft minecraft, boolean includeSelectedClientPacks) {
        List<KnownPack> knownPacks = filteredKnownPacks(includeSelectedClientPacks);
        if (!knownPacks.isEmpty()) {
            return knownPackCacheSignature(knownPacks);
        }

        return localCacheSignature(minecraft, includeSelectedClientPacks);
    }

    static List<String> serverKnownCacheSignature(boolean includeSelectedClientPacks) {
        return knownPackCacheSignature(filteredKnownPacks(includeSelectedClientPacks));
    }

    static String fallbackDescription(Minecraft minecraft, boolean includeSelectedClientPacks) {
        return filteredKnownPacks(includeSelectedClientPacks).isEmpty()
                ? (includeSelectedClientPacks ? "selected client packs" : "vanilla resources")
                : serverKnownDescription(includeSelectedClientPacks);
    }

    static String serverKnownDescription(boolean includeSelectedClientPacks) {
        return includeSelectedClientPacks ? "server-selected known packs" : "server-selected vanilla packs";
    }

    static List<String> localCacheSignature(Minecraft minecraft, boolean includeSelectedClientPacks) {
        if (!includeSelectedClientPacks) {
            return List.of();
        }

        return List.copyOf(minecraft.getResourcePackRepository().getSelectedIds()).stream()
                .map(id -> "client:" + id)
                .toList();
    }

    private static List<PackResources> createLocalPackResources(Minecraft minecraft, boolean includeSelectedClientPacks) {
        if (!includeSelectedClientPacks) {
            return List.of(new NonClosingPackResources(minecraft.getVanillaPackResources()));
        }

        PackRepository repository = minecraft.getResourcePackRepository();
        List<PackResources> resources = new ArrayList<>();
        boolean includedVanilla = false;

        for (Pack pack : repository.getSelectedPacks()) {
            if (isVanillaPack(pack)) {
                resources.add(new NonClosingPackResources(minecraft.getVanillaPackResources()));
                includedVanilla = true;
                continue;
            }

            resources.add(pack.open());
        }

        if (!includedVanilla) {
            resources.addFirst(new NonClosingPackResources(minecraft.getVanillaPackResources()));
        }

        return resources;
    }

    private static CloseableResourceManager createKnownPackResourceManager(boolean includeSelectedClientPacks) {
        List<KnownPack> knownPacks = filteredKnownPacks(includeSelectedClientPacks);
        if (knownPacks.isEmpty()) {
            return null;
        }

        KnownPacksManager knownPacksManager = new KnownPacksManager();
        List<KnownPack> selectedKnownPacks = knownPacksManager.trySelectingPacks(knownPacks);
        if (selectedKnownPacks.isEmpty()) {
            return null;
        }

        return knownPacksManager.createResourceManager();
    }

    private static List<KnownPack> filteredKnownPacks(boolean includeSelectedClientPacks) {
        ServerKnownPacksState.Snapshot snapshot = ServerKnownPacksState.snapshot();
        if (!snapshot.selectionReceived()) {
            return List.of();
        }

        return snapshot.selectedKnownPacks().stream()
                .filter(pack -> includeSelectedClientPacks || pack.isVanilla())
                .toList();
    }

    private static List<String> knownPackCacheSignature(List<KnownPack> knownPacks) {
        return knownPacks.stream()
                .map(pack -> "server:" + pack.namespace() + ":" + pack.id() + ":" + pack.version())
                .toList();
    }

    private static boolean isVanillaPack(Pack pack) {
        return "vanilla".equals(pack.getId())
                || pack.location().knownPackInfo().map(KnownPack::isVanilla).orElse(false);
    }
}

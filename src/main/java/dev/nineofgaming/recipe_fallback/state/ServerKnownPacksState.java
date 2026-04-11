package dev.nineofgaming.recipe_fallback.state;

import net.minecraft.server.packs.repository.KnownPack;

import java.util.Collection;
import java.util.List;

public final class ServerKnownPacksState {
    private static volatile boolean selectionReceived;
    private static volatile List<KnownPack> selectedKnownPacks = List.of();

    private ServerKnownPacksState() {
    }

    public static void clear() {
        selectionReceived = false;
        selectedKnownPacks = List.of();
    }

    public static void setSelectedKnownPacks(Collection<KnownPack> knownPacks) {
        selectionReceived = true;
        selectedKnownPacks = List.copyOf(knownPacks);
    }

    public static Snapshot snapshot() {
        return new Snapshot(selectionReceived, selectedKnownPacks);
    }

    public record Snapshot(
            boolean selectionReceived,
            List<KnownPack> selectedKnownPacks
    ) {
    }
}

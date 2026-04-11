package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.state.ServerKnownPacksState;
import net.minecraft.client.multiplayer.KnownPacksManager;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl.class)
abstract class ClientConfigurationPacketListenerMixin {
    @Inject(method = "handleSelectKnownPacks", at = @At("TAIL"))
    private void recipe_fallback$storeSelectedKnownPacks(
            ClientboundSelectKnownPacks packet,
            CallbackInfo callbackInfo
    ) {
        KnownPacksManager knownPacksManager = new KnownPacksManager();
        ServerKnownPacksState.setSelectedKnownPacks(knownPacksManager.trySelectingPacks(packet.knownPacks()));
    }
}

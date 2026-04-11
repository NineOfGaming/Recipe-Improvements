package dev.nineofgaming.recipe_fallback.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

final class NonClosingPackResources implements PackResources {
    private final PackResources delegate;

    NonClosingPackResources(PackResources delegate) {
        this.delegate = delegate;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String @NonNull ... strings) {
        return this.delegate.getRootResource(strings);
    }

    @Override
    public IoSupplier<InputStream> getResource(@NonNull PackType packType, @NonNull Identifier identifier) {
        return this.delegate.getResource(packType, identifier);
    }

    @Override
    public void listResources(@NonNull PackType packType, @NonNull String namespace, @NonNull String path, @NonNull ResourceOutput resourceOutput) {
        this.delegate.listResources(packType, namespace, path, resourceOutput);
    }

    @Override
    public @NonNull Set<String> getNamespaces(@NonNull PackType packType) {
        return this.delegate.getNamespaces(packType);
    }

    @Override
    public <T> T getMetadataSection(@NonNull MetadataSectionType<T> metadataSectionType) throws IOException {
        return this.delegate.getMetadataSection(metadataSectionType);
    }

    @Override
    public @NonNull PackLocationInfo location() {
        return this.delegate.location();
    }

    @Override
    public void close() {
    }
}

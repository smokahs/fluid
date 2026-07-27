package com.fluidunit.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;

import com.fluidunit.FluidUnit;

// serves the generated tags and tooltips. a fresh instance is made for every reload, so the lazy
// generation below is also what picks up a config edit followed by /reload.
public final class Resources implements PackResources {

    private final String id;
    private final PackType type;

    @Nullable
    private Gen.Generated generated;

    public Resources(String id, PackType type) {
        this.id = id;
        this.type = type;
    }

    private Map<ResourceLocation, byte[]> files(PackType packType) {
        Gen.Generated g = this.generated;
        if (g == null) {
            try {
                g = Gen.generate();
            } catch (Throwable failed) {
                FluidUnit.LOGGER.error("Failed to generate the fluid unit pack", failed);
                g = new Gen.Generated(Map.of(), Map.of());
            }
            this.generated = g;
        }
        return packType == PackType.SERVER_DATA ? g.data() : g.assets();
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        byte[] bytes = files(packType).get(location);
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput out) {
        for (Map.Entry<ResourceLocation, byte[]> entry : files(packType).entrySet()) {
            ResourceLocation location = entry.getKey();
            if (location.getNamespace().equals(namespace) && location.getPath().startsWith(path)) {
                byte[] bytes = entry.getValue();
                out.accept(location, () -> new ByteArrayInputStream(bytes));
            }
        }
    }

    // read off the files themselves. the pack writes into forge's and tinkers' namespaces, not its
    // own, and a namespace left out here is never asked for.
    @Override
    public Set<String> getNamespaces(PackType packType) {
        Set<String> namespaces = new HashSet<>();
        for (ResourceLocation location : files(packType).keySet()) {
            namespaces.add(location.getNamespace());
        }
        return namespaces;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        if (deserializer == PackMetadataSection.TYPE) {
            return (T) new PackMetadataSection(
                    Component.literal("Fluid Unit tags and tooltips"),
                    SharedConstants.getCurrentVersion().getPackVersion(type));
        }
        return null;
    }

    @Override
    public String packId() {
        return id;
    }

    @Override
    public void close() {}
}

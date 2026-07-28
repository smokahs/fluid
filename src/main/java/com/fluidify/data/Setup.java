package com.fluidify.data;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.fluidify.Fluidify;

// registers the generated pack. it goes on top so its tooltip units win over the ones tinkers
// ships, which are the same file id.
@Mod.EventBusSubscriber(modid = Fluidify.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Setup {

    private static final String PACK_ID = "fluidify_generated";

    private Setup() {}

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        PackType type = event.getPackType();
        Pack pack = Pack.readMetaAndCreate(
                PACK_ID,
                Component.literal("Fluidify"),
                true,
                id -> new Resources(id, type),
                type,
                Pack.Position.TOP,
                PackSource.BUILT_IN);
        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }
}

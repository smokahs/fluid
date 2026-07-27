package com.fluidunit.command;

import java.util.List;
import java.util.Map;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.fluidunit.FluidUnit;
import com.fluidunit.unit.Context;

import com.mojang.brigadier.context.CommandContext;

// the mapping is worked out from whatever fluids are actually registered, so there is no shipped
// table to read. this prints what it settled on, which is the fastest way to find a metal that did
// not line up.
@Mod.EventBusSubscriber(modid = FluidUnit.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Dump {

    private Dump() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal(FluidUnit.MOD_ID)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("dump").executes(Dump::run)));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        Context context = Context.latest();
        Map<String, String> aliases = context.aliases();
        List<String> unmatched = context.unmatched();

        say(ctx, "Unit: " + context.target() + "mB per ingot");
        say(ctx, "Rescaled mods: " + context.sourceUnits());
        say(ctx, "Folded fluids: " + aliases.size() + ", left alone: " + unmatched.size());
        say(ctx, "Full mapping written to the log");

        FluidUnit.LOGGER.info("Fluid unit: {}mB per ingot", context.target());
        FluidUnit.LOGGER.info("Rescaled mods: {}", context.sourceUnits());
        aliases.forEach((from, to) -> FluidUnit.LOGGER.info("  {} -> {}", from, to));
        for (String fluid : unmatched) {
            FluidUnit.LOGGER.info("  {} has no counterpart, kept as is", fluid);
        }
        return aliases.size();
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String line) {
        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
    }
}

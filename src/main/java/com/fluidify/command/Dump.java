package com.fluidify.command;

import java.util.List;
import java.util.Map;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.fluidify.Fluidify;
import com.fluidify.unit.Context;

import com.mojang.brigadier.context.CommandContext;


@Mod.EventBusSubscriber(modid = Fluidify.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Dump {

    private Dump() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal(Fluidify.MOD_ID)
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

        Fluidify.LOGGER.info("Fluidify unit: {}mB per ingot", context.target());
        Fluidify.LOGGER.info("Rescaled mods: {}", context.sourceUnits());
        aliases.forEach((from, to) -> Fluidify.LOGGER.info("  {} -> {}", from, to));
        for (String fluid : unmatched) {
            Fluidify.LOGGER.info("  {} has no counterpart, kept as is", fluid);
        }
        return aliases.size();
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String line) {
        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
    }
}

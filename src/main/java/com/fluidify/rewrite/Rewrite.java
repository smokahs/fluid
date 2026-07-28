package com.fluidify.rewrite;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;

import com.fluidify.Fluidify;
import com.fluidify.config.Cfg;
import com.fluidify.unit.Context;

import com.google.gson.JsonElement;

// entry point for the reload hook. runs over the raw recipe json before anything deserialises it.
public final class Rewrite {

    private Rewrite() {}

    public static void run(Map<ResourceLocation, JsonElement> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return;
        }
        // the config is read on every reload rather than cached, so editing it and running /reload
        // is enough. the spec check covers the odd order a dedicated server can start listeners in.
        if (!Cfg.SPEC.isLoaded() || !Cfg.INSTANCE.enabled.get()) {
            return;
        }

        try {
            Context context = Context.build();
            Report report = new Report();
            for (Map.Entry<ResourceLocation, JsonElement> entry : recipes.entrySet()) {
                ResourceLocation id = entry.getKey();
                if (context.excluded(id)) {
                    continue;
                }
                Walker.walk(entry.getValue(), id, context.sourceUnit(id.getNamespace()), context, report);
            }
            report.log(context);
        } catch (Throwable failed) {
            // a broken pass would otherwise take the whole datapack reload with it. better to ship
            // the recipes as written and say so loudly.
            Fluidify.LOGGER.error("Fluidify conversion failed, recipes are unchanged", failed);
        }
    }
}

package com.fluidify.rewrite;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

import com.fluidify.Fluidify;
import com.fluidify.config.Cfg;
import com.fluidify.unit.Context;

public final class Report {

    private static final int SAMPLES = 8;

    int scaled;
    int aliased;
    int resolvedTags;
    int rounded;

    private final Set<String> unknown = new LinkedHashSet<>();
    private final List<String> roundedSamples = new ArrayList<>();

    // a mod using a shared molten metal that is in neither unit list keeps whatever amounts it
    // shipped, which is how a melt-here cast-there loop gets back in. worth a line every reload.
    void unknown(String namespace) {
        unknown.add(namespace);
    }

    void rounded(ResourceLocation recipe, int from, int to) {
        rounded++;
        if (roundedSamples.size() < SAMPLES) {
            roundedSamples.add(recipe + " " + from + "->" + to);
        }
    }

    public void log(Context context) {
        if (Cfg.INSTANCE.logSummary.get() && (scaled > 0 || aliased > 0 || resolvedTags > 0)) {
            Fluidify.LOGGER.info(
                    "Fluid unit {}mB/ingot: rescaled {} amounts, repointed {} fluids, resolved {} output tags",
                    context.target(), scaled, aliased, resolvedTags);
        }
        if (rounded > 0 && Cfg.INSTANCE.logRounding.get()) {
            Fluidify.LOGGER.warn("{} amounts did not divide evenly and were rounded: {}", rounded, roundedSamples);
        }
        if (!unknown.isEmpty() && Cfg.INSTANCE.warnUnknownNamespaces.get()) {
            Fluidify.LOGGER.warn(
                    "These mods use a shared molten metal but are in neither unit list, so their amounts were "
                            + "left alone: {}. Add each as namespace=millibuckets under unit.sourceUnits, or to "
                            + "unit.nativeNamespaces if it already writes {}mB per ingot.",
                    unknown, context.target());
        }
    }
}

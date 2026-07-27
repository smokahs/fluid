package com.fluidunit.data;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

import com.fluidunit.FluidUnit;
import com.fluidunit.config.Cfg;
import com.fluidunit.unit.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

// the two things the json rewrite cannot reach. tags, because they are not recipes and the winning
// mod ships none of them, so every tinkers recipe asking for forge:molten_iron would otherwise be
// blind to the fluid it was just repointed at. and tinkers' tooltip units, because they are a flat
// assertion that an ingot is 90mB.
public final class Gen {

    private Gen() {}

    public record Generated(Map<ResourceLocation, byte[]> data, Map<ResourceLocation, byte[]> assets) {}

    private static final String FORGE = "forge";

    public static Generated generate() {
        Map<ResourceLocation, byte[]> data = new LinkedHashMap<>();
        Map<ResourceLocation, byte[]> assets = new LinkedHashMap<>();
        Context context = Context.latest();

        if (Cfg.INSTANCE.emitFluidTags.get()) {
            fluidTags(context, data);
        }
        if (Cfg.INSTANCE.emitTinkersTooltips.get() && FluidUnit.loaded(FluidUnit.TIC)) {
            tooltips(context, assets);
        }

        FluidUnit.LOGGER.info("Generated {} tag files and {} tooltip files", data.size(), assets.size());
        return new Generated(data, assets);
    }

    // the winning fluid joins forge:molten_<metal>, which is what tinkers' own recipes and its
    // tooltips/metal tag are both written against, so one file per metal covers both.
    private static void fluidTags(Context context, Map<ResourceLocation, byte[]> out) {
        JsonArray all = new JsonArray();
        context.byMetal().forEach((metal, fluid) -> {
            out.put(new ResourceLocation(FORGE, "tags/fluids/molten_" + metal + ".json"),
                    bytes(tag(fluid)));
            all.add(entry(fluid));
        });
        if (all.isEmpty() || !FluidUnit.loaded(FluidUnit.TIC)) {
            return;
        }
        // metals tinkers has no molten fluid of its own for are not in its tooltip tag either, so
        // they are added directly rather than through forge:molten_*.
        JsonObject tooltip = new JsonObject();
        tooltip.addProperty("replace", false);
        tooltip.add("values", all);
        out.put(new ResourceLocation(FluidUnit.TIC, "tags/fluids/tooltips/metal.json"), bytes(tooltip));
    }

    private static void tooltips(Context context, Map<ResourceLocation, byte[]> out) {
        int ingot = context.target();

        JsonObject ingots = new JsonObject();
        ingots.add("units", units(new String[] {"gui.tconstruct.fluid.ingot"}, new int[] {ingot}));
        out.put(tooltip("ingots"), bytes(ingots));

        JsonObject metals = new JsonObject();
        metals.addProperty("tag", "tconstruct:tooltips/metal");
        metals.add("units", ingot % 9 == 0
                ? units(new String[] {"gui.tconstruct.fluid.block", "gui.tconstruct.fluid.ingot",
                        "gui.tconstruct.fluid.nugget"},
                        new int[] {ingot * 9, ingot, ingot / 9})
                : units(new String[] {"gui.tconstruct.fluid.block", "gui.tconstruct.fluid.ingot"},
                        new int[] {ingot * 9, ingot}));
        out.put(tooltip("metals"), bytes(metals));
    }

    private static ResourceLocation tooltip(String name) {
        return new ResourceLocation(FluidUnit.TIC, "mantle/fluid_tooltips/" + name + ".json");
    }

    private static JsonArray units(String[] keys, int[] needed) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < keys.length; i++) {
            JsonObject unit = new JsonObject();
            unit.addProperty("key", keys[i]);
            unit.addProperty("needed", needed[i]);
            array.add(unit);
        }
        return array;
    }

    private static JsonObject tag(String fluid) {
        JsonArray values = new JsonArray();
        values.add(entry(fluid));
        JsonObject o = new JsonObject();
        o.addProperty("replace", false);
        o.add("values", values);
        return o;
    }

    // required false, so a metal the winning mod drops in a later version does not break the tag.
    private static JsonObject entry(String fluid) {
        JsonObject o = new JsonObject();
        o.addProperty("id", fluid);
        o.addProperty("required", false);
        return o;
    }

    private static byte[] bytes(JsonObject json) {
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }
}

package com.fluidify.rewrite;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import com.fluidify.config.Cfg;
import com.fluidify.unit.Context;
import com.fluidify.unit.Units;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

// walks a recipe's json and edits fluid nodes in place. going through the json rather than the
// deserialised recipe is the whole point: every mod's own serializer then builds the corrected
// recipe itself, so nothing here has to know what a tinkers casting recipe or a create mixing
// recipe looks like.
public final class Walker {

    private Walker() {}

    private static final int MAX_DEPTH = 48;

    public static void walk(JsonElement element, ResourceLocation recipe, int sourceUnit,
                            Context context, Report report) {
        walk(element, recipe, sourceUnit, false, 0, context, report);
    }

    private static void walk(JsonElement element, ResourceLocation recipe, int sourceUnit,
                             boolean output, int depth, Context context, Report report) {
        if (depth > MAX_DEPTH) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                walk(child, recipe, sourceUnit, output, depth + 1, context, report);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject object = element.getAsJsonObject();
        // the node is edited before its children are read, so nothing is structurally changed while
        // the entry set is being iterated.
        node(object, recipe, sourceUnit, output, context, report);

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement child = entry.getValue();
            if (child.isJsonObject() || child.isJsonArray()) {
                walk(child, recipe, sourceUnit, side(entry.getKey(), output), depth + 1, context, report);
            }
        }
    }

    // which side of the recipe we are under, so a tag written as a result can be turned into a real
    // fluid while a tag written as an ingredient is left broad. unknown keys inherit.
    private static boolean side(String key, boolean inherited) {
        if (Keys.OUTPUT.contains(key)) {
            return true;
        }
        if (Keys.INPUT.contains(key)) {
            return false;
        }
        return inherited;
    }

    private static void node(JsonObject object, ResourceLocation recipe, int sourceUnit,
                             boolean output, Context context, Report report) {
        for (String marker : Keys.ITEM_MARKERS) {
            if (object.has(marker)) {
                return;
            }
        }
        int amount = amount(object);
        if (amount <= 0) {
            return;
        }

        for (String key : Keys.FLUID_ID) {
            String id = string(object, key);
            if (id == null) {
                continue;
            }
            ResourceLocation fluid = ResourceLocation.tryParse(id);
            if (fluid == null || context.metalOfFluid(fluid) == null) {
                return;
            }
            scale(object, amount, sourceUnit, recipe, context, report);
            String canonical = context.alias(id);
            if (canonical != null && !canonical.equals(id)) {
                object.addProperty(key, canonical);
                report.aliased++;
            }
            return;
        }

        for (String key : Keys.FLUID_TAG) {
            String id = string(object, key);
            if (id == null) {
                continue;
            }
            ResourceLocation tag = ResourceLocation.tryParse(id);
            String metal = tag == null ? null : context.metalOfTag(tag);
            if (metal == null) {
                return;
            }
            scale(object, amount, sourceUnit, recipe, context, report);

            // an ingredient stays a tag, since the generated pack puts the winning fluid in it. a
            // result cannot: the tag would hand back whichever fluid happens to be listed first.
            if (output && Cfg.INSTANCE.resolveOutputTags.get()) {
                String canonical = context.canonicalFor(metal);
                if (canonical != null) {
                    object.remove(key);
                    object.addProperty(Keys.FLUID, canonical);
                    report.resolvedTags++;
                }
            }
            return;
        }
    }

    private static void scale(JsonObject object, int amount, int sourceUnit, ResourceLocation recipe,
                              Context context, Report report) {
        if (sourceUnit == Units.NATIVE) {
            return;
        }
        if (sourceUnit == Units.UNKNOWN) {
            report.unknown(recipe.getNamespace());
            return;
        }
        int scaled = Units.scale(amount, sourceUnit, context.target());
        if (scaled == amount) {
            return;
        }
        if (!Units.exact(amount, sourceUnit, context.target())) {
            report.rounded(recipe, amount, scaled);
        }
        object.addProperty(Keys.AMOUNT, scaled);
        report.scaled++;
    }

    private static int amount(JsonObject object) {
        JsonElement element = object.get(Keys.AMOUNT);
        if (element == null || !element.isJsonPrimitive()) {
            return 0;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (!primitive.isNumber()) {
            return 0;
        }
        try {
            return primitive.getAsInt();
        } catch (NumberFormatException notAnInt) {
            return 0;
        }
    }

    @Nullable
    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return null;
        }
        String value = element.getAsString();
        return value.isEmpty() ? null : value;
    }
}

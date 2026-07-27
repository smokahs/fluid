package com.fluidunit.unit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.registries.ForgeRegistries;

import com.fluidunit.FluidUnit;
import com.fluidunit.config.Cfg;

// works out which molten fluid stands in for which. built off the fluid registry rather than a
// shipped table, so a metal added by any mod is picked up as long as its name matches.
public final class Aliases {

    private Aliases() {}

    public record Result(Map<String, String> map, Set<String> canonical, List<String> unmatched) {}

    public static Result build(Set<String> metals, List<String> prefixes) {
        String canonicalNs = Cfg.INSTANCE.canonicalNamespace.get().trim();
        Map<String, String> overrides = Units.pairs(Cfg.INSTANCE.aliasOverrides.get());
        List<String> patterns = new ArrayList<>(Cfg.INSTANCE.canonicalPatterns.get());
        Map<String, String> spellings = spellings();
        int minTemperature = Cfg.INSTANCE.minMoltenTemperature.get();

        Map<String, String> map = new LinkedHashMap<>();
        Set<String> canonical = new LinkedHashSet<>();
        List<String> unmatched = new ArrayList<>();

        for (ResourceLocation key : ForgeRegistries.FLUIDS.getKeys()) {
            if (key.getNamespace().equals(canonicalNs)) {
                continue;
            }
            String metal = metal(key.getPath(), prefixes, metals);
            if (metal == null) {
                continue;
            }

            String id = key.toString();
            String override = overrides.get(id);
            if (override != null) {
                // an override with nothing on the right pins the fluid where it is.
                if (!override.isEmpty()) {
                    map.put(id, override);
                    canonical.add(override);
                }
                continue;
            }

            String target = match(canonicalNs, metal, patterns, spellings, minTemperature);
            if (target == null) {
                unmatched.add(id);
            } else {
                map.put(id, target);
                canonical.add(target);
            }
        }

        return new Result(map, canonical, unmatched);
    }

    /** The metal a fluid name refers to, or null when it is not a melt we handle. */
    @Nullable
    public static String metal(String path, List<String> prefixes, Set<String> metals) {
        // only the still fluid is ever named in a recipe, so the flowing half is left out of the
        // whole scheme rather than mapped onto a still fluid it is not.
        if (path.startsWith("flowing_")) {
            return null;
        }
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                String metal = path.substring(prefix.length());
                if (metals.contains(metal)) {
                    return metal;
                }
            }
        }
        return null;
    }

    @Nullable
    private static String match(String namespace, String metal, List<String> patterns,
                                Map<String, String> spellings, int minTemperature) {
        for (String spelling : new String[] {metal, spellings.get(metal)}) {
            if (spelling == null) {
                continue;
            }
            for (String pattern : patterns) {
                String candidate = namespace + ":" + pattern.replace("%s", spelling);
                if (isMolten(candidate, minTemperature)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    // a name match alone would happily fold molten lithium onto a mod's cold lithium chemical, so
    // the candidate has to actually be hot.
    private static boolean isMolten(String id, int minTemperature) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return false;
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(location);
        if (fluid == null || !ForgeRegistries.FLUIDS.containsKey(location)) {
            return false;
        }
        try {
            return fluid.getFluidType().getTemperature() >= minTemperature;
        } catch (Throwable unreadable) {
            FluidUnit.LOGGER.debug("Could not read the temperature of {}", id, unreadable);
            return false;
        }
    }

    private static Map<String, String> spellings() {
        Map<String, String> both = new LinkedHashMap<>();
        Units.pairs(Cfg.INSTANCE.spellings.get()).forEach((one, other) -> {
            both.put(one, other);
            both.put(other, one);
        });
        return both;
    }
}

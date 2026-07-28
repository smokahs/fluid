package com.fluidify.unit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import com.fluidify.config.Cfg;

// everything the rewrite needs, resolved once per reload and never mutated after. the walk runs off
// thread with the rest of resource preparation, so nothing may be lazily filled in mid-pass.
public final class Context {

    @Nullable
    private static volatile Context latest;

    private final int target;
    private final Map<String, Integer> sourceUnits;
    private final Set<String> nativeNamespaces;
    private final Set<String> metals;
    private final List<String> prefixes;
    private final List<String> excluded;
    private final Map<String, String> aliases;
    private final Map<String, String> byMetal;
    private final Set<String> canonical;
    private final List<String> unmatched;

    private Context(int target, Map<String, Integer> sourceUnits, Set<String> nativeNamespaces,
                    Set<String> metals, List<String> prefixes, List<String> excluded,
                    Aliases.Result aliases) {
        this.target = target;
        this.sourceUnits = sourceUnits;
        this.nativeNamespaces = nativeNamespaces;
        this.metals = metals;
        this.prefixes = prefixes;
        this.excluded = excluded;
        this.aliases = aliases.map();
        this.canonical = aliases.canonical();
        this.unmatched = aliases.unmatched();

        Map<String, String> perMetal = new LinkedHashMap<>();
        aliases.map().forEach((from, to) -> {
            ResourceLocation source = ResourceLocation.tryParse(from);
            if (source != null) {
                String metal = Aliases.metal(source.getPath(), prefixes, metals);
                if (metal != null) {
                    perMetal.putIfAbsent(metal, to);
                }
            }
        });
        this.byMetal = perMetal;
    }

    public static Context build() {
        Set<String> metals = new LinkedHashSet<>(Cfg.INSTANCE.metals.get());
        List<String> prefixes = new ArrayList<>(Cfg.INSTANCE.sourcePrefixes.get());
        Aliases.Result resolved = Cfg.INSTANCE.unify.get()
                ? Aliases.build(metals, prefixes)
                : new Aliases.Result(Map.of(), Set.of(), List.of());

        Context context = new Context(
                Cfg.INSTANCE.ingot.get(),
                Units.intPairs(Cfg.INSTANCE.sourceUnits.get()),
                new LinkedHashSet<>(Cfg.INSTANCE.nativeNamespaces.get()),
                metals,
                prefixes,
                new ArrayList<>(Cfg.INSTANCE.excludedRecipes.get()),
                resolved);
        latest = context;
        return context;
    }

    /** The context the last reload ran with, built on demand for the command. */
    public static Context latest() {
        Context context = latest;
        return context == null ? build() : context;
    }

    public int target() {
        return target;
    }

    public Map<String, String> aliases() {
        return aliases;
    }

    public Set<String> canonical() {
        return canonical;
    }

    public List<String> unmatched() {
        return unmatched;
    }

    public Map<String, Integer> sourceUnits() {
        return sourceUnits;
    }

    public Set<String> metals() {
        return metals;
    }

    /** Every metal we have a winning fluid for, mapped to it. */
    public Map<String, String> byMetal() {
        return byMetal;
    }

    public int sourceUnit(String namespace) {
        if (nativeNamespaces.contains(namespace)) {
            return Units.NATIVE;
        }
        Integer unit = sourceUnits.get(namespace);
        if (unit == null) {
            return Units.UNKNOWN;
        }
        return unit == target ? Units.NATIVE : unit;
    }

    public boolean excluded(ResourceLocation recipe) {
        if (excluded.isEmpty()) {
            return false;
        }
        String id = recipe.toString();
        for (String pattern : excluded) {
            if (pattern.endsWith("*") ? id.startsWith(pattern.substring(0, pattern.length() - 1)) : id.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    /** The metal a fluid id names, or null when it is not one of ours. */
    @Nullable
    public String metalOfFluid(ResourceLocation fluid) {
        return Aliases.metal(fluid.getPath(), prefixes, metals);
    }

    // tags nest, so forge:molten_iron and a pack's own metals/molten_iron both answer iron.
    @Nullable
    public String metalOfTag(ResourceLocation tag) {
        String path = tag.getPath();
        int slash = path.lastIndexOf('/');
        return Aliases.metal(slash < 0 ? path : path.substring(slash + 1), prefixes, metals);
    }

    @Nullable
    public String alias(String fluid) {
        return aliases.get(fluid);
    }

    @Nullable
    public String canonicalFor(String metal) {
        return byMetal.get(metal);
    }
}

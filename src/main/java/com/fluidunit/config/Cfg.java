package com.fluidunit.config;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

import org.apache.commons.lang3.tuple.Pair;

public final class Cfg {

    public static final Cfg INSTANCE;
    public static final ForgeConfigSpec SPEC;

    static {
        Pair<Cfg, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Cfg::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    // gregtech's ingot, and the two the tinkers side ships with.
    public static final int GT_INGOT = 144;
    public static final int TIC_INGOT = 90;

    public final BooleanValue enabled;
    public final IntValue ingot;
    public final ConfigValue<List<? extends String>> sourceUnits;
    public final ConfigValue<List<? extends String>> nativeNamespaces;
    public final BooleanValue warnUnknownNamespaces;

    public final BooleanValue unify;
    public final ConfigValue<String> canonicalNamespace;
    public final ConfigValue<List<? extends String>> canonicalPatterns;
    public final ConfigValue<List<? extends String>> aliasOverrides;
    public final ConfigValue<List<? extends String>> spellings;
    public final IntValue minMoltenTemperature;
    public final BooleanValue resolveOutputTags;

    public final ConfigValue<List<? extends String>> metals;
    public final ConfigValue<List<? extends String>> sourcePrefixes;
    public final ConfigValue<List<? extends String>> excludedRecipes;

    public final BooleanValue emitFluidTags;
    public final BooleanValue emitTinkersTooltips;

    public final BooleanValue logSummary;
    public final BooleanValue logRounding;

    private Cfg(ForgeConfigSpec.Builder b) {
        b.comment("How many millibuckets one ingot of metal is worth, and which mods get moved onto it.")
                .push("unit");
        enabled = b
                .comment("Master switch. Off leaves every recipe exactly as its mod wrote it.")
                .define("enabled", true);
        ingot = b
                .comment("Millibuckets per ingot after conversion. 144 is GregTech's; 90 is Tinkers'.",
                        "Block and nugget follow as ingot * 9 and ingot / 9.")
                .defineInRange("ingot", GT_INGOT, 1, 100_000);
        sourceUnits = b
                .comment("Mods whose recipes are written in a different unit, as namespace=millibuckets.",
                        "A recipe is only rescaled when its own namespace is listed here.",
                        "tinkersmetallurgy and tic3nh are addons that write Tinkers' 90 themselves.")
                .defineList("sourceUnits",
                        List.of("tconstruct=90",
                                "createmetallurgy=90",
                                "tinkersmetallurgy=90",
                                "tic3nh=90"),
                        Cfg::isPair);
        nativeNamespaces = b
                .comment("Mods already writing the unit above. Skipped silently instead of being warned about.")
                .defineList("nativeNamespaces", List.of("gtceu", "tfmg"), Cfg::isName);
        warnUnknownNamespaces = b
                .comment("Warn once per reload about mods that use a shared molten metal but are in neither",
                        "list above, so their amounts pass through untouched. Leave this on: an unlisted mod",
                        "on a different unit is exactly how a duplication loop gets in.")
                .define("warnUnknownNamespaces", true);
        b.pop();

        b.comment("Folding several mods' molten iron onto one fluid, so machines can share tanks.")
                .push("unify");
        unify = b
                .comment("Repoint recipes at one mod's molten fluids.")
                .define("unify", true);
        canonicalNamespace = b
                .comment("The mod whose molten fluids win.")
                .define("canonicalNamespace", "gtceu");
        canonicalPatterns = b
                .comment("Name patterns tried, in order, when looking for the winning mod's version of a metal.",
                        "%s is the metal name. GregTech drops the molten_ prefix when a material has only the",
                        "one fluid, so both spellings have to be tried.")
                .defineList("canonicalPatterns", List.of("molten_%s", "%s"), Cfg::isName);
        aliasOverrides = b
                .comment("Manual fluid mapping as from=to. An empty right hand side pins a fluid, so it is",
                        "never folded away. Applied before the automatic match.")
                .defineList("aliasOverrides", List.of(), Cfg::isPair);
        spellings = b
                .comment("Alternate spellings tried when matching a metal, as one=other. Tried both ways.")
                .defineList("spellings",
                        List.of("aluminum=aluminium",
                                "chromium=chrome",
                                "tungsten_steel=tungstensteel"),
                        Cfg::isPair);
        minMoltenTemperature = b
                .comment("A candidate fluid is only accepted as a molten metal above this temperature, in",
                        "kelvin. Stops a cold chemical of the same name being mistaken for the melt.")
                .defineInRange("minMoltenTemperature", 500, 0, 100_000);
        resolveOutputTags = b
                .comment("Turn a tag written as a recipe result into the winning mod's fluid.",
                        "Off, the result stays a tag and whichever fluid the tag lists first is produced.")
                .define("resolveOutputTags", true);
        b.pop();

        b.comment("Which fluids count as ingot denominated metal. Everything else is left alone.")
                .push("metals");
        metals = b
                .comment("Metal names, without the molten_ prefix. Only these get rescaled or folded, so",
                        "molten clay, glass, slime and the rest keep the values their own mod balanced.",
                        "Names with no matching fluid cost nothing.")
                .defineList("metals",
                        List.of(
                                // vanilla and tinkers
                                "iron", "gold", "copper", "cobalt", "netherite", "debris",
                                "slimesteel", "amethyst_bronze", "rose_gold", "pig_iron", "manyullyn",
                                "hepatizon", "queens_slime", "cinderslime", "soulsteel", "knightmetal",
                                "knightslime", "steeleaf",
                                // common addon metals
                                "steel", "tin", "aluminum", "aluminium", "lead", "silver", "nickel",
                                "zinc", "platinum", "tungsten", "osmium", "uranium", "chromium",
                                "chrome", "cadmium", "titanium", "iridium",
                                // alloys
                                "bronze", "brass", "electrum", "invar", "constantan", "pewter",
                                "enderium", "lumium", "signalum", "refined_glowstone",
                                "refined_obsidian", "nicrosil", "duralumin", "bendalloy",
                                "stainless_steel", "tungsten_steel", "tungstensteel", "magnalium",
                                // create metallurgy
                                "obdurium", "void_steel", "necromium", "lithium",
                                // tic3nh, which is the whole gregtech alloy tree on tinkers' unit
                                "wrought_iron", "blue_steel", "red_steel", "cobalt_brass",
                                "damascus_steel", "duranium", "hssg", "hsse", "hsss", "naquadah",
                                "naquadah_alloy", "neutronium", "sterling_silver", "tungsten_carbide",
                                "ultimet", "vanadium_steel", "tritanium", "black_bronze",
                                "bismuth_bronze", "cupronickel", "kanthal", "nichrome", "potin",
                                "soldering_alloy", "red_alloy", "blue_alloy", "mithril"),
                        Cfg::isName);
        sourcePrefixes = b
                .comment("Fluid name prefixes that mark a melt in the mods being converted.")
                .defineList("sourcePrefixes", List.of("molten_"), Cfg::isName);
        excludedRecipes = b
                .comment("Recipe ids left completely alone. A trailing * matches a prefix,",
                        "for example tconstruct:smeltery/casting/*.")
                .defineList("excludedRecipes", List.of(), Cfg::isName);
        b.pop();

        b.comment("Generated files shipped as a built in pack, on top of everything else.")
                .push("pack");
        emitFluidTags = b
                .comment("Add the winning mod's molten fluids to the forge:molten_* tags. Without this,",
                        "Tinkers' recipes, which ask for the tag, cannot see them at all.")
                .define("emitFluidTags", true);
        emitTinkersTooltips = b
                .comment("Rewrite Tinkers' ingot and metal tooltip units so tanks read in the unit above",
                        "instead of claiming an ingot is 90mB.")
                .define("emitTinkersTooltips", true);
        b.pop();

        b.push("logging");
        logSummary = b
                .comment("One line per reload with what was rescaled and folded.")
                .define("logSummary", true);
        logRounding = b
                .comment("Warn when an amount does not divide evenly and had to be rounded.")
                .define("logRounding", true);
        b.pop();
    }

    private static boolean isName(Object o) {
        return o instanceof String s && !s.isBlank();
    }

    // right hand side may be empty, which is how a fluid gets pinned.
    private static boolean isPair(Object o) {
        return o instanceof String s && s.indexOf('=') > 0;
    }
}

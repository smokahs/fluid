package com.fluidunit;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import com.fluidunit.config.Cfg;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(FluidUnit.MOD_ID)
public final class FluidUnit {

    public static final String MOD_ID = "fluidunit";

    // the mod whose fluid and whose unit everything else is moved onto by default.
    public static final String GT = "gtceu";

    public static final String TIC = "tconstruct";

    public static final Logger LOGGER = LogUtils.getLogger();

    public FluidUnit() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Cfg.SPEC);
    }

    public static boolean loaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}

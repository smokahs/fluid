package com.fluidunit.mixin;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;

import com.fluidunit.rewrite.Rewrite;

import com.google.gson.JsonElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// the hook is on prepare rather than on RecipeManager.apply on purpose. kubejs injects at the head
// of apply, hands the map to its own recipe event and then cancels vanilla outright, so anything
// else sitting on apply is in an ordering fight it can only win by luck. prepare is what builds the
// map every one of those handlers is later given, so editing it there lands first no matter what.
// the base class is shared with every other json listener, hence the instance check.
@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class JsonReloadMixin {

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;"
            + "Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;",
            at = @At("RETURN"))
    private void fluidunit$normalise(ResourceManager resourceManager,
                                     ProfilerFiller profiler,
                                     CallbackInfoReturnable<Map<ResourceLocation, JsonElement>> cir) {
        if ((Object) this instanceof RecipeManager) {
            Rewrite.run(cir.getReturnValue());
        }
    }
}

package com.norboc.serverfixtweaks.mixin;

import com.norboc.serverfixtweaks.ServerFixTweaks;
import com.norboc.serverfixtweaks.config.ServerFixTweaksConfig;
import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla {@link BeehiveDecorator#place} reads {@code context.logs().get(0)} without an
 * empty check. Modded trees that record no log positions therefore crash world generation
 * with an {@code IndexOutOfBoundsException: Index 0 out of bounds for length 0}. Skipping
 * the decorator for such trees simply means no bee nest spawns on them.
 */
@Mixin(BeehiveDecorator.class)
public class BeehiveDecoratorMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void serverfixtweaks$skipWhenNoLogs(TreeDecorator.Context context, CallbackInfo ci) {
        if (context.logs().isEmpty() && ServerFixTweaksConfig.fixBeehiveDecoratorCrash()) {
            ServerFixTweaks.LOGGER.debug("Skipping beehive tree decorator: tree generated no logs (would crash vanilla)");
            ci.cancel();
        }
    }
}

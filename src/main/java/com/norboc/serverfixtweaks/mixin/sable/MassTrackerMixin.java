package com.norboc.serverfixtweaks.mixin.sable;

import com.norboc.serverfixtweaks.ServerFixTweaks;
import com.norboc.serverfixtweaks.config.ServerFixTweaksConfig;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Part of the fix for <a href="https://github.com/ryanhcode/sable/issues/1315">ryanhcode/sable#1315</a>.
 *
 * <p>Sable's Create compat passes its (nullable) local bounding box straight into
 * {@link MassTracker#build}, guarded only by a disabled {@code assert}. A contraption whose
 * blocks are all air therefore crashes with {@code Cannot invoke "BoundingBox3ic.minX()"
 * because "bounds" is null}. Returning an empty tracker instead mirrors what {@code build}
 * itself does when it finds zero solid blocks, so Sable's empty-contraption handling in
 * {@code sable$contraptionInitialize} quits early exactly as it would for a tracker with no
 * centre of mass.
 *
 * <p>The bounds parameter is declared as {@code Object} (via {@link Coerce}) because its real
 * type, {@code dev.ryanhcode.sable.companion.math.BoundingBox3ic}, lives in Sable's jar-in-jar
 * companion library and is not available at compile time.
 */
@Mixin(value = MassTracker.class, remap = false)
public class MassTrackerMixin {
    @Inject(
            method = "build(Lnet/minecraft/world/level/BlockGetter;Ldev/ryanhcode/sable/companion/math/BoundingBox3ic;)Ldev/ryanhcode/sable/api/physics/mass/MassTracker;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void serverfixtweaks$emptyTrackerForNullBounds(BlockGetter blockGetter, @Coerce Object bounds, CallbackInfoReturnable<MassTracker> cir) {
        if (bounds == null && ServerFixTweaksConfig.fixSableEmptyContraptionCrash()) {
            ServerFixTweaks.LOGGER.debug("MassTracker.build called with null bounds (structure has no blocks); returning empty tracker");
            cir.setReturnValue(new MassTracker());
        }
    }
}

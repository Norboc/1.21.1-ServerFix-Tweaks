package com.norboc.serverfixtweaks.mixin.create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.norboc.serverfixtweaks.config.ServerFixTweaksConfig;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fixes <a href="https://github.com/Creators-of-Create/Create/issues/10218">Create#10218</a>
 * (duplicate: <a href="https://github.com/Creators-of-Create/Create/issues/10479">Create#10479</a>):
 * {@code NullPointerException: Cannot read field "x" because "mf.axis" is null} in
 * {@code ContinuousOBBCollider.collideMany}, crash-looping worlds with e.g. drill-on-pulley
 * or bearing tree-farm contraptions.
 *
 * <p>{@code ContinuousSeparationManifold.separate()} only assigns {@code axis} /
 * {@code normalAxis} when the projected centre distance on that axis is non-zero. When an
 * entity's OBB centre coincides exactly with a collider's centre on all axes (typical for
 * item drops spawned at block centres inside a contraption's swept volume), a discrete
 * collision is reported but {@code axis} stays null and the response maths dereferences it.
 * {@code normalAxis} has a second, stricter guard (A-axes only), so it can be null even when
 * {@code axis} is not.
 *
 * <p>Falling back to {@link Vec3#ZERO} makes such degenerate colliders contribute a zero
 * response/normal — there is no meaningful separation direction to push along anyway — and
 * {@code ContraptionCollider.collideEntities} already treats zero vectors as "no collision
 * response" / "no normal". All non-degenerate collisions are untouched. (Upstream's pending
 * PR #10301 relaxes the assignment guards instead, which also changes behaviour for
 * partially-aligned collisions; this fix deliberately stays narrower.)
 */
@Mixin(targets = "com.simibubi.create.foundation.collision.ContinuousOBBCollider", remap = false)
public class ContinuousOBBColliderMixin {
    @ModifyExpressionValue(
            method = "collideMany",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/simibubi/create/foundation/collision/ContinuousOBBCollider$ContinuousSeparationManifold;axis:Lnet/minecraft/world/phys/Vec3;",
                    opcode = org.objectweb.asm.Opcodes.GETFIELD
            )
    )
    private static Vec3 serverfixtweaks$nullSafeAxis(Vec3 axis) {
        if (axis == null && ServerFixTweaksConfig.fixCreateCollisionNullAxisCrash()) {
            return Vec3.ZERO;
        }
        return axis;
    }

    @ModifyExpressionValue(
            method = "collideMany",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/simibubi/create/foundation/collision/ContinuousOBBCollider$ContinuousSeparationManifold;normalAxis:Lnet/minecraft/world/phys/Vec3;",
                    opcode = org.objectweb.asm.Opcodes.GETFIELD
            )
    )
    private static Vec3 serverfixtweaks$nullSafeNormalAxis(Vec3 normalAxis) {
        if (normalAxis == null && ServerFixTweaksConfig.fixCreateCollisionNullAxisCrash()) {
            return Vec3.ZERO;
        }
        return normalAxis;
    }
}

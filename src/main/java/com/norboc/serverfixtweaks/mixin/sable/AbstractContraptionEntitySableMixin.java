package com.norboc.serverfixtweaks.mixin.sable;

import com.norboc.serverfixtweaks.ServerFixTweaks;
import com.norboc.serverfixtweaks.config.ServerFixTweaksConfig;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Part of the fix for <a href="https://github.com/ryanhcode/sable/issues/1315">ryanhcode/sable#1315</a>.
 *
 * <p>Sable's {@code AbstractContraptionEntityMixin} adds {@code sable$buildProperties()} to
 * Create's {@code AbstractContraptionEntity}. That method dereferences
 * {@code MassTracker.getCenterOfMass()} without a null check, but the centre of mass is
 * intentionally {@code null} for a contraption with no solid blocks. The result is a
 * {@code NullPointerException} on the contraption's first tick — a server crash-loop that
 * makes the world unloadable. Cancelling just before the dereference lets Sable's own
 * empty-contraption check in {@code sable$contraptionInitialize()} quit early instead,
 * which is exactly the path Sable already takes for a null mass tracker.
 *
 * <p>This mixin targets members added by Sable's mixin, so it must apply after it:
 * priority 2000 &gt; Sable's default 1000. {@code ServerFixTweaksMixinPlugin} only applies
 * it when both Sable and Create are installed. The target class is referenced by name
 * because compiling against Create would pull in its whole dependency chain for no gain.
 */
@Mixin(targets = "com.simibubi.create.content.contraptions.AbstractContraptionEntity", priority = 2000, remap = false)
public abstract class AbstractContraptionEntitySableMixin {
    @Dynamic("Field added to AbstractContraptionEntity by sable's AbstractContraptionEntityMixin")
    @Shadow(remap = false)
    private MassTracker sable$massTracker;

    @Dynamic("sable$buildProperties() is added to AbstractContraptionEntity by sable's AbstractContraptionEntityMixin")
    @Inject(
            method = "sable$buildProperties()V",
            at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/api/physics/mass/MassTracker;getCenterOfMass()Lorg/joml/Vector3dc;", remap = false),
            cancellable = true,
            remap = false
    )
    private void serverfixtweaks$skipEmptyContraption(CallbackInfo ci) {
        if (!ServerFixTweaksConfig.fixSableEmptyContraptionCrash()) {
            return;
        }
        if (this.sable$massTracker == null || this.sable$massTracker.getCenterOfMass() == null) {
            ServerFixTweaks.LOGGER.debug("Contraption has no solid blocks; skipping sable physics property build (would crash sable)");
            ci.cancel();
        }
    }
}

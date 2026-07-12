package com.norboc.serverfixtweaks.mixin.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.norboc.serverfixtweaks.config.ServerFixTweaksConfig;
import it.unimi.dsi.fastutil.ints.Int2BooleanFunction;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fixes <a href="https://github.com/ryanhcode/sable/issues/1292">ryanhcode/sable#1292</a>
 * (older reports: #788, #1223): {@code ArrayIndexOutOfBoundsException: Index -1 out of
 * bounds} in {@code Int2BooleanOpenHashMap.rehash}, crash-looping worlds as soon as an
 * affected block updates again.
 *
 * <p>{@code VoxelNeighborhoodState}'s {@code IS_SOLID_MEMOIZED} and {@code IS_FULL_BLOCK}
 * memoizers (anonymous classes {@code $1} and {@code $2}) each cache results in a plain
 * {@code Int2BooleanOpenHashMap} shared process-wide via a static field. That is unsafe in
 * two independent ways, and both corrupt the map's size bookkeeping — silently, until a
 * later rehash walks off the end of its arrays, after which the map throws on every miss
 * and the world becomes unloadable:
 *
 * <ul>
 * <li><b>Cross-thread races:</b> every block change Sable observes funnels into
 * {@code computeIfAbsent} — from the server thread, and in single player from the client
 * (render) thread handling the client-side copy of the same block change.</li>
 * <li><b>Same-thread reentrancy:</b> fastutil's {@code computeIfAbsent} computes the
 * insertion slot with {@code find(k)} <i>before</i> invoking the mapping callback and
 * inserts at that slot afterwards (Int2BooleanOpenHashMap.java:451-456). The callback here
 * evaluates {@code BlockState.getCollisionShape}, which can run arbitrary (modded) block
 * code; anything on that path that reaches another block change — e.g. Sable's own
 * {@code LevelAccelerator} synchronously generating a neighbouring chunk, whose
 * post-processing calls {@code setBlock} back into {@code handleBlockChange} →
 * {@code isSolid} — re-enters the same map and invalidates the outer call's slot. The
 * outer insert then overwrites a live entry while still incrementing {@code size}.
 * A {@code synchronized} block alone cannot prevent this: Java monitors are reentrant,
 * so the corrupting nested call happily proceeds on the lock-owning thread (verified in
 * the field, 2026-07-11 crash with the synchronized-only fix applied).</li>
 * </ul>
 *
 * <p>The wrapper therefore reimplements {@code computeIfAbsent}'s check-compute-store
 * contract with the callback hoisted <i>outside</i> any map operation: a synchronized
 * lookup, then the (possibly reentrant) computation with no map state on the stack, then a
 * synchronized {@code put} that does its own fresh slot lookup. Nested calls complete their
 * whole lookup-compute-put sequence before the outer put runs, and cross-thread access is
 * serialized on the cache itself — the private map has no other access point. Redundant
 * concurrent computations of the same key are possible and harmless (the function is a pure
 * per-BlockState predicate).
 */
@Mixin(
        targets = {
                "dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState$1",
                "dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState$2"
        },
        remap = false
)
public class VoxelNeighborhoodStateMemoizerMixin {
    @WrapOperation(
            method = "apply(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/level/block/state/BlockState;)Ljava/lang/Boolean;",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/ints/Int2BooleanOpenHashMap;computeIfAbsent(ILit/unimi/dsi/fastutil/ints/Int2BooleanFunction;)Z"
            )
    )
    private boolean serverfixtweaks$reentrantSafeComputeIfAbsent(
            Int2BooleanOpenHashMap cache, int key, Int2BooleanFunction mappingFunction, Operation<Boolean> original) {
        if (!ServerFixTweaksConfig.fixSableVoxelCacheRaceCrash()) {
            return original.call(cache, key, mappingFunction);
        }
        synchronized (cache) {
            if (cache.containsKey(key)) {
                return cache.get(key);
            }
        }
        // Faithful to fastutil's computeIfAbsent contract; always true for Sable's lambda.
        if (!mappingFunction.containsKey(key)) {
            return cache.defaultReturnValue();
        }
        // May re-enter this method (see class javadoc) — must run with no map operation
        // on the stack and outside the lock.
        boolean newValue = mappingFunction.get(key);
        synchronized (cache) {
            cache.put(key, newValue);
        }
        return newValue;
    }
}

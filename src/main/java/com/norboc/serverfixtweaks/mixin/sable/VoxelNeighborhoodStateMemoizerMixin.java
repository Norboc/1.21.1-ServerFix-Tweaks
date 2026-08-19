package com.norboc.serverfixtweaks.mixin.sable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.norboc.serverfixtweaks.config.ServerFixTweaksConfig;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Fixes <a href="https://github.com/ryanhcode/sable/issues/1292">ryanhcode/sable#1292</a>
 * (older reports: <a href="https://github.com/ryanhcode/sable/issues/788">#788</a>,
 * <a href="https://github.com/ryanhcode/sable/issues/1223">#1223</a>):
 * {@code ArrayIndexOutOfBoundsException: Index -1 out of bounds} thrown from the cache's
 * {@code rehash}, crash-looping worlds as soon as an affected block updates again.
 *
 * <p>{@code VoxelNeighborhoodState}'s {@code IS_SOLID_MEMOIZED} and {@code IS_FULL_BLOCK}
 * memoizers (anonymous {@code BiFunction}s {@code $1} and {@code $2}) each memoize
 * per-{@code BlockState} solidity in a plain fastutil map. Both memoizer instances are
 * held in {@code private static final} fields of the enum, so each map is process-wide and
 * shared by every thread that reaches {@code isSolid}/{@code isFullBlock}.
 *
 * <h2>What Sable 2.0.4 already fixed</h2>
 *
 * <p>Sable 2.0.3 used {@code Int2BooleanOpenHashMap.computeIfAbsent(state.hashCode(), fn)},
 * which was unsafe in two independent ways. The second one is gone as of 2.0.4: the
 * memoizers now hold a {@code Reference2BooleanMap<BlockState>} and do the work by hand —
 * {@code containsKey} &rarr; {@code getBoolean}, else compute, then {@code put} — with the
 * value computed <i>before</i> the {@code put}. That removes the reentrancy hazard.
 * fastutil's {@code computeIfAbsent} picked the insertion slot with {@code find(k)}
 * <i>before</i> running the mapping callback and inserted at that slot afterwards; the
 * callback evaluates {@code BlockState.getCollisionShape}, i.e. arbitrary modded block
 * code, which can reach a nested block change and re-enter the same map, invalidating the
 * outer call's slot. Every {@code put} in the rewritten {@code apply} does its own fresh
 * slot lookup, so a nested call that inserts the same key merely means the outer call
 * overwrites its own entry with an identical value — the map's {@code size} bookkeeping
 * stays correct.
 *
 * <h2>What is still broken</h2>
 *
 * <p>The cross-thread race. Nothing about 2.0.4/2.0.5 made these maps thread-safe: still a
 * process-wide static, still a plain unsynchronized {@code Reference2BooleanOpenHashMap},
 * still written from the server thread and — in single player — from the client thread
 * handling the client-side copy of the same block change. Two interleaved {@code put}s
 * corrupt the map's size and slot bookkeeping silently, and the damage surfaces later as
 * {@code ArrayIndexOutOfBoundsException: Index -1} inside {@code rehash}, after which the
 * map throws on every miss and the world becomes unloadable. Splitting
 * {@code computeIfAbsent} into separate {@code containsKey}/{@code put} calls, if anything,
 * widens the window between the check and the insert.
 *
 * <h2>Why a plain lock suffices now</h2>
 *
 * <p>Against 2.0.3, wrapping the map access in {@code synchronized} was <i>not</i> enough,
 * and was field-tested to still crash: Java monitors are reentrant, so the nested
 * {@code getCollisionShape}-triggered call re-acquired the monitor on the same thread and
 * corrupted the map from inside the critical section. The fix there had to hoist the
 * computation out of the map operation entirely.
 *
 * <p>Against 2.0.4+ that hazard is gone, so serializing whole {@code apply} calls on the
 * memoizer instance is sufficient: a reentrant nested call runs its full
 * lookup-compute-put sequence to completion before the outer call resumes, and the outer
 * {@code put} then re-resolves its own slot. Cross-thread callers are serialized. Locking
 * on {@code this} (the memoizer singleton) is safe — the {@code cache} field is
 * {@code private final} to each anonymous class, is never leaked, and only these two
 * classes plus the enum that constructs them reference the memoizers at all, so nothing
 * else in Sable can hold or contend for this monitor. {@code $1} and {@code $2} lock
 * independently because they own separate maps.
 *
 * <p>Tradeoff: the monitor is held across {@code getCollisionShape}, so solidity
 * computations no longer run concurrently. That is a cache-miss-only path and the results
 * are memoized, so the cost is small next to a crash-looping world.
 */
@Mixin(
        targets = {
                "dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState$1",
                "dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState$2"
        },
        remap = false
)
public class VoxelNeighborhoodStateMemoizerMixin {
    @WrapMethod(
            method = "apply(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/level/block/state/BlockState;)Ljava/lang/Boolean;"
    )
    private Boolean serverfixtweaks$synchronizeCacheAccess(
            BlockGetter level, BlockState state, Operation<Boolean> original) {
        if (!ServerFixTweaksConfig.fixSableVoxelCacheRaceCrash()) {
            return original.call(level, state);
        }
        synchronized (this) {
            return original.call(level, state);
        }
    }
}

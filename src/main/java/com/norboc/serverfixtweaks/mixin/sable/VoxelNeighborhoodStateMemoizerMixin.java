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
 * {@code Int2BooleanOpenHashMap} shared process-wide via a static field. Every block change
 * Sable observes funnels into {@code computeIfAbsent} on those maps — from the server thread,
 * and in single player from the client (render) thread handling the client-side copy of the
 * same block change. Unsynchronized concurrent inserts corrupt the map's internal size
 * bookkeeping; the corruption is silent until a rehash walks off the end of its arrays, after
 * which the map throws on every miss and the world becomes unloadable.
 *
 * <p>Wrapping the {@code computeIfAbsent} call in a {@code synchronized} block on the cache
 * itself makes every access to each cache mutually exclusive — the private map is touched
 * nowhere else. The critical section is a hash lookup (plus, on a miss, a cheap collision
 * shape query), so contention is negligible. Upstream's rewrite on main still lacks any
 * synchronization, so this stays needed on 2.0.3 and is why it is reported here rather than
 * as a patch upstream would take verbatim.
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
    private boolean serverfixtweaks$synchronizedComputeIfAbsent(
            Int2BooleanOpenHashMap cache, int key, Int2BooleanFunction mappingFunction, Operation<Boolean> original) {
        if (!ServerFixTweaksConfig.fixSableVoxelCacheRaceCrash()) {
            return original.call(cache, key, mappingFunction);
        }
        synchronized (cache) {
            return original.call(cache, key, mappingFunction);
        }
    }
}

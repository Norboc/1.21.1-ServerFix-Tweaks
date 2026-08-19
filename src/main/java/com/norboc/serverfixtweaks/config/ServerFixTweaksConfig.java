package com.norboc.serverfixtweaks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for ServerFix Tweaks. Every fix gets its own toggle under the
 * {@code fixes} section so individual fixes can be disabled without removing the mod.
 */
public final class ServerFixTweaksConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue FIX_BEEHIVE_DECORATOR_CRASH;
    private static final ModConfigSpec.BooleanValue FIX_SABLE_EMPTY_CONTRAPTION_CRASH;
    private static final ModConfigSpec.BooleanValue FIX_CREATE_COLLISION_NULL_AXIS_CRASH;
    private static final ModConfigSpec.BooleanValue FIX_SABLE_VOXEL_CACHE_RACE_CRASH;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("fixes");

        FIX_BEEHIVE_DECORATOR_CRASH = builder
                .comment(
                        "Prevents an IndexOutOfBoundsException crash during world generation when a tree",
                        "feature uses the vanilla beehive tree decorator but records no log blocks.",
                        "Vanilla BeehiveDecorator.place() reads logs.get(0) without an empty check, so",
                        "modded trees without logs crash the game. When enabled, the decorator is",
                        "skipped for such trees and no bee nest spawns on them.")
                .define("fixBeehiveDecoratorCrash", true);

        FIX_SABLE_EMPTY_CONTRAPTION_CRASH = builder
                .comment(
                        "DEPRECATED - NO-OP. This setting no longer does anything, whatever it is",
                        "set to. It used to guard against a NullPointerException crash-loop when a",
                        "Create contraption containing no solid blocks was assembled with Sable",
                        "installed (https://github.com/ryanhcode/sable/issues/1315). Sable fixed",
                        "that itself in 2.0.4/2.0.5 (with a wider guard than this mod's), so the",
                        "mixins behind this toggle have been removed. The key is kept only so that",
                        "existing config files stay valid; it can be deleted from your config.")
                .define("fixSableEmptyContraptionCrash", true);

        FIX_CREATE_COLLISION_NULL_AXIS_CRASH = builder
                .comment(
                        "Prevents a NullPointerException crash-loop in Create's contraption collision",
                        "maths (https://github.com/Creators-of-Create/Create/issues/10218,",
                        "duplicate #10479). When an entity's collision box centre coincides exactly",
                        "with a contraption collider's centre (typical for item drops spawned at",
                        "block centres by drills on pulleys, bearing tree farms, ...), Create's",
                        "separation manifold never assigns a separation axis but still reports a",
                        "collision, then dereferences the null axis. When enabled, such degenerate",
                        "colliders contribute a zero collision response instead of crashing.",
                        "Only takes effect when Create is installed.")
                .define("fixCreateCollisionNullAxisCrash", true);

        FIX_SABLE_VOXEL_CACHE_RACE_CRASH = builder
                .comment(
                        "Prevents an ArrayIndexOutOfBoundsException crash-loop in Sable's block",
                        "solidity cache (https://github.com/ryanhcode/sable/issues/1292). Sable",
                        "memoizes per-BlockState solidity in plain, unsynchronized hash maps held",
                        "in static fields and shared by every thread - the server thread and, in",
                        "single player, the client thread handling the same block change. Two",
                        "interleaved writes silently corrupt the map until a later rehash throws,",
                        "after which every block update crashes and the world becomes unloadable.",
                        "When enabled, the memoizer lookups are serialized on the memoizer itself.",
                        "Only takes effect when Sable is installed.")
                .define("fixSableVoxelCacheRaceCrash", true);

        builder.pop();

        SPEC = builder.build();
    }

    private ServerFixTweaksConfig() {
    }

    public static boolean fixBeehiveDecoratorCrash() {
        // Fail safe: if worldgen somehow runs before the config loads, keep the fix active.
        return !SPEC.isLoaded() || FIX_BEEHIVE_DECORATOR_CRASH.get();
    }

    /**
     * @deprecated no-op. Sable fixed <a href="https://github.com/ryanhcode/sable/issues/1315">#1315</a>
     *         itself in 2.0.4/2.0.5, and the mixins that read this toggle were removed. The
     *         config key is retained so existing config files keep validating.
     */
    @Deprecated
    public static boolean fixSableEmptyContraptionCrash() {
        return !SPEC.isLoaded() || FIX_SABLE_EMPTY_CONTRAPTION_CRASH.get();
    }

    public static boolean fixCreateCollisionNullAxisCrash() {
        // Fail safe: if collisions somehow run before the config loads, keep the fix active.
        return !SPEC.isLoaded() || FIX_CREATE_COLLISION_NULL_AXIS_CRASH.get();
    }

    public static boolean fixSableVoxelCacheRaceCrash() {
        // Fail safe: if a block update somehow runs before the config loads, keep the fix active.
        return !SPEC.isLoaded() || FIX_SABLE_VOXEL_CACHE_RACE_CRASH.get();
    }
}

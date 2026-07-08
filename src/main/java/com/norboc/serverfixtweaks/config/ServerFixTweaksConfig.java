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
                        "Prevents a NullPointerException crash-loop when a Create contraption that",
                        "contains no solid blocks is assembled with Sable installed",
                        "(https://github.com/ryanhcode/sable/issues/1315). Sable builds physics",
                        "properties for the contraption without null-checking its bounding box or",
                        "centre of mass, crashing the server on the contraption's first tick and",
                        "making the world unloadable. When enabled, such contraptions skip Sable's",
                        "physics setup via Sable's own empty-contraption path. Only takes effect",
                        "when Sable and Create are installed.")
                .define("fixSableEmptyContraptionCrash", true);

        builder.pop();

        SPEC = builder.build();
    }

    private ServerFixTweaksConfig() {
    }

    public static boolean fixBeehiveDecoratorCrash() {
        // Fail safe: if worldgen somehow runs before the config loads, keep the fix active.
        return !SPEC.isLoaded() || FIX_BEEHIVE_DECORATOR_CRASH.get();
    }

    public static boolean fixSableEmptyContraptionCrash() {
        // Fail safe: if a contraption somehow ticks before the config loads, keep the fix active.
        return !SPEC.isLoaded() || FIX_SABLE_EMPTY_CONTRAPTION_CRASH.get();
    }
}

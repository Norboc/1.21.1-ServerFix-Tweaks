package com.norboc.serverfixtweaks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for ServerFix Tweaks. Every fix gets its own toggle under the
 * {@code fixes} section so individual fixes can be disabled without removing the mod.
 */
public final class ServerFixTweaksConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue FIX_BEEHIVE_DECORATOR_CRASH;

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

        builder.pop();

        SPEC = builder.build();
    }

    private ServerFixTweaksConfig() {
    }

    public static boolean fixBeehiveDecoratorCrash() {
        // Fail safe: if worldgen somehow runs before the config loads, keep the fix active.
        return !SPEC.isLoaded() || FIX_BEEHIVE_DECORATOR_CRASH.get();
    }
}

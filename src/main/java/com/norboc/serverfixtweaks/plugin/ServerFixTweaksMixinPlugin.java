package com.norboc.serverfixtweaks.plugin;

import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.MavenVersionAdapter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Gates mixins that target other mods' classes on those mods actually being installed, and
 * on Sable being a version this mod was written against.
 *
 * <p>Mixins under {@code com.norboc.serverfixtweaks.mixin.sable} patch Sable internals that
 * carry no compatibility promise — Sable has already reshaped them once (the
 * {@code VoxelNeighborhoodState} memoizers were rewritten in 2.0.4, invalidating the
 * injection point this mod used against 2.0.3). With {@code defaultRequire: 1}, a mixin
 * whose target has moved is a hard startup crash, and a per-fix config toggle cannot help
 * because the toggle is read inside the handler, long after injection failed. So the
 * package is skipped outside {@link #SABLE_SUPPORTED_RANGE}: a future Sable refactor then
 * costs a log line and an unfixed bug instead of an unbootable game.
 *
 * <p>Mixins under {@code com.norboc.serverfixtweaks.mixin.create} patch Create itself and
 * only require Create.
 */
public final class ServerFixTweaksMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("serverfixtweaks");

    private static final String SABLE_MIXIN_PACKAGE = "com.norboc.serverfixtweaks.mixin.sable.";
    private static final String CREATE_MIXIN_PACKAGE = "com.norboc.serverfixtweaks.mixin.create.";
    private static final String CREATE_CLASS_PREFIX = "com.simibubi.create.";

    private static final String SABLE_MOD_ID = "sable";

    /**
     * Sable versions the {@code mixin.sable} package is known to inject into cleanly.
     * 2.0.4 is the floor because that is where the memoizer rewrite landed; 2.1.0 is an
     * assumed-breaking ceiling. Raise the ceiling once a newer version is verified.
     */
    private static final String SABLE_SUPPORTED_RANGE = "[2.0.4,2.1.0)";

    /** Memoized so the version is parsed, and any warning logged, exactly once. */
    private static Boolean sableVersionSupported;

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(SABLE_MIXIN_PACKAGE)) {
            if (!isModLoaded(SABLE_MOD_ID)) {
                return false;
            }
            if (!isSableVersionSupported()) {
                return false;
            }
            if (targetClassName.startsWith(CREATE_CLASS_PREFIX) && !isModLoaded("create")) {
                return false;
            }
        }
        if (mixinClassName.startsWith(CREATE_MIXIN_PACKAGE)) {
            return isModLoaded("create");
        }
        return true;
    }

    private static boolean isModLoaded(String modId) {
        return getModFile(modId) != null;
    }

    private static ModFileInfo getModFile(String modId) {
        // ModList is not populated yet while mixin configs are processed; the loading list is.
        LoadingModList loadingModList = LoadingModList.get();
        return loadingModList == null ? null : loadingModList.getModFileById(modId);
    }

    private static synchronized boolean isSableVersionSupported() {
        if (sableVersionSupported == null) {
            sableVersionSupported = checkSableVersion();
        }
        return sableVersionSupported;
    }

    /**
     * @return whether Sable's version falls in {@link #SABLE_SUPPORTED_RANGE}. Anything that
     *         cannot be read or parsed counts as supported: an unknown version is not
     *         evidence of an incompatible one, and skipping the fixes on a hunch would be
     *         worse than letting them apply.
     */
    private static boolean checkSableVersion() {
        String rawVersion;
        try {
            ModFileInfo sable = getModFile(SABLE_MOD_ID);
            rawVersion = sable == null ? null : sable.versionString();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not read Sable's version; applying the Sable fixes anyway", e);
            return true;
        }
        if (rawVersion == null || rawVersion.isBlank()) {
            LOGGER.warn("Sable reports no version; applying the Sable fixes anyway");
            return true;
        }

        ArtifactVersion version = parseVersion(rawVersion);
        if (version == null) {
            LOGGER.warn(
                    "Could not parse Sable's version '{}'; applying the Sable fixes anyway",
                    rawVersion);
            return true;
        }

        VersionRange supported;
        try {
            supported = MavenVersionAdapter.createFromVersionSpec(SABLE_SUPPORTED_RANGE);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not parse the supported Sable range '{}'; applying the Sable fixes anyway",
                    SABLE_SUPPORTED_RANGE, e);
            return true;
        }

        if (!supported.containsVersion(version)) {
            LOGGER.warn(
                    "Sable {} is outside the range {} that ServerFix Tweaks' Sable fixes were written"
                            + " against; skipping them. The bugs they fix may be present, or may have been"
                            + " fixed upstream. Check for a ServerFix Tweaks update.",
                    rawVersion, SABLE_SUPPORTED_RANGE);
            return false;
        }
        return true;
    }

    /**
     * Parses a mod version leniently. Semver build metadata is dropped first, so Modrinth-style
     * {@code 2.0.5+mc1.21.1} compares as {@code 2.0.5}. Returns {@code null} when Maven could
     * make nothing of the string — which it signals by stuffing the whole thing into the
     * qualifier rather than by throwing, and which would otherwise compare as version 0.
     */
    private static ArtifactVersion parseVersion(String rawVersion) {
        int buildMetadata = rawVersion.indexOf('+');
        String numeric = buildMetadata < 0 ? rawVersion : rawVersion.substring(0, buildMetadata);
        if (numeric.isBlank()) {
            return null;
        }
        ArtifactVersion version = new DefaultArtifactVersion(numeric);
        return numeric.equals(version.getQualifier()) ? null : version;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}

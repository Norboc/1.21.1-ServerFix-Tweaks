package com.norboc.serverfixtweaks.plugin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Gates mixins that target other mods' classes on those mods actually being installed.
 *
 * <p>Mixins under {@code com.norboc.serverfixtweaks.mixin.sable} patch Sable (and, for the
 * Create-compat crash, members Sable's own mixin adds to Create's contraption entity). They
 * must not be applied when Sable is absent — Create's {@code AbstractContraptionEntity}
 * would still load, and the shadowed {@code sable$}-members would not exist.
 */
public final class ServerFixTweaksMixinPlugin implements IMixinConfigPlugin {
    private static final String SABLE_MIXIN_PACKAGE = "com.norboc.serverfixtweaks.mixin.sable.";
    private static final String CREATE_CLASS_PREFIX = "com.simibubi.create.";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(SABLE_MIXIN_PACKAGE)) {
            if (!isModLoaded("sable")) {
                return false;
            }
            if (targetClassName.startsWith(CREATE_CLASS_PREFIX) && !isModLoaded("create")) {
                return false;
            }
        }
        return true;
    }

    private static boolean isModLoaded(String modId) {
        // ModList is not populated yet while mixin configs are processed; the loading list is.
        LoadingModList loadingModList = LoadingModList.get();
        return loadingModList != null && loadingModList.getModFileById(modId) != null;
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

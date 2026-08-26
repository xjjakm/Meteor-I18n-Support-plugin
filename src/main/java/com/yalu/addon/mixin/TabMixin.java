package com.yalu.addon.mixin;

import com.yalu.addon.util.NameCache;
import com.yalu.addon.util.TransUtil;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.yalu.addon.TranslateAddon.MC;
import static com.yalu.addon.TranslateAddon.TRANSLATOR;

@Mixin(value = Tab.class, remap = false)
public abstract class TabMixin {
    @Mutable
    @Shadow
    @Final
    public String name;

    @Inject(method = "<init>*", at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        String originalName = NameCache.tab((Tab) (Object) this);
        if (MC == null || MC.getResourceManager() == null) return;
        TRANSLATOR.reload(MC.getResourceManager());
        String key = "Tab.Meteor." + TransUtil.baseFormat(originalName);
        String translated = TRANSLATOR.Translate(key, originalName);
        if (!translated.equals(originalName)) {
            ((TabAccessor) this).setName(translated);
        }
    }
}

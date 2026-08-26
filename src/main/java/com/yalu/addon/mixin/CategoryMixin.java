package com.yalu.addon.mixin;

import com.yalu.addon.util.NameCache;
import com.yalu.addon.util.TransUtil;
import meteordevelopment.meteorclient.systems.modules.Category;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.yalu.addon.TranslateAddon.MC;
import static com.yalu.addon.TranslateAddon.TRANSLATOR;

@Mixin(value = Category.class, remap = false)
public abstract class CategoryMixin {
    @Mutable
    @Shadow
    @Final
    public String name;

    @Inject(method = "<init>*", at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        String originalName = NameCache.category((Category) (Object) this);
        if (MC == null || MC.getResourceManager() == null) return;
        TRANSLATOR.reload(MC.getResourceManager());
        String key = "Category.Meteor." + TransUtil.baseFormat(originalName);
        String translated = TRANSLATOR.Translate(key, originalName);
        if (!translated.equals(originalName)) {
            ((CategoryAccessor) this).setName(translated);
        }
    }
}

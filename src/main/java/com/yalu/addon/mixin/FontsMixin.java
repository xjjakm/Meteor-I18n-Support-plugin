package com.yalu.addon.mixin;


import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.utils.render.FontUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = Fonts.class , remap = false)
public class FontsMixin {
    @Shadow
    @Final
    public static List<FontFamily> FONT_FAMILIES;

    @Inject(method = "refresh",at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"))
    private static void refresh(CallbackInfo ci) {
        FontUtils.loadBuiltin(FONT_FAMILIES,"WenQuanWeiMiHei");
    }
}

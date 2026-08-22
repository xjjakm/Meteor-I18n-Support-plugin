package com.yalu.addon.mixin;


import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TextRenderer.class,remap = false)
public interface TextRendererMixin {
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private static void get(CallbackInfoReturnable<TextRenderer> cir){
        cir.setReturnValue(Fonts.RENDERER);
    }
}

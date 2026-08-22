package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.utils.render.color.Color;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer.class,remap = false)
public class VanillaTextRendererMixin {
    @Shadow
    public boolean scaleIndividually;

    @Inject(method = "end" , at = @At("RETURN"))
    public void endMixin(CallbackInfo ci){
        this.scaleIndividually = true;
    }
}

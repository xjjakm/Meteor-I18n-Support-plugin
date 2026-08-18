package com.yalu.addon.mixin;

import com.yalu.addon.util.TextReplacement;
import meteordevelopment.meteorclient.gui.widgets.WMultiLabel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = WMultiLabel.class, remap = false)
public class WMultiLabelMixin {

@ModifyVariable(method = "<init>(Ljava/lang/String;ZD)V", at = @At("HEAD"), argsOnly = true)
private static String onInit(String text) {
return TextReplacement.replace(text);
}
}
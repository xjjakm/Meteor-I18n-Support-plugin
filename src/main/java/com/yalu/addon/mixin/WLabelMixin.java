package com.yalu.addon.mixin;

import com.yalu.addon.util.TextReplacement;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = WLabel.class, remap = false)
public class WLabelMixin {

@ModifyVariable(method = "<init>(Ljava/lang/String;Z)V", at = @At("HEAD"), argsOnly = true)
private static String onInit(String text) {
return TextReplacement.replace(text);
}

@ModifyVariable(method = "set(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
private String onSet(String text) {
return TextReplacement.replace(text);
}
}
package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static com.yalu.addon.TranslateAddon.gui;

@Mixin(value = WTextBox.class, remap = false)
public class WTextBoxMixin {

@ModifyVariable(method = "<init>(Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/gui/utils/CharFilter;Ljava/lang/Class;)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
private static String onInitPlaceholder(String placeholder) {
return gui(placeholder);
}
}
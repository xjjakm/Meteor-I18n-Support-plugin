package com.yalu.addon.mixin;

import com.yalu.addon.util.TextReplacement;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = WWindow.class, remap = false)
public class WWindowMixin {

@ModifyVariable(method = "<init>(Lmeteordevelopment/meteorclient/gui/widgets/WWidget;Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
private static String onInit(String title) {
return TextReplacement.replace(title);
}
}

package com.yalu.addon.mixin;

import com.yalu.addon.util.TextReplacement;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = WSection.class, remap = false)
public class WSectionMixin {

@ModifyVariable(method = "<init>(Ljava/lang/String;ZLmeteordevelopment/meteorclient/gui/widgets/WWidget;)V", at = @At("HEAD"), argsOnly = true)
private static String onInit(String title) {
return TextReplacement.replace(title);
}
}

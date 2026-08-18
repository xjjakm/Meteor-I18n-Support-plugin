package com.yalu.addon.mixin;

import com.yalu.addon.util.TextReplacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
* Translate enum/object values rendered inside dropdown widgets.
* Both the collapsed value and the opened option list use WValue.toString().
*/
@Mixin(targets = "meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorDropdown$WValue", remap = false)
public class WMeteorDropdownValueMixin {

@Redirect(
method = "onRender(Lmeteordevelopment/meteorclient/gui/renderer/GuiRenderer;DDD)V",
at = @At(value = "INVOKE", target = "Ljava/lang/Object;toString()Ljava/lang/String;")
)
private String onValueToString(Object value) {
return TextReplacement.replace(value.toString());
}
}

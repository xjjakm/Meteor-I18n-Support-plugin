package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorDropdown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.yalu.addon.TranslateAddon.gui;

/**
* Translate the selected value text rendered in the dropdown header
* (the collapsed view showing the currently chosen option).
*/
@Mixin(value = WMeteorDropdown.class, remap = false)
public class WMeteorDropdownHeaderMixin {

@Redirect(
method = "onRender(Lmeteordevelopment/meteorclient/gui/renderer/GuiRenderer;DDD)V",
at = @At(value = "INVOKE", target = "Ljava/lang/Object;toString()Ljava/lang/String;")
)
private String onHeaderValueToString(Object value) {
return gui(value.toString());
}
}
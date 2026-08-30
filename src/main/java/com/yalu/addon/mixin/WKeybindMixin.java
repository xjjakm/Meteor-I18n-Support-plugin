package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.yalu.addon.TranslateAddon.recordKeybindText;

/**
 * WKeybind.refreshLabel() 通过 button.set(keybind.toString()) 显示按键绑定文本
 * （如 "RCONTROL"、"Ctrl + Right Control"、"None"）。这些是动态数据，由 vanilla key.*
 * 语言键处理，不应被翻译/记录进 lang.json。
 * 本 mixin 在 set 转发处登记该文本，TranslateAddon.gui() 即可据此跳过。
 */
@Mixin(value = WKeybind.class, remap = false)
public class WKeybindMixin {

    @Redirect(method = "refreshLabel()V", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/widgets/pressable/WButton;set(Ljava/lang/String;)V"))
    private void onSet(WButton button, String text) {
        recordKeybindText(text);
        button.set(text);
    }
}
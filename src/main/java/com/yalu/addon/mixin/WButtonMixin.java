package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static com.yalu.addon.TranslateAddon.gui;

/**
 * 修正按钮宽度：onCalculateSize 用原始英文文本计算 textWidth，
 * 导致渲染中文时居中偏移。这里在计算 textWidth 前先把文本翻成目标语言，
 * 使 width/textWidth 按译文宽度计算，配合渲染期的 @ModifyArg 达到正确居中。
 * 适用 WButton 及其子类（含 WConfirmedButton 的 confirmText）。
 */
@Mixin(value = WButton.class, remap = false)
public class WButtonMixin {

    @ModifyArg(
        method = "onCalculateSize()V",
        at = @At(value = "INVOKE",
            target = "Lmeteordevelopment/meteorclient/gui/GuiTheme;textWidth(Ljava/lang/String;)D"),
        index = 0
    )
    private String onCalcTextWidth(String text) {
        return gui(text);
    }
}

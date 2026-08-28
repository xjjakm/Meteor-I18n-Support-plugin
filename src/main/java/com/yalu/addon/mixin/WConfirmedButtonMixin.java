package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.yalu.addon.TranslateAddon.gui;

/**
 * 渲染确认按钮文本（含 confirmText）。
 * WMeteorConfirmedButton.onRender 通过 getText() 取文本（按下后返回 confirmText），
 * WButton.onCalculateSize 也经 getText() 计算翻译后的宽度，
 * 故在此统一在 getText() 返回值时翻译：文本与 confirmText 首次显示即为正确译文，
 * 且宽度按译文计算，无需点击。
 */
@Mixin(value = WConfirmedButton.class, remap = false)
public class WConfirmedButtonMixin {

    @Inject(method = "getText()Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void onGetTextReturn(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(gui(cir.getReturnValue()));
    }
}

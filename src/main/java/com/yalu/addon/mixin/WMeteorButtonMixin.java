package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.yalu.addon.TranslateAddon.gui;

/**
 * 渲染时翻译普通按钮文本。
 * WMeteorButton.onRender 直接读取 text 字段（不走 getText()），
 * 构造时若语言未就绪会存下英文、需点击才重建。这里在渲染读取 text 时翻译，
 * 与下拉框一致，保证无需交互即显示正确译文。
 * 宽度由 WButtonMixin 在 onCalculateSize 修正。
 */
@Mixin(value = WMeteorButton.class, remap = false)
public class WMeteorButtonMixin {

    @Redirect(
        method = "onRender(Lmeteordevelopment/meteorclient/gui/renderer/GuiRenderer;DDD)V",
        at = @At(value = "FIELD",
            target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/widgets/pressable/WMeteorButton;text:Ljava/lang/String;")
    )
    private String onRenderText(WMeteorButton self) {
        return gui(self.getText());
    }
}

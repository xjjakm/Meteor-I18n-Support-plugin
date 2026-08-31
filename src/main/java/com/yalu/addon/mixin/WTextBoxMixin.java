package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.yalu.addon.TranslateAddon.gui;
import static com.yalu.addon.TranslateAddon.recordUserTypedText;

/**
 * WTextBox 处理两处：
 * 1. 构造时的占位提示（placeholder）仍走 gui()，保留并翻译搜索框等提示（已记录 Gui.Meteor.* 键）；
 * 2. calculateTextWidths()：所有调用源（onCalculateSize 布局尺寸/runAction 输入/set 赋值）都会
 *    汇聚到这里，并对当前 text 调用 theme.textWidth(...)；Catppuccin 主题下该实现会经
 *    RichText.of(text) 再次送入 gui()。在这里登记，先于 textWidth 的 gui()，保证输入内容被跳过
 *    （不翻译成 Gui.Meteor.vanilla="原版"，也不写入 lang.json）。
 * 3. render() 兜底：渲染路径经 catppuccin GuiRendererMixin 也会把 text 送入 gui()，此处登记兜底
 *    （尺寸计算之外、由内部其它路径改动的文本）。
 */
@Mixin(value = WTextBox.class, remap = false)
public class WTextBoxMixin {

    @Shadow
    protected String text;

    @ModifyVariable(method = "<init>(Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/gui/utils/CharFilter;Ljava/lang/Class;)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static String onInitPlaceholder(String placeholder) {
        return gui(placeholder);
    }

    @Inject(method = "calculateTextWidths", at = @At("HEAD"))
    private void onCalculateTextWidths(CallbackInfo ci) {
        recordUserTypedText(text);
    }

    @Inject(method = "render(Lmeteordevelopment/meteorclient/gui/renderer/GuiRenderer;DDD)Z", at = @At("HEAD"))
    private void onRenderHead(GuiRenderer renderer, double mouseX, double mouseY, double delta, CallbackInfoReturnable<Boolean> cir) {
        recordUserTypedText(text);
    }
}
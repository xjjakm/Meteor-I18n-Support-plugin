package com.yalu.addon.mixin;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.yalu.addon.TranslateAddon.TRANSLATOR;
import static com.yalu.addon.TranslateAddon.guiTextKey;

/**
 * Meteor 的 JoinMultiplayerScreenMixin 用英文模板（"Logged in as " / "Using proxy " /
 * "Not using a proxy"）计算状态文本宽度，作为玩家名/代理信息文本的 x 偏移；而插件在
 * 渲染层已把这些模板翻译成中文（GuiGraphicsExtractorMixin），英文宽度与译文宽度不一致
 * 导致信息文本错位。这里在 Font.width(String) 入口按译文计算这三类模板的宽度，
 * 单点覆盖多人游戏界面两条状态行（登录行、代理行宽度均在此方法处计算）。
 */
@Mixin(value = Font.class, priority = 1100)
public abstract class FontWidthMixin {

    @Inject(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void onWidth(String text, CallbackInfoReturnable<Integer> cir) {
        String key = guiTextKey(text);
        if (key != null) {
            // 递归一次：译文不命中模板，走原版宽度计算
            cir.setReturnValue(((Font) (Object) this).width(TRANSLATOR.get(key, text)));
        }
    }
}
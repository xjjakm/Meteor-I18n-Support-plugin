package com.yalu.addon.mixin;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.yalu.addon.TranslateAddon.applyVanillaButtonMessage;

/**
 * 拦截原版 AbstractWidget#getMessage()：Meteor 通过 screen mixin 注入到原版界面的
 * 原版样式按钮（DisconnectedScreen 的 Reconnect / Toggle Auto Reconnect，
 * BookEditScreen / BookViewScreen 的 Copy / Paste，JoinMultiplayerScreen 的 Accounts / Proxies）
 * 在渲染与事件处理时都经此方法取文本。
 * 文本命中白名单（含 Reconnect (N.N) 动态后缀模板）时送入 mixinButton() 翻译
 * （mixinButton.Meteor.* 键）；白名单之外的组件——原版 translatable 按钮、
 * 第三方 mod（如 ViaFabricPlus）注入的按钮——原样返回，不受影响。
 */
@Mixin(value = AbstractWidget.class)
public class VanillaButtonMixin {

    @Inject(method = "getMessage", at = @At("RETURN"), cancellable = true)
    private void meteori18n$getMessage(CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        Component translated = applyVanillaButtonMessage(original);
        if (translated != original) cir.setReturnValue(translated);
    }
}

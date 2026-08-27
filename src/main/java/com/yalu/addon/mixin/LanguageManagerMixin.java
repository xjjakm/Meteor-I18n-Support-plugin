package com.yalu.addon.mixin;

import com.yalu.addon.util.LanguageRefresh;
import com.yalu.addon.util.UniversalLangLoader;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.resources.language.LanguageManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LanguageManager.class)
public class LanguageManagerMixin {

    @Inject(method = "setSelected", at = @At("TAIL"))
    private void onSetSelected(String code, CallbackInfo ci) {
        // 重新加载 TextReplacement 的 universal 翻译表
        UniversalLangLoader.reload();
        // 重建聊天前缀（ChatUtils.init 只在启动时调用一次，切换语言后需要重建）
        ChatUtils.init();
        // 重新翻译 Module.title / description、Setting、Category、Tab、以及
        // 不属于任何 Module 的独立 Settings（HUD、GUI 主题等）所有字段
        LanguageRefresh.applyAll();
        // 若当前正打开某个 Meteor WidgetScreen（如 GUI/HUD 编辑器），清除并重建其控件树，
        // 使构造时固化的文字随语言切换立即更新
        if (MeteorClient.mc.gui.screen() instanceof WidgetScreen widgetScreen) {
            widgetScreen.reload();
        }
    }
}

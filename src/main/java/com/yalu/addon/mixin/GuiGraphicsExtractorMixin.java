package com.yalu.addon.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static com.yalu.addon.TranslateAddon.TRANSLATOR;
import static com.yalu.addon.TranslateAddon.guiTextKey;

@Mixin(value = GuiGraphicsExtractor.class, priority = 1100)
public abstract class GuiGraphicsExtractorMixin {

    @ModifyArg(
        method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"),
        index = 1
    )
    private static String replaceTextString(String str) {
        // 仅对已映射到标准语言文件（meteori18n.*）的屏幕文本模板做翻译；
        // 其余文本原样保留（通用翻译表已移除）。
        String key = guiTextKey(str);
        if (key != null) return TRANSLATOR.get(key, str);
        return str;
    }

    @ModifyArg(
        method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/FormattedText;of(Ljava/lang/String;)Lnet/minecraft/network/chat/FormattedText;"),
        index = 0
    )
    private static String replaceTextStringShadow(String str) {
        String key = guiTextKey(str);
        if (key != null) return TRANSLATOR.get(key, str);
        return str;
    }
}

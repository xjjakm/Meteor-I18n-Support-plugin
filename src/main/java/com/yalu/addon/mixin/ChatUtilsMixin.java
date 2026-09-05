package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static com.yalu.addon.TranslateAddon.TRANSLATOR;
import static com.yalu.addon.TranslateAddon.chatTemplateKey;

@Mixin(value = ChatUtils.class, remap = false)
public class ChatUtilsMixin {

    @ModifyArg(
        method = "sendMsg(ILjava/lang/String;Lnet/minecraft/ChatFormatting;Lnet/minecraft/ChatFormatting;Ljava/lang/String;[Ljava/lang/Object;)V",
        at = @At(value = "INVOKE", target = "Ljava/lang/String;format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"),
        index = 0
    )
    private static String onSendMsgFormatted(String messageContent) {
        // 仅对已映射到标准语言文件（meteori18n.*）的聊天模板做翻译；
        // 其余模板原样保留（通用翻译表已移除）。
        String key = chatTemplateKey(messageContent);
        if (key != null) return TRANSLATOR.get(key, messageContent);
        return messageContent;
    }
}
package com.yalu.addon.mixin;

import com.yalu.addon.util.TextReplacement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = GuiGraphicsExtractor.class, priority = 1100)
public abstract class GuiGraphicsExtractorMixin {

    @ModifyArg(
        method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"),
        index = 1
    )
    private static String replaceTextString(String str) {
        return TextReplacement.replace(str);
    }

    @ModifyArg(
        method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/FormattedText;of(Ljava/lang/String;)Lnet/minecraft/network/chat/FormattedText;"),
        index = 0
    )
    private static String replaceTextStringShadow(String str) {
        return TextReplacement.replace(str);
    }
}

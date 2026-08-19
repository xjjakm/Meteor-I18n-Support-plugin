package com.yalu.addon.mixin;

import com.yalu.addon.util.TextReplacement;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JoinMultiplayerScreen.class, priority = 1100)
public abstract class JoinMultiplayerScreenTranslationMixin extends Screen {

    protected JoinMultiplayerScreenTranslationMixin(Component title) {
        super(title);
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void onRepositionElementsTail(CallbackInfo ci) {
        for (GuiEventListener child : this.children()) {
            if (child instanceof Button button) {
                String text = button.getMessage().getString();
                String translated = TextReplacement.replace(text);
                if (!text.equals(translated)) {
                    button.setMessage(Component.literal(translated));
                }
            }
        }
    }

    @ModifyArg(
        method = "extractRenderState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"),
        index = 1
    )
    private String onTextRender(String str) {
        return TextReplacement.replace(str);
    }
}

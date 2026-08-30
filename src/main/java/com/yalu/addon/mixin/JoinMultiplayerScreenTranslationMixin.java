package com.yalu.addon.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

import static com.yalu.addon.TranslateAddon.gui;

@Mixin(value = JoinMultiplayerScreen.class, priority = 1100)
public abstract class JoinMultiplayerScreenTranslationMixin extends Screen {

    /**
     * 仅翻译 Meteor 自己添加到多人游戏界面的按钮（Accounts / Proxies）。
     * 原版按钮已由 Minecraft 语言系统本地化，第三方 mod（如 ViaFabricPlus）的按钮不属于本插件，
     * 都不应被翻译或记录进 lang.json。
     */
    @Unique
    private static final Set<String> METEOR_BUTTONS = Set.of("Accounts", "Proxies");

    protected JoinMultiplayerScreenTranslationMixin(Component title) {
        super(title);
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void onRepositionElementsTail(CallbackInfo ci) {
        for (GuiEventListener child : this.children()) {
            if (child instanceof Button button) {
                String text = button.getMessage().getString();
                if (!METEOR_BUTTONS.contains(text)) continue;
                String translated = gui(text);
                if (!text.equals(translated)) {
                    button.setMessage(Component.literal(translated));
                }
            }
        }
    }
}

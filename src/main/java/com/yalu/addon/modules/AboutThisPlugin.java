package com.yalu.addon.modules;

import com.yalu.addon.TranslateAddon;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;

public class AboutThisPlugin extends Module {

    private static final String SEPARATOR = "------------------------------------------------------------------------------------------------------";

    public AboutThisPlugin() {
        super(TranslateAddon.CATEGORY, "关于汉化插件", "插件作者|译者：kono_yalu");
    }

    @Override
    public void onActivate() {
        // 一次构建整段多行消息并只发一条，保证仅首行带「[彗星翻译]」前缀，其余行均为白色正文。
        MutableComponent msg = Component.empty();
        msg.append(Component.translatable("about.thisplugin.authors"));
        msg.append(newline());
        msg.append(Component.translatable("about.thisplugin.othertranslators"));
        msg.append(newline());
        msg.append(Component.translatable("about.thisplugin.version", TranslateAddon.VERSION));
        msg.append(newline());
        msg.append(Component.literal(SEPARATOR));
        msg.append(newline());
        msg.append(Component.translatable("about.thisplugin.desc"));
        msg.append(newline());
        msg.append(Component.literal(SEPARATOR));
        msg.append(newline());
        msg.append(Component.translatable("about.thisplugin.ai"));
        msg.append(newline());
        msg.append(Component.translatable("about.thisplugin.free"));
        msg.append(newline());
        msg.append(Component.translatable("about.thisplugin.langfile"));
        msg.append(newline());
        msg.append(Component.translatable("about.thisplugin.untranslated"));
        msg.append(newline());
        msg.append(prefix());
        msg.append(Component.translatable("about.thisplugin.repo").withStyle(
            s -> s.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/xjjakm/Meteor-I18n-Support-plugin/")))));

        addMessage(msg);
    }

    /** 前缀：[黄色左括号 + 紫色「彗星翻译」 + 黄色右括号]。 */
    private static MutableComponent prefix() {
        return Component.literal("[")
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.translatable("meteori18n.prefix").withStyle(ChatFormatting.DARK_PURPLE))
            .append(Component.literal("] ").withStyle(ChatFormatting.YELLOW));
    }

    private static Component newline() {
        return Component.literal("\n");
    }

    /** 与 Meteor 无关的原版聊天输出：整个文本显式设为白色，避免继承前缀紫色。 */
    private void addMessage(Component message) {
        MeteorClient.mc.gui.hud.getChat().addClientSystemMessage(prefix().append(message.copy().withStyle(ChatFormatting.WHITE)));
    }
}
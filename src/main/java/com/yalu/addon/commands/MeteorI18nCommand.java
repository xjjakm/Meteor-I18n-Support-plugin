package com.yalu.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.yalu.addon.util.JsonDump;
import com.yalu.addon.util.LanguageRefresh;
import com.yalu.addon.util.UniversalLangLoader;
import com.yalu.addon.util.trans_engine.TransEngineNew;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * 使用 Fabric Command API (v2) 挂载 /meteori18n 命令。
 * <p>
 * 输出与 Meteor 无关：不经过 Meteor 的聊天前缀管线，而是直接用标准 Minecraft
 * 语言文件（assets/yalu/lang/*.json）的键组装「[前缀] 消息」并写入聊天。前缀
 * 与正文均按当前游戏语言本地化（如紫色「[彗星翻译]」）。
 * <ul>
 *   <li>/meteori18n           —— 显示帮助信息</li>
 *   <li>/meteori18n help      —— 显示帮助信息</li>
 *   <li>/meteori18n reload    —— 重新加载并应用翻译</li>
 *   <li>/meteori18n export    —— 导出所有翻译键值对到语言文件</li>
 * </ul>
 */
public class MeteorI18nCommand {

    public static LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> build() {
        return literal("meteori18n")
            .executes(context -> {
                help();
                return SINGLE_SUCCESS;
            })
            .then(literal("help").executes(context -> {
                help();
                return SINGLE_SUCCESS;
            }))
            .then(literal("reload").executes(context -> {
                reload();
                return SINGLE_SUCCESS;
            }))
            .then(literal("export").executes(context -> {
                export();
                return SINGLE_SUCCESS;
            }));
    }

    private static void reload() {
        try {
            // 与语言切换（LanguageManagerMixin）完全一致的重载流程
            UniversalLangLoader.reload();               // 重载 universal 文本替换表
            ChatUtils.init();                           // 重建聊天前缀（ChatUtils 只在启动时初始化一次）
            LanguageRefresh.applyAll(true);              // 强制重载标准语言文件 + 重译模块/设置/分类/Tab/独立 Settings
            if (MeteorClient.mc.gui.screen() instanceof WidgetScreen widgetScreen) {
                widgetScreen.reload();                   // 刷新打开中的 Meteor GUI/HUD 编辑器控件树
            }
            send("meteori18n.reload.success");
        } catch (Exception e) {
            sendError("meteori18n.reload.error");
        }
    }

    private static void export() {
        try {
            int exported = JsonDump.getINSTANCE().write(new TransEngineNew(), new TransEngineNew());
            if (exported > 0) {
                send("meteori18n.export.success", exported, JsonDump.getINSTANCE().dumpPathExposed());
            } else {
                send("meteori18n.export.empty");
            }
        } catch (Exception e) {
            sendError("meteori18n.export.error");
        }
    }

    private static void help() {
        send("meteori18n.help");
        send("meteori18n.help.root");
        send("meteori18n.help.help");
        send("meteori18n.help.reload");
        send("meteori18n.help.export");
    }

    /** 发送一条带本地化前缀的普通聊天消息。 */
    private static void send(String key, Object... args) {
        addMessage(Component.translatable(key, args));
    }

    /** 发送一条带本地化前缀的红色错误消息。 */
    private static void sendError(String key) {
        addMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
    }

    private static void addMessage(Component message) {
        MutableComponent prefix = Component.literal("[")
            .withStyle(ChatFormatting.DARK_PURPLE)
            .append(Component.translatable("meteori18n.prefix").withStyle(ChatFormatting.DARK_PURPLE))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_PURPLE));
        MeteorClient.mc.gui.hud.getChat().addClientSystemMessage(prefix.append(message));
    }
}
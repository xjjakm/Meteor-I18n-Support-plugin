package com.yalu.addon;

import com.mojang.logging.LogUtils;
import com.yalu.addon.commands.MeteorI18nCommand;
import com.yalu.addon.mixin.CategoryAccessor;
import com.yalu.addon.modules.AboutThisPlugin;
import com.yalu.addon.util.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.PostInit;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.util.Set;

public class TranslateAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final String VERSION = "fork-1.1.0";
    public static final Minecraft MC = MeteorClient.mc;
    public static final Translator TRANSLATOR = new Translator();
    public static final Category CATEGORY = new Category("I18n");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor I18n Support Addon");

        // Load universal text replacement table early so that static strings
        // created during Meteor's PostInit (e.g. ChatUtils prefix) can be translated.
        UniversalLangLoader.reload();
        TextReplacement.setEnabled(true);

        // Translate the I18n category created during <clinit> (when MC was null)
        if (MC != null && MC.getResourceManager() != null) {
            TRANSLATOR.reload(MC.getResourceManager());
            String originalName = NameCache.category(CATEGORY);
            String key = "Category.Meteor." + TransUtil.baseFormat(originalName);
            String translated = TRANSLATOR.Translate(key, originalName);
            if (!translated.equals(originalName)) {
                ((CategoryAccessor) CATEGORY).setName(translated);
            }

            // Re-translate all modules, settings, categories, tabs and setting groups.
            // Some modules may have been constructed before MC was set (during <clinit>),
            // so their Mixin.onInit could not translate them at that time.
            LanguageRefresh.applyAll();
        }

        // Modules
        Modules.get().add(new AboutThisPlugin());

        // 使用 Fabric Command API 挂载 /meteori18n 导出命令
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(MeteorI18nCommand.build()));

        // Always dump collected unknown text when the client stops, even if
        // module deactivate is not reliably called during shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread(this::dumpUnknownText));
    }

    /**
     * 在 Meteor 所有 addon 的 onInitialize 之后、Minecraft 资源管理就绪时强制刷新一次翻译。
     * 分类/标签页/Meteor 自身模块在 onInitialize 阶段可能因 resourceManager 尚未就绪
     * （CategoryMixin.onInit 等提前 return）而没有应用中文，故在此兜底，避免启动后需手动 reload。
     * @PostInit 方法要求静态且由 ReflectInit 反射调用。
     */
    @PostInit
    public static void postInit() {
        if (MC == null || MC.getResourceManager() == null) return;
        LanguageRefresh.applyAll(true);
    }

    /**
     * 翻译 Meteor 及其它 addon GUI 中硬编码的字面量（按钮文字、下拉框枚举值等）。
     * 仅走标准语言文件系统的通用键 Gui.Meteor.{name}，不做任何映射回退。
     * 未命中且为英文时写入 lang.json（去重），并原样返回文本。
     */
    public static String gui(String text) {
        if (text == null || text.isEmpty()) return text;
        // 仅当文本包含 ASCII 字母（即仍是英文）时才查询/记录缺失键，
        // 避免把已翻译的中文文本再生成 Gui.Meteor.{中文} 键污染 lang.json。
        boolean hasAsciiLetter = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) { hasAsciiLetter = true; break; }
        }
        if (!hasAsciiLetter) return text;
        String key = "Gui.Meteor." + TransUtil.baseFormat(text);
        return TRANSLATOR.recordMissing(key, text);
    }

    private void dumpUnknownText() {
        Set<String> unknown = TextReplacement.getUnknown();
        if (unknown.isEmpty()) return;

        String path = UnknownDump.getCurrentPath();
        LOG.info("Shutdown hook: dumping {} unknown English strings to {}", unknown.size(), path);
        int wrote = UnknownDump.dump(unknown);
        if (wrote < 0) {
            LOG.error("Shutdown hook: failed to dump unknown strings to {}", path);
        }
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.yalu.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("xjjakm", "Meteor-I18n-Support-plugin");
    }
}

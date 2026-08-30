package com.yalu.addon;

import com.mojang.logging.LogUtils;
import com.yalu.addon.commands.MeteorI18nCommand;
import com.yalu.addon.mixin.CategoryAccessor;
import com.yalu.addon.modules.AboutThisPlugin;
import com.yalu.addon.util.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
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

        // 启动时删除旧的 lang.json，让缺失翻译键从本次启动重新干净收集，
        // 避免历史遗留的无意义键（按键名、玩家名等）残留。
        deleteOldLangJson();

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
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _registryAccess) ->
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
        // 文本含中日韩表意字符即视为已翻译（如"防AFK"、"存储ESP"、"Microsoft 喜马拉雅"），
        // 直接原样返回，避免把中英混合文本再生成 Gui.Meteor.{中文} 键污染 lang.json。
        for (int i = 0; i < text.length(); i++) {
            if (isCjk(text.charAt(i))) return text;
        }
        // 仅当文本包含 ASCII 字母（即仍是英文）时才查询/记录缺失键。
        boolean hasAsciiLetter = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) { hasAsciiLetter = true; break; }
        }
        if (!hasAsciiLetter) return text;
        // 跳过 Meteor 账户系统的玩家名（动态数据，非 UI 文本），
        // 避免把玩家名写进 lang.json。
        Accounts accounts = Accounts.get();
        if (accounts != null) {
            for (Account<?> account : accounts) {
                if (text.equalsIgnoreCase(account.getUsername())) return text;
            }
        }
        // 跳过按键绑定显示的按键名（动态数据，由 WKeybind 刷新时登记、vanilla key.* 语言键处理），
        // 避免把 "RCONTROL"、"左侧 Ctrl"、"Ctrl + Right Control"、"None" 等写进 lang.json。
        if (isKeybindText(text)) return text;
        String key = "Gui.Meteor." + TransUtil.baseFormat(text);
        return TRANSLATOR.recordMissing(key, text);
    }

    private static boolean isCjk(char c) {
        return (c >= '一' && c <= '鿿')  // CJK 统一表意文字
            || (c >= '㐀' && c <= '䶿')  // CJK 扩展 A
            || (c >= '豈' && c <= '﫿'); // CJK 兼容表意文字
    }

    /** WKeybind 刷新时登记的按键绑定显示文本（动态数据），gui() 据此跳过，避免写入 lang.json。 */
    private static final java.util.Set<String> keybindDisplayTexts = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    /** 供 WKeybindMixin 登记按键绑定显示文本。 */
    public static void recordKeybindText(String text) {
        if (text != null && !text.isEmpty()) keybindDisplayTexts.add(text);
    }

    /**
     * 设置组（SettingGroup）标题的中文显示表。
     * <p>
     * Group.name 是 final 字段，同时也是 NBT 序列化的键名（SettingGroup.toTag/fromTag 用它做
     * getGroup 查询）。以前用 SetAccessor.setName 把 name 改成中文会破坏序列化：保存时以中文名
     * 写入，下次启动 <code>Settings.fromTag</code> 用 <code>getGroup(中文名)</code> 在还没被改名
     * 的内存里找不到对应分组（GUI 主题分组在 postInit 阶段才改名，与 GuiThemes.postInit 加载主题
     * 的顺序不确定），导致颜色等设置恢复为默认。
     * <p>
     * 解决办法：不再改动 name 字段，改为在此记录每个分组实例 → 中文标题，由
     * DefaultSettingsWidgetFactory 渲染分组标题时读取。这样 name 始终保持英文，序列化键稳定。
     * 用 WeakHashMap 按对象身份保存，避免长期持有分组引用造成内存泄漏。
     */
    private static final java.util.Map<SettingGroup, String> groupTitles =
        java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** 记录分组实例对应的显示标题（渲染时用）。 */
    public static void recordGroupTitle(SettingGroup group, String title) {
        if (group != null && title != null && !title.isEmpty()) groupTitles.put(group, title);
    }

    /** 渲染分组标题时调用：优先用记录的中文标题，否则回退 name 原值。 */
    public static String groupTitle(SettingGroup group) {
        if (group == null) return "";
        String t;
        try {
            t = groupTitles.get(group);
        } catch (Throwable ignored) {
            t = null;
        }
        return t != null ? t : group.name;
    }

    private static boolean isKeybindText(String text) {
        if (text == null || text.isEmpty()) return false;
        if (keybindDisplayTexts.contains(text)) return true; // WKeybind 已登记的实际按键文本
        return isKeynameLike(text); // 兜底按值识别（不依赖 WKeybind，也不触发 glfw）
    }

    /** 按键名称特征识别（无 GLFW 调用）：单字母键（如 "Z"、"A"）或修饰键组合（如 "Ctrl + Z"）。 */
    private static boolean isKeynameLike(String text) {
        // 单个字母键：原版字母键译名就是英文字母本身（非中文），需跳过
        if (text.length() == 1) {
            char c = text.charAt(0);
            return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
        }
        // 修饰键组合，如 "Ctrl + Z"、"Shift + A"（" + " 分隔的 token 均为修饰键或单字母）
        if (text.contains(" + ")) {
            for (String seg : text.split(" \\+ ")) {
                String s = seg.trim();
                if (s.isEmpty() || !(isKeyModifier(s) || isSingleLetter(s))) return false;
            }
            return true;
        }
        return false;
    }

    private static boolean isKeyModifier(String s) {
        return s.equals("Ctrl") || s.equals("Cmd") || s.equals("Alt") || s.equals("Shift")
            || s.equals("Caps Lock") || s.equals("Num Lock");
    }

    private static boolean isSingleLetter(String s) {
        return s.length() == 1 && isAsciiLetter(s.charAt(0));
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    /** 删除运行目录下旧的缺失翻译清单（lang.json），仅在每次启动清理一次。 */
    private static void deleteOldLangJson() {
        try {
            boolean deleted = java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("lang.json"));
            if (deleted) LOG.info("[MeteorTranslation] 已删除旧的 lang.json");
        } catch (java.io.IOException e) {
            LOG.warn("[MeteorTranslation] 删除旧的 lang.json 失败: {}", e.toString());
        }
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

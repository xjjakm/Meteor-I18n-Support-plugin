package com.yalu.addon;

import com.mojang.logging.LogUtils;
import com.yalu.addon.commands.MeteorI18nCommand;
import com.yalu.addon.mixin.CategoryAccessor;
import com.yalu.addon.modules.AboutThisPlugin;
import com.yalu.addon.util.LanguageRefresh;
import com.yalu.addon.util.NameCache;
import com.yalu.addon.util.TransUtil;
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
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.util.Map;

public class TranslateAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final String VERSION = "fork-1.1.0";
    /**
     * 动态获取 Minecraft 实例。MeteorClient.mc 在类加载时为 null，
     * 之后才由 Minecraft 构造器赋值；不能用 static final 快照，
     * 否则翻译加载永远判断 MC == null。
     */
    public static Minecraft getMC() { return MeteorClient.mc; }
    public static final Translator TRANSLATOR = new Translator();
    public static final Category CATEGORY = new Category("I18n");

    @Override
    public void onInitialize() {
        LOG.info("[MeteorTranslation] === onInitialize() START ===");
        LOG.info("[MeteorTranslation] MC={}, resourceManager={}", getMC() != null ? "OK" : "NULL",
            getMC() != null && getMC().getResourceManager() != null ? "OK" : "NULL");

        // 启动时删除旧的 lang.json，让缺失翻译键从本次启动重新干净收集，
        // 避免历史遗留的无意义键（按键名、玩家名等）残留。
        deleteOldLangJson();

        // 注册 Fabric 生命周期事件 —— MC 完全启动后回调
        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
            LOG.info("[MeteorTranslation] === CLIENT_STARTED ===");
            LOG.info("[MeteorTranslation] MC={}, resourceManager={}", mc != null ? "OK" : "NULL",
                mc != null && mc.getResourceManager() != null ? "OK" : "NULL");
            reloadTranslations("CLIENT_STARTED", mc);
        });

        // 注册资源重载监听器 —— /reload 或 F3+T 时回调
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() { return Identifier.fromNamespaceAndPath("meteor-i18n-addon", "reload"); }

                @Override
                public void onResourceManagerReload(ResourceManager manager) {
                    LOG.info("[MeteorTranslation] === Resource reload (/reload or F3+T) ===");
                    LOG.info("[MeteorTranslation] manager={}", manager != null ? "OK" : "NULL");
                    if (manager != null) {
                        TRANSLATOR.reload(manager);
                        LanguageRefresh.applyAll();
                        LOG.info("[MeteorTranslation] Resource reload done");
                    }
                }
            });

        // Translate the I18n category created during <clinit> (when MC was null)
        LOG.info("[MeteorTranslation] Trying reload in onInitialize...");
        if (getMC() != null && getMC().getResourceManager() != null) {
            reloadTranslations("onInitialize", getMC());
        } else {
            LOG.warn("[MeteorTranslation] SKIP onInitialize reload: MC or resourceManager is null");
        }

        // Modules
        Modules.get().add(new AboutThisPlugin());

        // 使用 Fabric Command API 挂载 /meteori18n 导出命令
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _registryAccess) ->
            dispatcher.register(MeteorI18nCommand.build()));

        LOG.info("[MeteorTranslation] === onInitialize() END ===");
    }

    /** 统一的翻译重载入口，带完整日志。 */
    private static void reloadTranslations(String where, Minecraft mc) {
        if (mc == null) {
            LOG.warn("[MeteorTranslation] [{}] mc is null, abort reload", where);
            return;
        }
        var rm = mc.getResourceManager();
        if (rm == null) {
            LOG.warn("[MeteorTranslation] [{}] resourceManager is null, abort reload", where);
            return;
        }
        LOG.info("[MeteorTranslation] [{}] reload START, languageCode={}", where, mc.options.languageCode);
        try {
            TRANSLATOR.reload(rm);
            LOG.info("[MeteorTranslation] [{}] TRANSLATOR.reload OK", where);

            // 翻译 I18n 分类（clinit 时 MC 为 null，当时没翻译）
            String originalName = NameCache.category(CATEGORY);
            String key = "Category.Meteor." + TransUtil.baseFormat(originalName);
            String translated = TRANSLATOR.Translate(key, originalName);
            if (!translated.equals(originalName)) {
                ((CategoryAccessor) CATEGORY).setName(translated);
                LOG.info("[MeteorTranslation] [{}] Category renamed: {} -> {}", where, originalName, translated);
            }

            // 全量重刷所有模块/设置/分类/标签页
            LanguageRefresh.applyAll();
            LOG.info("[MeteorTranslation] [{}] LanguageRefresh.applyAll OK", where);
        } catch (Throwable t) {
            LOG.error("[MeteorTranslation] [{}] reload FAILED", where, t);
        }
        LOG.info("[MeteorTranslation] [{}] reload DONE", where);
    }

    /**
     * 在 Meteor 所有 addon 的 onInitialize 之后、Minecraft 资源管理就绪时强制刷新一次翻译。
     * 分类/标签页/Meteor 自身模块在 onInitialize 阶段可能因 resourceManager 尚未就绪
     * （CategoryMixin.onInit 等提前 return）而没有应用中文，故在此兜底，避免启动后需手动 reload。
     * @PostInit 方法要求静态且由 ReflectInit 反射调用。
     */
    @PostInit
    public static void postInit() {
        LOG.info("[MeteorTranslation] === @PostInit postInit() ===");
        LOG.info("[MeteorTranslation] MC={}, resourceManager={}", getMC() != null ? "OK" : "NULL",
            getMC() != null && getMC().getResourceManager() != null ? "OK" : "NULL");
        if (getMC() == null || getMC().getResourceManager() == null) {
            LOG.warn("[MeteorTranslation] @PostInit SKIP: MC or resourceManager is null");
            return;
        }
        LanguageRefresh.applyAll(true);
        LOG.info("[MeteorTranslation] @PostInit LanguageRefresh.applyAll(true) OK");
    }

    /**
     * 翻译 Meteor 及其它 addon GUI 中硬编码的字面量（按钮、标签、标题等）。
     * 仅走标准语言文件系统的通用键 Gui.Meteor.{name}，不做任何映射回退。
     * 未命中且为英文时写入 lang.json（去重），并原样返回文本。
     */
    public static String gui(String text) {
        return guiWithKeyPrefix("Gui.Meteor.", text);
    }

    /**
     * 下拉框选项专用入口（WDropdown 折叠头部 / 展开列表 / Catppuccin getNameFor）。
     * 键前缀为 Dropdown.Meteor.（如 Dropdown.Meteor.flat），与通用 Gui.Meteor.* 命名空间区分，
     * 过滤规则与 gui() 完全一致（模板、CJK、账户名、按键名、输入文本）。
     */
    public static String dropdown(String text) {
        return guiWithKeyPrefix("Dropdown.Meteor.", text);
    }

    /**
     * Meteor GUI 按钮专用入口（WButton / WMeteorButton / WConfirmedButton 等自绘控件）。
     * 键前缀为 Button.Meteor.（如 Button.Meteor.save）。
     */
    public static String button(String text) {
        return guiWithKeyPrefix("Button.Meteor.", text);
    }

    /**
     * Meteor 通过 screen mixin 注入到原版界面的原版样式按钮专用入口
     * （DisconnectedScreen 的 Reconnect / Toggle Auto Reconnect，BookEditScreen / BookViewScreen
     * 的 Copy / Paste，JoinMultiplayerScreen 的 Accounts / Proxies）。
     * 键前缀为 mixinButton.Meteor.（如 mixinButton.Meteor.reconnect）。
     * 第三方 mod（如 ViaFabricPlus）注入的按钮由调用方白名单过滤，不进入此路径。
     */
    public static String mixinButton(String text) {
        return guiWithKeyPrefix("mixinButton.Meteor.", text);
    }

    /**
     * Meteor 注入原版界面的按钮文本白名单（精确匹配）。
     * 原版 translatable 按钮、第三方 mod（如 ViaFabricPlus）注入的按钮
     * 文本不会命中这些裸词，从而被 VanillaButtonMixin 原样放行。
     */
    private static final java.util.Set<String> VANILLA_STYLE_BUTTONS = java.util.Set.of(
        "Accounts", "Proxies", "Copy", "Paste", "Toggle Auto Reconnect"
    );

    /**
     * Reconnect / Reconnect (N.N) 动态模板（%.1f 的小数分隔符点/逗号都兼容）。
     * Auto Reconnect 开启时按钮文本带倒计时后缀，键固定为 mixinButton.Meteor.reconnect。
     */
    private static final java.util.regex.Pattern RECONNECT_TEMPLATE =
        java.util.regex.Pattern.compile("^Reconnect(?: \\(([0-9]+[.,][0-9])\\))?$");

    /**
     * VanillaButtonMixin 的拦截入口：命中白名单/模板时返回译文的 literal Component，
     * 否则返回原引用（快速路径，每帧多次调用无额外开销）。
     */
    public static Component applyVanillaButtonMessage(Component original) {
        if (original == null) return null;
        String text = original.getString();
        if (text == null || text.isEmpty()) return original;

        if (VANILLA_STYLE_BUTTONS.contains(text)) {
            String translated = mixinButton(text);
            return translated.equals(text) ? original : Component.literal(translated);
        }

        if (text.startsWith("Reconnect")) {
            java.util.regex.Matcher m = RECONNECT_TEMPLATE.matcher(text);
            if (m.matches()) {
                String translated = mixinButton("Reconnect");
                if (translated.equals("Reconnect")) return original;
                // 保留原始倒计时后缀，如 "重新连接 (2.3)"
                return Component.literal(m.group(1) == null ? translated : translated + " (" + m.group(1) + ")");
            }
        }
        return original;
    }

    private static String guiWithKeyPrefix(String keyPrefix, String text) {
        if (text == null || text.isEmpty()) return text;

        // 处理含动态数字的格式化串：数字可能变化，把数字占位后按模板查翻译，再把数字还原。
        // 例如 "(0 selected)" → 模板 "(N selected)" → 查 Gui.Meteor._n_selected 或 meteori18n.selected-template，
        // 再把 N 替换回原数字。这样语言文件只需维护模板，不会因为数字不同生成 N 个键。
        String templated = tryTemplateTranslate(text);
        if (templated != null) return templated;

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
        // 跳过 WTextBox 的实时输入文本（由 WTextBoxMixin 渲染时登记）：用户输入是动态数据，
        // 不应按 Gui.Meteor.{输入} 翻译（否则输入 "vanilla" 会因 Gui.Meteor.vanilla="原版" 变字）
        // 也不应写入 lang.json。占位提示（placeholder）走 onInitPlaceholder 的 gui() 仍正常翻译。
        if (userTypedTexts.contains(text)) return text;
        String key = keyPrefix + TransUtil.baseFormat(text);
        return TRANSLATOR.recordMissing(key, text);
    }

    /**
     * 检测含动态数字的常见模板串，把数字占位为 'N' 后查翻译表，再还原数字。
     * 命中返回翻译后文本；未命中返回 null 让 gui() 走正常流程。
     * 当前处理的模板：
     *   - "(N selected)" / "(N selected)"  → meteori18n.selected-count
     */
    private static final java.util.regex.Pattern SELECTED_COUNT = java.util.regex.Pattern.compile("^\\((\\d+) selected\\)$");

    private static String tryTemplateTranslate(String text) {
        // "(N selected)"
        java.util.regex.Matcher m = SELECTED_COUNT.matcher(text);
        if (m.matches()) {
            String n = m.group(1);
            String template = TRANSLATOR.get("meteori18n.selected-count", null);
            if (template == null) {
                TRANSLATOR.recordMissing("meteori18n.selected-count", "(N selected)");
                return text;
            }
            return template.replace("{n}", n);
        }
        return null;
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
     * WTextBox 实时输入文本集合：由 WTextBoxMixin 在渲染时登记当前 text。
     * gui() 据此跳过输入内容（不翻译、不写 lang.json），而 placeholder 仍走 gui() 翻译。
     */
    private static final java.util.Set<String> userTypedTexts = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    /** 仅登记含 ASCII 字母的输入（gui() 只处理这类文本），避免空串/纯符号/中文占用集合。 */
    public static void recordUserTypedText(String text) {
        if (text == null || text.isEmpty()) return;
        boolean hasAsciiLetter = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) { hasAsciiLetter = true; break; }
        }
        if (hasAsciiLetter) userTypedTexts.add(text);
    }

    /**
     * Meteor 聊天消息模板 → 标准语言文件键（meteori18n.*）的映射。
     * 例如 Modules.java 绑定快捷键的 info("Bound to (highlight)%s(default).", ...)，
     * 经 ChatUtilsMixin 用标准语言文件查找对应译文，替代废弃的通用翻译表。
     * 模板中的 (highlight)/(default) 是 Meteor 的格式化标记，译文需原样保留。
     */
    private static final Map<String, String> CHAT_TEMPLATE_KEYS = Map.of(
        "Bound to (highlight)%s(default).", "meteori18n.bound-to"
    );

    /** 返回聊天模板对应的标准语言文件键；未配置时返回 null。 */
    public static String chatTemplateKey(String template) {
        return CHAT_TEMPLATE_KEYS.get(template);
    }

    /**
     * 直接绘制到屏幕（原版 GuiGraphics.text）的文本模板 → 标准语言文件键（meteori18n.*）。
     * 例如多人游戏界面的 "Logged in as " / "Using proxy " / "Not using a proxy"（Meteor 的
     * JoinMultiplayerScreenMixin 绘制），经 GuiGraphicsExtractorMixin 从标准语言文件取译文，
     * 替代废弃的通用翻译表。
     */
    private static final Map<String, String> GUI_TEXT_KEYS = Map.of(
        "Logged in as ", "meteori18n.logged-in-as",
        "Using proxy ", "meteori18n.using-proxy",
        "Not using a proxy", "meteori18n.not-using-proxy"
    );

    /** 返回屏幕文本模板对应的标准语言文件键；未配置时返回 null。 */
    public static String guiTextKey(String template) {
        return GUI_TEXT_KEYS.get(template);
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

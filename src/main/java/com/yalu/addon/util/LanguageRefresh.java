package com.yalu.addon.util;

import com.yalu.addon.mixin.*;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

import static com.yalu.addon.TranslateAddon.*;

/**
 * 无需重启就能将所有已经 <init> 过的模块、设置、分类、Tab 等字段
 * 按照当前语言重新翻译。由 LanguageManager.setSelected() 切换语言时调用。
 */
public final class LanguageRefresh {

    private static final Logger LOGGER = LOG;

    private LanguageRefresh() {}

    public static void applyAll() {
        applyAll(false);
    }

    /** @param forceTranslatorReload 强制重载 Translator 标准语言文件，绕过幂等去重（手动 reload 用） */
    public static void applyAll(boolean forceTranslatorReload) {
        try {
            if (MC == null || MC.getResourceManager() == null) return;

            // 1. 重新加载 Translator（标准语言文件 zh_cn.json / en_us.json 等）
            if (forceTranslatorReload) {
                TRANSLATOR.forceReload(MC.getResourceManager());
            } else {
                TRANSLATOR.reload(MC.getResourceManager());
            }

            int modulesDone = 0, settingsDone = 0, groupsDone = 0, categoriesDone = 0, tabsDone = 0;

            // 2. 重翻译所有 Module
            for (Module module : Modules.get().getAll()) {
                // module.name 字段永远保持原始英文；title/description 会被翻译。
                // 动态获取插件前缀，与 ModuleMixin.onInit 逻辑一致
                String packageName = module.addon.name.replace(" ", "-");
                if (packageName.equals("Meteor-Client")) {
                    packageName = "Meteor";
                }
                String moduleKey = "Module." + packageName + "." + module.name;
                String descKey   = "Module." + packageName + "." + module.name + ".Description";

                String newTitle = TRANSLATOR.Translate(moduleKey, module.name);
                String newDesc  = TRANSLATOR.Translate(descKey, module.description);
                ((ModuleAccessor) module).setTitle(newTitle);
                ((ModuleAccessor) module).setDescription(newDesc);
                modulesDone++;

                // 3. 该 Module 下所有 SettingGroup + Setting
                if (module.settings != null) {
                    for (SettingGroup group : module.settings.groups) {
                        // SettingGroup 名称
                        String originalGroupName = NameCache.group(group);
                        String groupKey = getSettingGroupKey(module, originalGroupName);
                        String translatedGroup = TRANSLATOR.Translate(groupKey, originalGroupName);
                        if (!translatedGroup.equals(originalGroupName)) {
                            ((SettingGroupAccessor) group).setName(translatedGroup);
                        }
                        groupsDone++;

                        // Settings
                        for (Setting<?> setting : ((SettingGroupAccessor) group).getSettings()) {
                            String originalSettingName = ((SettingAccessor) setting).getName();
                            String settingKey   = "Setting." + packageName + "." + originalSettingName;
                            String settingDescKey = "Setting." + packageName + "." + originalSettingName + ".Description";
                            String newSettingTitle = TRANSLATOR.Translate(settingKey, originalSettingName);
                            String originalDesc    = setting.description;
                            String newSettingDesc  = TRANSLATOR.Translate(settingDescKey, originalDesc);
                            ((SettingAccessor) setting).setTitle(newSettingTitle);
                            ((SettingAccessor) setting).setDescription(newSettingDesc);
                            recordDropdownValues(setting);
                            settingsDone++;
                        }
                    }
                }
            }

            // 3.5 重翻译不属于任何 Module 的独立 Settings（HUD、GUI 主题等）。
            // 这两种 Settings 不会被遍历 Modules 的循环覆盖，若不在此重翻，
            // 它们的 Setting.title 会停留在游戏启动时的语言。
            // onInitialize 阶段可能在 GuiThemes/Hud 初始化前触发（Minecraft.<init> 期间），
            // 因此对它们的 get() 结果做空判断，避免 NPE 中断后续分类/Tab 的翻译。
            Hud hud = Hud.get();
            if (hud != null) translateStandalone("hud", hud.settings);

            GuiTheme guiTheme = GuiThemes.get();
            if (guiTheme != null) translateStandalone("gui_theme", guiTheme.settings);

            // 配置系统设置（与上方独立 Settings 采用相同的 Setting.* 键格式）
            Config config = Config.get();
            if (config != null) translateStandalone("config", config.settings);

            // 4. 重翻译所有 Category（包括第三方的）
            for (Category category : Modules.loopCategories()) {
                String originalName = NameCache.category(category);
                String key = "Category.Meteor." + TransUtil.baseFormat(originalName);
                String translated = TRANSLATOR.Translate(key, originalName);
                if (!translated.equals(category.name)) {
                    ((CategoryAccessor) category).setName(translated);
                }
                categoriesDone++;
            }

            // 5. 重翻译所有 Tab
            for (Tab tab : Tabs.get()) {
                String originalName = NameCache.tab(tab);
                String key = "Tab.Meteor." + TransUtil.baseFormat(originalName);
                String translated = TRANSLATOR.Translate(key, originalName);
                if (!translated.equals(tab.name)) {
                    ((TabAccessor) tab).setName(translated);
                }
                tabsDone++;
            }

            LOGGER.info("[MeteorTranslation] Language refreshed: modules={}, settings={}, groups={}, categories={}, tabs={}",
                modulesDone, settingsDone, groupsDone, categoriesDone, tabsDone);

        } catch (Throwable t) {
            LOGGER.error("[MeteorTranslation] Failed to apply language refresh", t);
        }
    }

    private static String getSettingGroupKey(Module module, String groupName) {
        // 与 ModuleMixin.onInit 逻辑一致：动态获取插件前缀
        String packageName = module.addon.name.replace(" ", "-");
        if (packageName.equals("Meteor-Client")) {
            packageName = "Meteor";
        }
        return "Module." + packageName + "." + module.name + "." + TransUtil.baseFormat(groupName) + ".name";
    }

    /**
     * 重翻译一个独立 Settings 对象中的所有设置（title / description），并将缺失
     * 的设置组名一并写入清单。
     * 键名规则与 SettingMixin 保持一致：根据 Setting 的运行时类前缀解析所属 addon，
     * 生成 "Setting.{pkg}.{name}" 与 "Setting.{pkg}.{name}.Description"。
     * 设置组名采用 "Setting.{prefix}.{group}.name"，prefix 用于区分 HUD/Config/GUI主题
     * 对应不同的独立系统设置（避免同名组互相覆盖）。
     */
    private static void translateStandalone(String prefix, Settings settings) {
        if (settings == null) return;
        for (SettingGroup group : settings.groups) {
            // 设置组名
            String originalGroupName = NameCache.group(group) != null ? NameCache.group(group) : group.name;
            String groupKey = "Setting." + prefix + "." + TransUtil.baseFormat(originalGroupName) + ".name";
            String translatedGroup = TRANSLATOR.Translate(groupKey, originalGroupName);
            if (!translatedGroup.equals(originalGroupName)) {
                ((SettingGroupAccessor) group).setName(translatedGroup);
            }

            for (Setting<?> setting : ((SettingGroupAccessor) group).getSettings()) {
                String originalName = ((SettingAccessor) setting).getName();
                String pkg = getSettingPackage(setting);
                String settingKey = "Setting." + pkg + "." + originalName;
                String settingDescKey = settingKey + ".Description";
                ((SettingAccessor) setting).setTitle(
                    TRANSLATOR.Translate(settingKey, originalName));
                ((SettingAccessor) setting).setDescription(
                    TRANSLATOR.Translate(settingDescKey, setting.description));
                recordDropdownValues(setting);
            }
        }
    }

    /** 与 SettingMixin 相同的 addon 解析逻辑。 */
    private static String getSettingPackage(Setting<?> setting) {
        String classname = setting.getClass().getName();
        for (MeteorAddon addon : AddonManager.ADDONS) {
            if (classname.startsWith(addon.getPackage())) {
                String pkg = addon.name.replace(" ", "-");
                return pkg.equals("Meteor-Client") ? "Meteor" : pkg;
            }
        }
        return "Meteor";
    }

    /**
     * 记录下拉框所有候选值的缺失翻译键，避免必须逐个点开下拉框才会写入 lang.json。
     * 这里通过设置的数据模型静态枚举（而非等待界面渲染——渲染只发生在打开界面时）：
     * - EnumSetting：遍历全部枚举常量，选项文本即 value.toString()；
     * - ProvidedStringSetting：遍历 supplier 提供的候选字符串。
     * 统一走 gui()，与 WMeteorDropdown 渲染路径生成的键完全一致（Gui.Meteor.{baseFormat}），
     * 因此启动时即可把这些缺失键写入 lang.json。
     */
    private static void recordDropdownValues(Setting<?> setting) {
        try {
            if (setting instanceof EnumSetting<?> enumSetting) {
                Object current = enumSetting.get();
                if (current instanceof Enum<?> e) {
                    for (Object constant : e.getDeclaringClass().getEnumConstants()) {
                        if (constant != null) gui(constant.toString());
                    }
                }
            } else if (setting instanceof ProvidedStringSetting provided) {
                if (provided.supplier != null) {
                    String[] options = provided.supplier.get();
                    if (options != null) {
                        for (String option : options) {
                            if (option != null) gui(option);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("[MeteorTranslation] Failed to record dropdown values for setting", t);
        }
    }
}

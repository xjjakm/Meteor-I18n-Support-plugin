package com.yalu.addon.util;

import com.yalu.addon.mixin.*;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
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
        try {
            if (MC == null || MC.getResourceManager() == null) return;

            // 1. 重新加载 Translator（标准语言文件 zh_cn.json / en_us.json 等）
            TRANSLATOR.reload(MC.getResourceManager());

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
                            settingsDone++;
                        }
                    }
                }
            }

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
}

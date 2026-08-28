package com.yalu.addon.util;

import com.mojang.logging.LogUtils;
import com.yalu.addon.mixin.SettingGroupAccessor;
import com.yalu.addon.util.trans_engine.AbstractTransEngine;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonDump {

    public static JsonDump getINSTANCE() {
        return INSTANCE;
    }

    private static final JsonDump INSTANCE = new JsonDump();

    private static final Logger LOGGER = LogUtils.getLogger();

    //    private LinkedHashSet<String> keySet = new LinkedHashSet<>();
    private final LinkedHashMap<String, String> entMap = new LinkedHashMap<>();
    private BufferedWriter dumpBW;

    private boolean dumpText() {
        return true;
    }

    private String dumpPath() {
        // 输出到 .minecraft 下的固定绝对路径，与 unknown.json 同目录保持统一
        java.nio.file.Path mcDir = Minecraft.getInstance().gameDirectory.toPath();
        return mcDir.resolve("meteor-client/meteor-translation-addon/meteor-i18n-export.json").toString();
    }

    /** 供命令层获取导出路径（用于本地化导出的成功提示）。 */
    public String dumpPathExposed() {
        return dumpPath();
    }


    /**
     * 导出所有翻译键值对到语言文件。
     * @return 成功导出的翻译条数；无数据时返回 0。
     * @throws RuntimeException 导出失败时抛出（由调用方决定如何提示）。
     */
    public int write(AbstractTransEngine engine, AbstractTransEngine engine2) {

        dump2Set(engine, engine2);

        if (entMap.isEmpty()) {
            LOGGER.info("[MeteorTranslation] 无翻译数据可导出，跳过 (lang 文件 {})", dumpPath());
            entMap.clear();
            return 0;
        }

        try {
            File path = new File(dumpPath());
            File parent = path.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                LOGGER.error("[MeteorTranslation] 导出失败：无法创建导出目录 {}", parent);
                entMap.clear();
                throw new RuntimeException("无法创建导出目录 " + parent);
            }
            if (path.exists() || path.createNewFile()) {

                // 规范 JSON：构建对象后由 Gson 序列化，保证键值转义正确
                com.google.gson.JsonObject jsonObj = new com.google.gson.JsonObject();
                for (Map.Entry<String, String> entry : entMap.entrySet()) {
                    jsonObj.addProperty(entry.getKey(), entry.getValue());
                }
                String json = new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(jsonObj);

                dumpBW = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(path, false), StandardCharsets.UTF_8));
                dumpBW.write(json);
                dumpBW.newLine();

                dumpBW.flush();
                dumpBW.close();

                int count = entMap.size();
                LOGGER.info("[MeteorTranslation] 已导出 {} 条翻译到 lang 文件 {} (作为 json: true)", count, dumpPath());
                entMap.clear();
                return count;
            } else {
                entMap.clear();
                throw new RuntimeException("无法创建导出文件 " + path);
            }
        } catch (IOException e) {
            LOGGER.error("[MeteorTranslation] 导出 lang 文件 {} 失败: {}", dumpPath(), e.getMessage());
            entMap.clear();
            throw new RuntimeException(e);
        } finally {
            try {
                if (dumpBW != null)
                    dumpBW.close();
            } catch (IOException ignore) {
            }
        }
    }

    private void dump2Set(AbstractTransEngine engine, AbstractTransEngine engine2) {
        boolean dumpText = dumpText();
        for (Module module : Modules.get().getAll()) {
            // 不再按 addon 过滤，导出所有模块的翻译

            String nameKey = engine.getModuleNameKey(module);
            addEntry(nameKey, dumpText ? engine2.transModuleName(module) : module.name);

            String desKey = engine.getModuleDescriptionKey(module);
            addEntry(desKey, dumpText ? engine2.transModuleDescription(module) : module.description);

            for (SettingGroup group : module.settings.groups) {
                for (Setting<?> setting : ((SettingGroupAccessor) group).getSettings()) {

                    String settingNameKey = engine.getSettingNameKey(module, group, setting);
                    addEntry(settingNameKey, dumpText ? engine2.transSettingName(module, group, setting) : setting.name);

                    String settDescKey = engine.getSettingDesKey(module, group, setting);
                    addEntry(settDescKey, dumpText ? engine2.transSettingDes(module, group, setting) : setting.description);


                }

                // 模块设置组名称
                String groupNameKey = engine.getGroupNameKey(module, group);
                addEntry(groupNameKey, dumpText ? engine2.transGroupName(module, group) : group.name);
            }

        }

        // --- HUD Element keys ---
        for (HudElementInfo<?> info : Hud.get().infos.values()) {
            HudGroup hudGroup = info.group;
            String hudTitleKey = engine.getHudTitleKey(hudGroup, info);
            addEntry(hudTitleKey, dumpText ? engine2.transHudTitle(hudGroup, info) : info.title);

            String hudDescKey = engine.getHudDescriptionKey(hudGroup, info);
            addEntry(hudDescKey, dumpText ? engine2.transHudDescription(hudGroup, info) : info.description);
        }

        // --- Category keys ---
        for (Category category : Modules.loopCategories()) {
            String key = engine.getCategoryNameKey(category);
            addEntry(key, dumpText ? engine2.transCategoryName(category) : category.name);
        }

        // --- Command keys ---
        for (Command command : Commands.COMMANDS) {
            String titleKey = engine.getCommandTitleKey(command);
            addEntry(titleKey, dumpText ? engine2.transCommandTitle(command) : Utils.nameToTitle(command.getName()));

            String descKey = engine.getCommandDescriptionKey(command);
            addEntry(descKey, dumpText ? engine2.transCommandDescription(command) : command.getDescription());
        }

        // --- HUD Preset keys ---
        for (HudElementInfo<?> info : Hud.get().infos.values()) {
            if (!info.hasPresets()) continue;
            for (HudElementInfo<?>.Preset preset : info.presets) {
                String presetName = TransUtil.baseFormat(preset.title);
                String key = engine.getHudPresetTitleKey(info, presetName);
                addEntry(key, dumpText ? engine.transHudPresetTitle(info, presetName) : preset.title);
            }
        }

        // --- Tab keys ---
        for (Tab tab : Tabs.get()) {
            String key = engine.getTabNameKey(tab);
            addEntry(key, dumpText ? engine.transTabName(tab) : tab.name);
        }

        // --- System-level SettingGroup keys ---
        dumpSystemGroups(engine, engine2, "hud", Hud.get().settings, dumpText);
        dumpSystemGroups(engine, engine2, "config", Config.get().settings, dumpText);
        dumpSystemGroups(engine, engine2, "gui_theme", GuiThemes.get().settings, dumpText);
    }

    private void dumpSystemGroups(AbstractTransEngine engine, AbstractTransEngine engine2, String systemId, Settings settings, boolean dumpText) {
        for (SettingGroup group : settings.groups) {
            String originalName = NameCache.group(group);
            if (originalName == null || originalName.isEmpty()) continue;

            String key = engine.getSystemGroupNameKey(systemId, group);
            addEntry(key, dumpText ? engine.transSystemGroupName(systemId, group) : group.name);
        }
    }

    private void addEntry(String key, String value) {
        entMap.putIfAbsent(key, value);
    }
}

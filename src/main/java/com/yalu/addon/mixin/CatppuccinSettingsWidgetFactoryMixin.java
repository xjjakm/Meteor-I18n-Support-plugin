package com.yalu.addon.mixin;

import com.yalu.addon.TranslateAddon;
import me.pindour.catppuccin.gui.themes.catppuccin.CatppuccinSettingsWidgetFactory;
import meteordevelopment.meteorclient.settings.SettingGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Catppuccin 主题有独立的设置渲染工厂，其 group() 同样直接读取 group.name 作为分组标题。
 * 与 DefaultSettingsWidgetFactoryMixin 一样：不改写 group.name（NBT 序列化键），
 * 仅在这里用记录的中文标题替代显示用原值，保证颜色等设置重启后能正确恢复。
 */
@Mixin(value = CatppuccinSettingsWidgetFactory.class, remap = false)
public class CatppuccinSettingsWidgetFactoryMixin {

    @Redirect(
        method = "group",
        at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/settings/SettingGroup;name:Ljava/lang/String;"),
        remap = false
    )
    private String redirectGroupName(SettingGroup group) {
        return TranslateAddon.groupTitle(group);
    }
}
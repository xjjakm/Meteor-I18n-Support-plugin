package com.yalu.addon.mixin;


import com.yalu.addon.TranslateAddon;
import com.yalu.addon.settings.ExtraSettings;
import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory;
import meteordevelopment.meteorclient.settings.SettingGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DefaultSettingsWidgetFactory.class)
public abstract class DefaultSettingsWidgetFactoryMixin extends SettingsWidgetFactory {
public DefaultSettingsWidgetFactoryMixin(GuiTheme theme) {
super(theme);
}

@Inject(method = "<init>", at = @At("TAIL"), remap = false)
private void onInit(GuiTheme theme, CallbackInfo ci) {
new ExtraSettings(factories, this.theme).addSettings();
}

/**
 * 渲染设置组分组标题时，用记录的中文标题替代 group.name 原值。
 * 不再改写 group.name（它同时是 NBT 序列化键，改了会导致设置重载回默认），
 * 只在这里叠加显示层翻译。上侧 group() 的唯一一处读取即为分组标题。
 */
@Redirect(
    method = "group",
    at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/settings/SettingGroup;name:Ljava/lang/String;"),
    remap = false
)
private String redirectGroupName(SettingGroup group) {
return TranslateAddon.groupTitle(group);
}
}
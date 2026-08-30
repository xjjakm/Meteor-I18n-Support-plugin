package com.yalu.addon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static com.yalu.addon.TranslateAddon.gui;

/**
* RichText 是 Catppuccin 主题所有 widget 文本的统一汇聚点。
* Catppuccin 的 WCatppuccinButton / WCatppuccinLabel / WCatppuccinSection /
* WCatppuccinDropdown / WCatppuccinTopBar 等把文本缓存进 RichText 字段、渲染时直接读取，
* 绕过了 Meteor 基类的 set()/构造器和 WMeteorButtonMixin，导致按钮等在此主题下仍为英文。
* 本 mixin 在 RichText 的构造器与 append() 两处处理文本，一处覆盖整个 Catppuccin 主题，
* 仅走标准语言文件系统的通用键 Gui.Meteor.{name}（不使用通用翻译表）。
*/
@Mixin(targets = "me.pindour.catppuccin.api.text.RichText", remap = false)
public class RichTextMixin {

@ModifyVariable(method = "<init>(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true, name = "text")
private static String onInit(String text) {
return gui(text);
}

@ModifyVariable(
method = "append(Ljava/lang/String;)Lme/pindour/catppuccin/api/text/RichText;",
at = @At("HEAD"),
argsOnly = true,
name = "text"
)
private String onAppend(String text) {
return gui(text);
}
}
package com.yalu.addon.mixin;

import com.yalu.addon.util.TextReplacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
* RichText 是 Catppuccin 主题所有 widget 文本的统一汇聚点。
* Catppuccin 的 WCatppuccinModule / WCatppuccinDropdown / WCatppuccinTopBar 等
* 在构造时把文本缓存进 RichText 字段、渲染时直接读取，绕过了 Meteor 基类的 set()/构造器，
* 导致这些 widget 在通用替换层下失效。
* 本 mixin 在 RichText 的构造器与 append() 两处替换，一处覆盖整个 Catppuccin 主题。
*/
@Mixin(targets = "me.pindour.catppuccin.api.text.RichText", remap = false)
public class RichTextMixin {

@ModifyVariable(method = "<init>(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true, index = 1)
private static String onInit(String text) {
return TextReplacement.replace(text);
}

@ModifyVariable(
method = "append(Ljava/lang/String;)Lme/pindour/catppuccin/api/text/RichText;",
at = @At("HEAD"),
argsOnly = true
)
private String onAppend(String text) {
return TextReplacement.replace(text);
}
}
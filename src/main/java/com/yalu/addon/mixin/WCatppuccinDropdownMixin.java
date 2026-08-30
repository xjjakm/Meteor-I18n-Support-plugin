package com.yalu.addon.mixin;

import me.pindour.catppuccin.gui.themes.catppuccin.widgets.input.WCatppuccinDropdown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.yalu.addon.TranslateAddon.gui;

/**
 * Catppuccin 主题的下拉框不走 Meteor 基类 WMeteorDropdown 的 onRender toString() 渲染，
 * 而是把 getNameFor(value) 的显示名缓存为 RichText 后直接读取，绕过了
 * WMeteorDropdownHeaderMixin / WMeteorDropdownValueMixin。
 * 这里拦截 getNameFor 中的 value.toString()，把显示名送回标准语言文件系统（Gui.Meteor.*）。
 * 头部文本、展开列表选项、选中后刷新均经由 getNameFor，一处注入覆盖三处。
 */
@Mixin(value = WCatppuccinDropdown.class, remap = false)
public class WCatppuccinDropdownMixin {

    @Redirect(
        method = "getNameFor(Ljava/lang/Object;)Ljava/lang/String;",
        at = @At(value = "INVOKE", target = "Ljava/lang/Object;toString()Ljava/lang/String;")
    )
    private String onNameToString(Object value) {
        return gui(value.toString());
    }
}

package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.gui.tabs.Tab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.yalu.addon.TranslateAddon.gui;

/**
 * 顶部菜单 Tab 按钮标题翻译。
 * WTopBar.WTopBarButton 在 onRender() 里用 renderer.text(tab.name, ...) 直接绘制 Tab 名，
 * 不经过 WLabel / WButton。这里在读取 tab.name 时走标准语言文件系统（Gui.Meteor.*）。
 */
@Mixin(targets = "meteordevelopment.meteorclient.gui.widgets.WTopBar$WTopBarButton", remap = false)
public class WTopBarButtonMixin {

    @Redirect(method = {"onRender", "onCalculateSize"}, at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/gui/tabs/Tab;name:Ljava/lang/String;"))
    private String replaceTabName(Tab tab) {
        return gui(tab.name);
    }
}
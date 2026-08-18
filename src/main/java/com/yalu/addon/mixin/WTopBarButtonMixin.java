package com.yalu.addon.mixin;

import com.yalu.addon.util.TextReplacement;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
* 顶部菜单 Tab 按钮标题替换。
* WTopBar.WTopBarButton 在 onRender() 里用 renderer.text(tab.name, ...) 直接绘制 Tab 名，
* 不经过 WLabel / WButton，通用替换层拦不到。这里在读取 tab.name 时替换。
*/
@Mixin(targets = "meteordevelopment.meteorclient.gui.widgets.WTopBar$WTopBarButton", remap = false)
public class WTopBarButtonMixin {

@Redirect(method = {"onRender", "onCalculateSize"}, at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/gui/tabs/Tab;name:Ljava/lang/String;"))
private String replaceTabName(Tab tab) {
return TextReplacement.replace(tab.name);
}
}
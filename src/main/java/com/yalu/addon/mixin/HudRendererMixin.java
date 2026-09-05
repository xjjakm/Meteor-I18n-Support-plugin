package com.yalu.addon.mixin;

import com.yalu.addon.font_fix.FontFix;
import meteordevelopment.meteorclient.renderer.text.Font;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

/**
 * HUD 叠加层（含 HUD 编辑器左侧的可拖拽叠加层）使用 HudRenderer.text() / textWidth()
 * 渲染文本，这条路径既不走 GuiGraphicsExtractor.text()，也不走 TextRenderer.get()。
 * 此 Mixin 只负责把 Meteor 的 Font 替换成支持动态加载 CJK 的 FontFix（自定义字体「走我们的插件」），
 * 不再做通用翻译表替换（已移除）。
 */
@Mixin(value = HudRenderer.class, remap = false)
public abstract class HudRendererMixin {

    /**
     * 把 Meteor 的 Font 替换成我们支持动态加载 CJK 的 FontFix（自定义字体「走我们的插件」），
     * 从而让 HUD 叠加层的中文字符也能正常显示字形。
     */
    @Redirect(
        method = "loadFont",
        at = @At(value = "NEW", target = "Lmeteordevelopment/meteorclient/renderer/text/Font;")
    )
    private static Font useCustomFontFix(ByteBuffer buffer, int height) {
        return new FontFix(buffer, height);
    }
}
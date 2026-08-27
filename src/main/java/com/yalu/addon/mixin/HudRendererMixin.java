package com.yalu.addon.mixin;

import com.yalu.addon.font_fix.FontFix;
import com.yalu.addon.util.TextReplacement;
import meteordevelopment.meteorclient.renderer.text.Font;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

/**
 * HUD 叠加层（含 HUD 编辑器左侧的可拖拽叠加层）使用 HudRenderer.text() / textWidth()
 * 渲染文本，这条路径既不走 GuiGraphicsExtractor.text()，也不走 TextRenderer.get()，
 * 因此之前完全绕过了 TextReplacement 通用翻译。这里在渲染前对文本做一次替换。
 */
@Mixin(value = HudRenderer.class, remap = false)
public abstract class HudRendererMixin {

    @ModifyArg(
        method = "text(Ljava/lang/String;DDLmeteordevelopment/meteorclient/utils/render/color/Color;ZD)D",
        at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/renderer/text/Font;render(Lmeteordevelopment/meteorclient/renderer/MeshBuilder;Ljava/lang/String;DDLmeteordevelopment/meteorclient/utils/render/color/Color;D)D"),
        index = 1
    )
    private static String translateHudRender(String text) {
        return TextReplacement.replace(text);
    }

    @ModifyArg(
        method = "textWidth(Ljava/lang/String;ZD)D",
        at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/renderer/text/Font;getWidth(Ljava/lang/String;I)D"),
        index = 0
    )
    private static String translateHudWidth(String text) {
        return TextReplacement.replace(text);
    }

    /**
     * 把 Meteor 的 Font 替换成我们支持动态加载 CJK 的 FontFix（自定义字体「走我们的插件」），
     * 从而让 HUD 叠加层的英文字符翻译成中文后也能正常显示字形。
     */
    @Redirect(
        method = "loadFont",
        at = @At(value = "NEW", target = "Lmeteordevelopment/meteorclient/renderer/text/Font;")
    )
    private static Font useCustomFontFix(ByteBuffer buffer, int height) {
        return new FontFix(buffer, height);
    }
}
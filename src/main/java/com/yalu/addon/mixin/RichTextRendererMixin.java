package com.yalu.addon.mixin;

import com.yalu.addon.font_fix.FontFix;
import me.pindour.catppuccin.api.text.FontStyle;
import me.pindour.catppuccin.api.text.RichTextSegment;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 替换 RichTextRenderer 中的 Font 为 FontFix，使 Catppuccin 主题支持 CJK 动态字符加载。
 * 与 CustomTextRendererMixin 采用相同策略。
 */
@Mixin(targets = "me.pindour.catppuccin.renderer.text.RichTextRenderer", remap = false)
public abstract class RichTextRendererMixin implements TextRenderer {

    @Shadow @Final private MeshBuilder mesh;
    @Shadow private FontStyle currentStyle;
    @Shadow private int currentFontIndex;
    @Shadow private boolean building;
    @Shadow private boolean scaleOnly;
    @Shadow private double fontScale;
    @Shadow private double scale;

    @Unique private static final Color SHADOW_COLOR = new Color(60, 60, 60, 180);

    @Unique private FontFix[] fontsFixRegular;
    @Unique private FontFix[] fontsFixBold;
    @Unique private FontFix[] fontsFixItalic;
    @Unique private FontFix fontFix;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(FontFace fontFace, CallbackInfo ci) throws IOException {
        ByteBuffer buffer = fontFace.readToDirectByteBuffer();

        fontsFixRegular = new FontFix[5];
        fontsFixBold = new FontFix[5];
        fontsFixItalic = new FontFix[5];

        for (int i = 0; i < 5; i++) {
            int height = (int) Math.round(27 * ((i * 0.5) + 1));
            fontsFixRegular[i] = new FontFix(buffer, height);
            fontsFixBold[i] = new FontFix(buffer, height);
            fontsFixItalic[i] = new FontFix(buffer, height);
        }
    }

    @Unique
    private FontFix[] getFontsFix(FontStyle style) {
        return switch (style) {
            case BOLD -> fontsFixBold;
            case ITALIC -> fontsFixItalic;
            default -> fontsFixRegular;
        };
    }

    @Unique
    private FontFix pickFontFix(FontStyle style, int index) {
        FontFix[] fonts = getFontsFix(style);
        if (index < fonts.length) return fonts[index];
        return fonts[0];
    }

    /**
     * @author yalu
     * @reason 替换 Font 为 FontFix 以支持 CJK
     */
    @Overwrite
    public void begin(double scale, boolean scaleOnly, boolean big) {
        if (building) throw new RuntimeException("RichTextRenderer.begin() called twice");
        if (!scaleOnly) mesh.begin();

        int scaleIndex;
        if (scale >= 3) scaleIndex = 4;
        else if (scale >= 2.5) scaleIndex = 3;
        else if (scale >= 2) scaleIndex = 2;
        else if (scale >= 1.5) scaleIndex = 1;
        else scaleIndex = 0;

        currentFontIndex = scaleIndex;
        fontFix = pickFontFix(currentStyle, currentFontIndex);

        building = true;
        this.scaleOnly = scaleOnly;
        fontScale = fontFix.getHeight() / 27.0;
        this.scale = 1 + (scale - fontScale) / fontScale;
    }

    /**
     * @author yalu
     * @reason 替换 Font 为 FontFix 以支持 CJK
     */
    @Overwrite
    public double getWidth(RichTextSegment segment, int length) {
        FontFix[] fonts = getFontsFix(segment.getStyle());
        FontFix font = building ? (currentFontIndex < fonts.length ? fonts[currentFontIndex] : fonts[0]) : fonts[0];

        double width = font.getWidth(segment.getText(), length);
        if (segment.hasShadow()) width += 1;

        return width * (segment.getScale() / 1.5);
    }

    /**
     * @author yalu
     * @reason 替换 Font 为 FontFix 以支持 CJK
     */
    @Overwrite
    public double getHeight(RichTextSegment segment) {
        FontFix[] fonts = getFontsFix(segment.getStyle());
        FontFix font = building ? (currentFontIndex < fonts.length ? fonts[currentFontIndex] : fonts[0]) : fonts[0];

        return (font.getHeight() + 1 + (segment.hasShadow() ? 1 : 0)) * segment.getScale() / 1.5;
    }

    /**
     * @author yalu
     * @reason 替换 Font 为 FontFix 以支持 CJK
     */
    @Overwrite
    public double getHeight(boolean shadow) {
        FontFix font = building ? fontFix : fontsFixRegular[0];
        return (font.getHeight() + 1 + (shadow ? 1 : 0)) * scale / 1.5;
    }

    /**
     * @author yalu
     * @reason 替换 Font 为 FontFix 以支持 CJK
     */
    @Overwrite
    public double render(String text, double x, double y, Color color, boolean shadow) {
        boolean wasBuilding = building;
        if (!wasBuilding) begin(1, false, false);

        double renderScale = scale / 1.5;
        double width;

        if (shadow) {
            int originalShadowAlpha = SHADOW_COLOR.a;
            SHADOW_COLOR.a = (int) (color.a / 255.0 * originalShadowAlpha);

            double shadowOffset = fontScale * renderScale;

            width = fontFix.render(mesh, text, x + shadowOffset, y + shadowOffset, SHADOW_COLOR, renderScale);
            fontFix.render(mesh, text, x, y, color, renderScale);

            SHADOW_COLOR.a = originalShadowAlpha;
        } else {
            width = fontFix.render(mesh, text, x, y, color, renderScale);
        }

        if (!wasBuilding) end();
        return width;
    }

    /**
     * @author yalu
     * @reason 替换 Font 为 FontFix 以支持 CJK
     */
    @Overwrite
    public void end() {
        if (!building) throw new RuntimeException("end() called without calling begin()");

        if (!scaleOnly) {
            mesh.end();
            MeshRenderer.begin()
                    .attachments(Minecraft.getInstance().getMainRenderTarget())
                    .pipeline(MeteorRenderPipelines.UI_TEXT)
                    .mesh(mesh)
                    .sampler("u_Texture", fontFix.texture.getTextureView(), fontFix.texture.getSampler())
                    .end();
        }

        building = false;
        scale = 1;
    }

    /**
     * @author yalu
     * @reason 替换 Font 为 FontFix 以支持 CJK，更新 fontFix 而非 currentFont
     */
    @Overwrite
    private void setStyleInternal(FontStyle style) {
        this.currentStyle = style;
        if (building) {
            fontFix = pickFontFix(style, currentFontIndex);
        }
    }
}

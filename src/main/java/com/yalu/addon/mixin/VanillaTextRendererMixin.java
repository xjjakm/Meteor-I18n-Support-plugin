package com.yalu.addon.mixin;

import com.yalu.addon.util.QueuedText;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(value = VanillaTextRenderer.class, remap = false)
public abstract class VanillaTextRendererMixin {

    @Shadow
    @Final
    private MultiBufferSource.BufferSource immediate;

    @Shadow
    public double scale;

    @Shadow
    private boolean building;

    @Shadow
    private double alpha;

    @Unique
    private final List<QueuedText> cjkTextQueue = new ArrayList<>();

    @Overwrite
    public void begin(double scale, boolean scaleOnly, boolean big) {
        if (building) throw new RuntimeException("VanillaTextRenderer.begin() called twice");

        this.scale = scale * 2;
        this.building = true;
        cjkTextQueue.clear();
    }

    @Overwrite
    public double render(String text, double x, double y, Color color, boolean shadow) {
        boolean wasBuilding = building;
        if (!wasBuilding) begin(1, false, false);

        x += 0.5 * scale;
        y += 0.5 * scale;

        int packedColor = new Color(
            color.r, color.g, color.b,
            (int) (((double) color.a / 255 * alpha) * 255)
        ).getPacked();

        cjkTextQueue.add(new QueuedText(
            text,
            (float) (x / scale),
            (float) (y / scale),
            packedColor,
            shadow
        ));

        double width = mc.font.width(text) + (shadow ? 1 : 0);

        if (!wasBuilding) end(null);
        return (x / scale + width - 1) * scale;
    }

    public void end(PoseStack matrices) {
        if (!building) throw new RuntimeException("VanillaTextRenderer.end() called without calling begin()");

        Matrix4f baseMatrix = matrices != null ? matrices.last().pose() : new Matrix4f();

        Matrix4f finalMatrix = new Matrix4f(baseMatrix);
        finalMatrix.scale((float) scale, (float) scale, 1);

        GlStateManager._disableDepthTest();

        for (QueuedText qt : cjkTextQueue) {
            mc.font.drawInBatch(
                qt.text(),
                qt.x(),
                qt.y(),
                qt.color(),
                qt.shadow(),
                finalMatrix,
                immediate,
                Font.DisplayMode.NORMAL,
                0,
                0xF000F0
            );
        }

        cjkTextQueue.clear();
        immediate.endBatch();

        GlStateManager._enableDepthTest();

        this.scale = 2;
        this.building = false;
    }
}

package com.yalu.addon.mixin;

import com.yalu.addon.util.CJKFontSupport;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.renderer.text.Font;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackRange;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(value = Font.class, remap = false)
public abstract class MixinFont {

    @Unique
    private static final int CJK_SIZE = 8192;

    @Shadow
    @Final
    @Mutable
    private Object texture;

    @Shadow
    @Final
    @Mutable
    private Int2ObjectOpenHashMap<Object> charMap = new Int2ObjectOpenHashMap<>();

    @Unique
    private static Constructor<?> cjkCharDataCtor;

    @Inject(method = "<init>(Ljava/nio/ByteBuffer;I)V", at = @At("RETURN"))
    private void onInit(ByteBuffer buffer, int height, CallbackInfo ci) {
        CJKFontSupport.loadCharset();
        int[] extraCPs = CJKFontSupport.getExtraCodepoints();

        if (this.charMap == null) {
            this.charMap = new Int2ObjectOpenHashMap<>();
        }
        charMap.clear();

        if (texture != null) {
            try {
                texture.getClass().getMethod("close").invoke(texture);
            } catch (Exception e) {
            }
        }

        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        STBTruetype.stbtt_InitFont(fontInfo, buffer);

        ByteBuffer bitmap = BufferUtils.createByteBuffer(CJK_SIZE * CJK_SIZE);

        STBTTPackedchar.Buffer[] cdata = {
            STBTTPackedchar.create(95),
            STBTTPackedchar.create(96),
            STBTTPackedchar.create(128),
            STBTTPackedchar.create(144),
            STBTTPackedchar.create(256),
            STBTTPackedchar.create(1),
            STBTTPackedchar.create(extraCPs.length)
        };

        IntBuffer cpBuf = null;
        if (extraCPs.length > 0) {
            cpBuf = BufferUtils.createIntBuffer(extraCPs.length);
            cpBuf.put(extraCPs).flip();
        }

        STBTTPackContext packContext = STBTTPackContext.create();
        STBTruetype.stbtt_PackBegin(packContext, bitmap, CJK_SIZE, CJK_SIZE, 0, 1);

        STBTTPackRange.Buffer packRange = STBTTPackRange.create(cdata.length);
        packRange.put(STBTTPackRange.create().set(height, 32,   null, 95,  cdata[0], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 160,  null, 96,  cdata[1], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 256,  null, 128, cdata[2], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 880,  null, 144, cdata[3], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 1024, null, 256, cdata[4], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 8734, null, 1,   cdata[5], (byte) 2, (byte) 2));
        if (cpBuf != null) {
            packRange.put(STBTTPackRange.create().set(height, 0, cpBuf,
                extraCPs.length, cdata[6], (byte) 2, (byte) 2));
        } else {
            packRange.put(STBTTPackRange.create().set(height, 0, null, 0, cdata[6], (byte) 2, (byte) 2));
        }
        packRange.flip();

        STBTruetype.stbtt_PackFontRanges(packContext, buffer, 0, packRange);
        STBTruetype.stbtt_PackEnd(packContext);

        GpuTexture newTexture = RenderSystem.getDevice().createTexture(
            "CJK Font Texture",
            GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
            TextureFormat.RED8,
            CJK_SIZE,
            CJK_SIZE,
            1,
            1
        );

        try {
            bitmap.rewind();
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(
                newTexture,
                bitmap,
                com.mojang.blaze3d.platform.NativeImage.Format.LUMINANCE,
                0,
                0,
                0,
                0,
                CJK_SIZE,
                CJK_SIZE
            );
        } catch (Exception e) {
        }

        texture = newTexture;

        for (int i = 0; i < 6; i++) {
            STBTTPackedchar.Buffer cbuf = cdata[i];
            int offset = packRange.get(i).first_unicode_codepoint_in_range();
            for (int j = 0; j < cbuf.capacity(); j++) {
                charMap.put(j + offset, cjkToCharData(cbuf.get(j), CJK_SIZE));
            }
        }

        if (cpBuf != null && cdata[6].capacity() > 0) {
            STBTTPackedchar.Buffer cbuf = cdata[6];
            cpBuf.rewind();
            for (int j = 0; j < cbuf.capacity(); j++) {
                int cp = cpBuf.get(j);
                charMap.put(cp, cjkToCharData(cbuf.get(j), CJK_SIZE));
            }
        }
    }

    @Unique
    private static Object cjkToCharData(STBTTPackedchar pc, int size) {
        float ipw = 1f / size;
        float iph = 1f / size;
        try {
            if (cjkCharDataCtor == null) {
                Class<?> cdClass = Class.forName("meteordevelopment.meteorclient.renderer.text.Font$CharData");
                cjkCharDataCtor = cdClass.getDeclaredConstructor(
                    float.class, float.class, float.class, float.class,
                    float.class, float.class, float.class, float.class, float.class);
                cjkCharDataCtor.setAccessible(true);
            }
            return cjkCharDataCtor.newInstance(
                pc.xoff(), pc.yoff(), pc.xoff2(), pc.yoff2(),
                pc.x0() * ipw, pc.y0() * iph, pc.x1() * ipw, pc.y1() * iph,
                pc.xadvance()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create CharData", e);
        }
    }
}

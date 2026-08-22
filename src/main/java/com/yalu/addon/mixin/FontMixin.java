package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.renderer.text.Font;
import org.lwjgl.stb.STBTTPackRange;
import org.lwjgl.stb.STBTTPackedchar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = Font.class ,remap = false)
public abstract class FontMixin {
    /** CJK 起始码点 0x4E00,数量 20976(与成功源码一致) */
    @Unique
    private static final int CJK_COUNT = 20976;

    @Unique
    private static final int CJK_START = 0x4E00;

    @Unique
    private static STBTTPackedchar.Buffer cjkCharData;

    /**
     * 扩 cdata 数组。注入点选 STBTTPackRange.create(I) 调用处:
     * 该调用点的参数正是 cdata.length,即 cdata 在此处被读取,
     * 是 @ModifyVariable 定位局部变量的可靠锚点。
     */
    @ModifyVariable(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/stb/STBTTPackRange;create(I)Lorg/lwjgl/stb/STBTTPackRange$Buffer;"), name = "cdata")
    private STBTTPackedchar.Buffer[] addCjkCdata(STBTTPackedchar.Buffer[] cdata){
        cjkCharData = STBTTPackedchar.create(CJK_COUNT);
        STBTTPackedchar.Buffer[] templist = new STBTTPackedchar.Buffer[cdata.length + 1];
        System.arraycopy(cdata, 0, templist, 0, cdata.length);
        templist[cdata.length] = cjkCharData;
        return templist;
    }

    /**
     * 扩 packRange buffer:复制原 6 个 range,末尾追加 CJK range。
     * 注入点选 stbtt_PackFontRanges 调用处:packRange 在此处被读取作为参数。
     */
    @ModifyVariable(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/stb/STBTruetype;stbtt_PackFontRanges(Lorg/lwjgl/stb/STBTTPackContext;Ljava/nio/ByteBuffer;ILorg/lwjgl/stb/STBTTPackRange$Buffer;)Z"), name = "packRange")
    private STBTTPackRange.Buffer addPackRange(STBTTPackRange.Buffer packRange){
        int count = packRange.limit(); // 原 6 个有效 range
        STBTTPackRange.Buffer newRange = STBTTPackRange.create(count + 1); // 容量 7

        // 复制原 6 个 range
        for (int i = 0; i < count; i++) {
            newRange.put(STBTTPackRange.create().set(packRange.get(i)));
        }

        // 追加第 7 个:CJK range,数据写入 cjkCharData(cdata[6])
        newRange.put(STBTTPackRange.create().set(
            packRange.get(0).font_size(), // 与其它区块同高度
            CJK_START,                    // 0x4E00
            null,                         // 字体名
            CJK_COUNT,                    // 20976
            cjkCharData,                  // ← 对应 cdata[6]
            (byte) 2, (byte) 2
        ));

        newRange.flip(); // limit = 7
        return newRange;
    }

    // 1) size 本身(4 处 2048 → 4096)
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 2048))
    private int changeSize(int value) {
        return 4096;
    }

    // 2) size*size = 4194304 → 4096*4096 = 16777216(bitmap 分配)
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 4194304))
    private int changeBitmapSize(int value) {
        return 4096 * 4096;
    }

    // 3) 1f/size = 4.8828125E-4f → 1f/4096 = 2.44140625E-4f(uv 坐标)
    @ModifyConstant(method = "<init>", constant = @Constant(floatValue = 4.8828125E-4f))
    private float changeUvScale(float value) {
        return 1f / 4096;
    }
}

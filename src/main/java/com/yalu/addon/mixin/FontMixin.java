package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.renderer.text.Font;
import org.lwjgl.stb.STBTTPackRange;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.CustomBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = Font.class ,remap = false)
public abstract class FontMixin {
    /** CJK 起始码点 0x4E00,数量 6000(常用字+次常用,控制位图占用) */
    @Unique
    private static final int CJK_COUNT = 20976;

    @Unique
    private static final int CJK_START = 0x4E00;

    @Unique
    private static STBTTPackedchar.Buffer cjkCharData;


    @ModifyVariable(method = "<init>", at = @At(value = "INVOKE", target ="Lorg/lwjgl/stb/STBTruetype;stbtt_PackBegin(Lorg/lwjgl/stb/STBTTPackContext;Ljava/nio/ByteBuffer;IIII)Z"), name = "cdata")
    private STBTTPackedchar.Buffer[] addCjkCdata(STBTTPackedchar.Buffer[] cdata){
        // 1) 创建 CJK range 数据缓冲区
        cjkCharData = STBTTPackedchar.create(CJK_COUNT);
        STBTTPackedchar.Buffer[] templist = new STBTTPackedchar.Buffer[cdata.length + 1];
        System.arraycopy(cdata, 0, templist, 0, cdata.length);
        templist[cdata.length] = cjkCharData;
        return templist;
    }


    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/stb/STBTTPackRange$Buffer;flip()Lorg/lwjgl/system/CustomBuffer;"))
    private CustomBuffer flipWithCjk(STBTTPackRange.Buffer packRange){
        // 追加 CJK range(此时 position=6,容量 7)
        packRange.put(STBTTPackRange.create().set(
            packRange.get(0).font_size(), // 与其它区块同高度
            CJK_START,                    // 0x4E00
            null,                         // 字体名
            CJK_COUNT,                    // 20976
            cjkCharData,                  // ← 对应 cdata[6]
            (byte) 2, (byte) 2
        ));

        // 调用原 flip()
        return packRange.flip();
    }

    // 1) size 本身(4 处 2048 → 8192)
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 2048))
    private int changeSize(int value) {
        return 8192;
    }

    // 2) size*size = 4194304 → 8192*8192 = 67108864(bitmap 分配)
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 4194304))
    private int changeBitmapSize(int value) {
        return 8192 * 8192;
    }

    // 3) 1f/size = 4.8828125E-4f → 1f/8192 = 1.220703125E-4f(uv 坐标)
    @ModifyConstant(method = "<init>", constant = @Constant(floatValue = 4.8828125E-4f))
    private float changeUvScale(float value) {
        return 1f / 8192;
    }
}

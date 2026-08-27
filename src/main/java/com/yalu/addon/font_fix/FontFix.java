package com.yalu.addon.font_fix;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.text.Font;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class FontFix extends Font {
private final int height;
private final float scale;
private final float ascent;
private final Int2ObjectOpenHashMap<CharData> charMap = new Int2ObjectOpenHashMap<>();
private final static int SIZE = 2048;

private final ByteBuffer buffer;
    private final ByteBuffer bitmap;
private final STBTTPackContext packContext;

private long loadTimer = 0;
private int loadCount = 0;
/**
 * 自定义字体中 CJK 字符数量多，7/100ms 的上限会让中文叠加层长时间空帧。
 * 调整为：每 100ms 内最多打包 128 个新字符，足以覆盖一次 HUD 刷新出现的所有 CJK 字符。
 */
private static final int LOAD_SPEED_LIMIT = 128;

public FontFix(ByteBuffer buffer, int height) {
super(buffer, height);
this.buffer = buffer;
this.height = height;

    STBTTFontinfo fontInfo = STBTTFontinfo.create();
STBTruetype.stbtt_InitFont(fontInfo, buffer);

bitmap = BufferUtils.createByteBuffer(SIZE * SIZE);
packContext = STBTTPackContext.create();
STBTruetype.stbtt_PackBegin(packContext, bitmap, SIZE, SIZE, 0, 1);

scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height);

try (MemoryStack stack = MemoryStack.stackPush()) {
IntBuffer a = stack.mallocInt(1);
STBTruetype.stbtt_GetFontVMetrics(fontInfo, a, null, null);
this.ascent = a.get(0);
}

preloadAsciiCharacters();
}

private void preloadAsciiCharacters() {
STBTTPackedchar.Buffer cdata = STBTTPackedchar.create(128);
STBTTPackRange.Buffer pr = STBTTPackRange.create(1);
pr.put(STBTTPackRange.create().set(height, 32, null, 128, cdata, (byte) 2, (byte) 2));
pr.flip();

STBTruetype.stbtt_PackFontRanges(packContext, buffer, 0, pr);

for (int i = 0; i < cdata.capacity(); i++) putCharData(i + 32, cdata.get(i));
createTexture();
}

private void loadCharacter(List<Integer> codePoints) {
if (System.currentTimeMillis() - loadTimer > 100) { loadTimer = System.currentTimeMillis(); loadCount = 0; }
if (loadCount >= LOAD_SPEED_LIMIT) return;
for (Integer cp : codePoints) loadCharacter(cp);
createTexture();
loadCount++;
}

private void loadCharacter(int codePoint) {
if (charMap.containsKey(codePoint)) return;

STBTTPackedchar.Buffer cdata = STBTTPackedchar.create(1);
STBTTPackRange.Buffer pr = STBTTPackRange.create(1);
pr.put(STBTTPackRange.create().set(height, codePoint, null, 1, cdata, (byte) 2, (byte) 2));
pr.flip();

STBTruetype.stbtt_PackFontRanges(packContext, buffer, 0, pr);
putCharData(codePoint, cdata.get(0));
}

private void putCharData(int codePoint, STBTTPackedchar pc) {
float ipw = 1f / SIZE, iph = 1f / SIZE;
charMap.put(codePoint, new CharData(
pc.xoff(), pc.yoff(), pc.xoff2(), pc.yoff2(),
pc.x0() * ipw, pc.y0() * iph, pc.x1() * ipw, pc.y1() * iph,
pc.xadvance()
));
}

private void createTexture() {
// 复用父类 Font 的 texture：upload 会把字形位图整体（覆盖式）写入纹理，
// 因此 HUD 通过 font.texture 读到的父纹理始终是包含 CJK 的最新字形图。
texture.upload(bitmap);
}

public double getWidth(String string, int length) {
double width = 0;
tryLoadString(string);
int i = 0;
int processed = 0;
float fallbackWidth = height * 0.5f; // 尚未加载字形时，按字符尺寸的 0.5 倍估算宽度以避免布局跳变
while (i < string.length() && processed < length) {
    int cp = string.codePointAt(i);
    int chars = Character.charCount(cp);
    if (processed + chars <= length) {
        CharData c = charMap.get(cp);
        if (c != null) width += c.xAdvance;
        else width += fallbackWidth;
    }
    i += chars;
    processed += chars;
}
return width;
}

public int getHeight() { return height; }

/**
 * 遍历字符串中尚未缓存字形的码位，加入待加载队列。
 * 与旧实现不同：该方法不再影响上层是否跳过渲染，也就是说，
 * 即便当前帧触发了新字符打包，已缓存的字符仍然会被正常绘制/宽度测量。
 */
private void tryLoadString(String s) {
    List<Integer> pts = null;
    int i = 0;
    while (i < s.length()) {
        int cp = s.codePointAt(i);
        if (!charMap.containsKey(cp)) {
            if (pts == null) pts = new ArrayList<>();
            pts.add(cp);
        }
        i += Character.charCount(cp);
    }
    if (pts != null) loadCharacter(pts);
}

public double render(MeshBuilder mesh, String string, double x, double y, Color color, double s) {
tryLoadString(string);
y += ascent * this.scale * s;

int len = string.length();
mesh.ensureCapacity(len * 4, len * 6);
int i = 0;
while (i < len) {
    int cp = string.codePointAt(i);
    int chars = Character.charCount(cp);
    CharData c = charMap.get(cp);
    if (c != null) {
        mesh.quad(
mesh.vec2(x + c.x0 * s, y + c.y0 * s).vec2(c.u0, c.v0).color(color).next(),
mesh.vec2(x + c.x0 * s, y + c.y1 * s).vec2(c.u0, c.v1).color(color).next(),
mesh.vec2(x + c.x1 * s, y + c.y1 * s).vec2(c.u1, c.v1).color(color).next(),
mesh.vec2(x + c.x1 * s, y + c.y0 * s).vec2(c.u1, c.v0).color(color).next()
);
x += c.xAdvance * s;
    }
    i += chars;
}
return x;
}

private record CharData(float x0, float y0, float x1, float y1,
float u0, float v0, float u1, float v1,
float xAdvance) {}
}

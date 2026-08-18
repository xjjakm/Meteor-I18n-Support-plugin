package com.yalu.addon.mixin.HudRenderer;

import com.yalu.addon.font_fix.FontFix;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;

public   class FontHolder {
public final FontFix font;
public boolean visited;

private MeshBuilder mesh;

public FontHolder(FontFix font) {
this.font = font;
}

public MeshBuilder getMesh() {
if (mesh == null) mesh = new MeshBuilder(MeteorRenderPipelines.UI_TEXT);
if (!mesh.isBuilding()) mesh.begin();
return mesh;
}

public void destroy() {
font.texture.close();
}
}
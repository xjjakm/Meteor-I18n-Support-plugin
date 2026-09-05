package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static com.yalu.addon.TranslateAddon.gui;

/**
 * 模块列表项标题翻译。
 * WMeteorModule 在 onRender() 里用 renderer.text(title, ...) 直接绘制模块名，
 * 不经过 WLabel / WButton。这里在构造器把 title 参数走标准语言文件系统（Gui.Meteor.*）。
 * 注意：<init> 中 super() 之前的注入 handler 必须为 static，否则 Mixin 报
 * "handler before super() invocation must be static"。
 */
@Mixin(value = WMeteorModule.class, remap = false)
public class WMeteorModuleMixin {

    @ModifyVariable(method = "<init>(Lmeteordevelopment/meteorclient/systems/modules/Module;Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
    private static String onInit(String title) {
        return gui(title);
    }
}
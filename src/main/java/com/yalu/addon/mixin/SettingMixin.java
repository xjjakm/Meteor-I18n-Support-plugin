package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

import static com.yalu.addon.TranslateAddon.MC;
import static com.yalu.addon.TranslateAddon.TRANSLATOR;

@Mixin(value = Setting.class,remap = false)
public class SettingMixin {

    @Unique MeteorAddon addon = null;

    @Mutable
    @Final
    @Shadow
    public String title;
    @Mutable
    @Final
    @Shadow
    public String description;
    @Inject(method = "<init>",at = @At("TAIL"))
    public void init(String name, String description, Object defaultValue, Consumer onChanged, Consumer onModuleActivated, IVisible visible, CallbackInfo ci){
        if (MC == null || MC.getResourceManager() == null) return;
        TRANSLATOR.reload(MC.getResourceManager());

        String classname = this.getClass().getName();
        for (MeteorAddon addon : AddonManager.ADDONS) {
            if (classname.startsWith(addon.getPackage())) {
                this.addon = addon;
            }
        }

        String PackageName = this.addon.name.replace(" ", "-");
        if (PackageName.equals("Meteor-Client")){//历史遗留问题,我不想改语言文件
            PackageName = "Meteor";
        }


        String SettingKey = "Setting." + PackageName + "." + name;
        String DescriptionKey = "Setting." + PackageName + "." + name + ".Description";
        this.title = TRANSLATOR.Translate(SettingKey,name);
        this.description = TRANSLATOR.Translate(DescriptionKey,description);
    }
}

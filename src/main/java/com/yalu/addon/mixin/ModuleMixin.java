package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.yalu.addon.TranslateAddon.MC;
import static com.yalu.addon.TranslateAddon.TRANSLATOR;

@Mixin(value = Module.class,remap = false,priority = 999)
public abstract class ModuleMixin {
    @Final
    @Mutable
    @Shadow
    public String title;
    @Mutable
    @Shadow
    @Final
    public String description;
    @Shadow
    @Final
    public MeteorAddon addon;
    @Unique
    public String name;
    @Inject(method = "<init>*", at = @At("RETURN"))
    public void onInit(CallbackInfo ci){
        TRANSLATOR.reload(MC.getResourceManager());
        String PackageName = this.addon.name.replace(" ", "-");
        if (PackageName.equals("Meteor-Client")){//历史遗留问题,不想改语言文件
            PackageName = "Meteor";
        }
        String ModuleKey = "Module." + PackageName + "." + this.name;
        String DescriptionKey = "Module." + PackageName + "." + this.name + ".Description";
        this.title = TRANSLATOR.Translate(ModuleKey, this.name);
        this.description = TRANSLATOR.Translate(DescriptionKey,this.description);
    }
}

package com.yalu.addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
    @Unique
    public String name;
    @Inject(method = "<init>*", at = @At("RETURN"))
    public void onInit(CallbackInfo ci){
        TRANSLATOR.reload(MC.getResourceManager());
        String ModuleKey = "Module.Meteor." + this.name;
        String DescriptionKey = "Module.Meteor." + this.name + ".Description";
        this.title = TRANSLATOR.Translate(ModuleKey, this.name);
        this.description = TRANSLATOR.Translate(DescriptionKey,this.description);
    }

    @Redirect(method = "sendToggledMsg", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/utils/player/ChatUtils;sendMsg(ILnet/minecraft/ChatFormatting;Ljava/lang/String;[Ljava/lang/Object;)V"))
    private void redirectToggledMsg(int id, ChatFormatting color, String message, Object... args) {
        Object[] newArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String s) {
                if (s.equals(ChatFormatting.GREEN + "on")) {
                    newArgs[i] = ChatFormatting.GREEN + "开启";
                } else if (s.equals(ChatFormatting.RED + "off")) {
                    newArgs[i] = ChatFormatting.RED + "关闭";
                } else {
                    newArgs[i] = s;
                }
            } else {
                newArgs[i] = args[i];
            }
        }
        ChatUtils.sendMsg(id, color, message, newArgs);
    }
}

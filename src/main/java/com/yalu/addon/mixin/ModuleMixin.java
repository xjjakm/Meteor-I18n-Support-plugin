package com.yalu.addon.mixin;

import com.yalu.addon.util.NameCache;
import com.yalu.addon.util.TransUtil;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
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
    @Shadow
    @Final
    public MeteorAddon addon;
    @Shadow
    @Final
    public String name;
    @Final
    @Shadow
    public Settings settings;

    @Inject(method = "<init>*", at = @At("RETURN"))
    public void onInit(CallbackInfo ci){
        if (this.settings != null) {
            for (SettingGroup group : this.settings.groups) {
                NameCache.group(group);
            }
        }
        if (MC == null || MC.getResourceManager() == null) return;
        TRANSLATOR.reload(MC.getResourceManager());
        String PackageName = this.addon.name.replace(" ", "-");
        if (PackageName.equals("Meteor-Client")){
            PackageName = "Meteor";
        }
        String ModuleKey = "Module." + PackageName + "." + this.name;
        String DescriptionKey = "Module." + PackageName + "." + this.name + ".Description";
        this.title = TRANSLATOR.Translate(ModuleKey, this.name);
        this.description = TRANSLATOR.Translate(DescriptionKey,this.description);

        if (this.settings != null) {
            for (SettingGroup group : this.settings.groups) {
                String originalGroupName = NameCache.group(group);
                String groupKey = "Module." + PackageName + "." + this.name + "." + TransUtil.baseFormat(originalGroupName) + ".name";
                String translatedGroup = TRANSLATOR.Translate(groupKey, originalGroupName);
                if (!translatedGroup.equals(originalGroupName)) {
                    ((SettingGroupAccessor) group).setName(translatedGroup);
                }
            }
        }
    }

    @Unique
    private static boolean isZhCn() {
        try {
            return MC.getLanguageManager().getSelected().equals("zh_cn");
        } catch (Exception ignored) {
            return false;
        }
    }

    @Unique
    private static boolean isZhTw() {
        try {
            return MC.getLanguageManager().getSelected().equals("zh_tw");
        } catch (Exception ignored) {
            return false;
        }
    }

    @Redirect(method = "sendToggledMsg", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/utils/player/ChatUtils;sendMsg(ILnet/minecraft/ChatFormatting;Ljava/lang/String;[Ljava/lang/Object;)V"))
    private void redirectToggledMsg(int id, ChatFormatting color, String message, Object... args) {
        Object[] newArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String s) {
                if (isZhCn()) {
                    if (s.equals(ChatFormatting.GREEN + "on")) {
                        newArgs[i] = ChatFormatting.GREEN + "开启";
                        continue;
                    } else if (s.equals(ChatFormatting.RED + "off")) {
                        newArgs[i] = ChatFormatting.RED + "关闭";
                        continue;
                    }
                } else if (isZhTw()) {
                    if (s.equals(ChatFormatting.GREEN + "on")) {
                        newArgs[i] = ChatFormatting.GREEN + "開啟";
                        continue;
                    } else if (s.equals(ChatFormatting.RED + "off")) {
                        newArgs[i] = ChatFormatting.RED + "關閉";
                        continue;
                    }
                }
                newArgs[i] = s;
            } else {
                newArgs[i] = args[i];
            }
        }
        ChatUtils.sendMsg(id, color, message, newArgs);
    }
}

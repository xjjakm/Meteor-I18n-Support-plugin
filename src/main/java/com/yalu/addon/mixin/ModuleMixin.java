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

import static com.yalu.addon.TranslateAddon.TRANSLATOR;
import static com.yalu.addon.TranslateAddon.getMC;

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
        if (getMC() == null || getMC().getResourceManager() == null) return;
        TRANSLATOR.reload(getMC().getResourceManager());
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
    private static String translateToggleArg(String s) {
        // 参数形如 ChatFormatting.GREEN + "on"（§aon）或 ChatFormatting.RED + "off"（§coff）。
        // 去掉颜色前缀后按标准语言文件（meteori18n.toggle.on/off）翻译，替代硬编码中文字符串。
        if (s.length() >= 2 && s.charAt(0) == '§') {
            ChatFormatting fmt = ChatFormatting.getByCode(s.charAt(1));
            String word = s.substring(2);
            String key = null;
            if ("on".equals(word)) key = "meteori18n.toggle.on";
            else if ("off".equals(word)) key = "meteori18n.toggle.off";
            if (fmt != null && key != null) return fmt + TRANSLATOR.get(key, word);
        }
        return s;
    }

    @Redirect(method = "sendToggledMsg", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/utils/player/ChatUtils;sendMsg(ILnet/minecraft/ChatFormatting;Ljava/lang/String;[Ljava/lang/Object;)V"))
    private void redirectToggledMsg(int id, ChatFormatting color, String message, Object... args) {
        // 翻译模板串里的静态英文词（message, 如 "Toggled (highlight)%s(default) %s(default)." 中的 "Toggled"）
        String translatedMessage = translateToggleTemplate(message);

        Object[] newArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String s) {
                newArgs[i] = translateToggleArg(s);
            } else {
                newArgs[i] = args[i];
            }
        }
        ChatUtils.sendMsg(id, color, translatedMessage, newArgs);
    }

    /** 翻译模块切换消息模板，保留 Meteor 的 (highlight)/(default) 格式化标记和 %s 占位符。 */
    private static String translateToggleTemplate(String message) {
        if (message == null) return null;
        if ("Toggled (highlight)%s(default) %s(default).".equals(message)) {
            String tpl = TRANSLATOR.get("meteori18n.toggled", null);
            if (tpl != null) return tpl;
        }
        return message;
    }
}

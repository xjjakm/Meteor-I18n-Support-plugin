package com.yalu.addon;

import com.mojang.logging.LogUtils;
import com.yalu.addon.modules.AboutThisPlugin;
import com.yalu.addon.util.TextReplacement;
import com.yalu.addon.util.UniversalLangLoader;
import com.yalu.addon.util.UnknownDump;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.util.Set;

public class TranslateAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final String VERSION = "fork-1.0.4";
    public static final Category CATEGORY = new Category("I18n");
    public static final Minecraft MC = MeteorClient.mc;
    public static final Translator TRANSLATOR = new Translator();

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor I18n Support Addon");

        // Load universal text replacement table early so that static strings
        // created during Meteor's PostInit (e.g. ChatUtils prefix) can be translated.
        UniversalLangLoader.reload();
        TextReplacement.setEnabled(true);

        // Modules
        Modules.get().add(new AboutThisPlugin());

        // Always dump collected unknown text when the client stops, even if
        // module deactivate is not reliably called during shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread(this::dumpUnknownText));
    }

    private void dumpUnknownText() {
        Set<String> unknown = TextReplacement.getUnknown();
        if (unknown.isEmpty()) return;

        String path = UnknownDump.getCurrentPath();
        LOG.info("Shutdown hook: dumping {} unknown English strings to {}", unknown.size(), path);
        int wrote = UnknownDump.dump(unknown);
        if (wrote < 0) {
            LOG.error("Shutdown hook: failed to dump unknown strings to {}", path);
        }
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.yalu.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("dingzhen-vape", "Meteor-I18n-Support-plugin");
    }
}

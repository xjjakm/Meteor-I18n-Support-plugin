package com.yalu.addon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;

public class Translator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Translator.class);
    private final JsonObject langJson = new JsonObject();
    private Map<String, String> currentLangStrings;
    /** 记录已加载语言代码的签名，语言未变化时跳过重复 reload */
    private String loadedLangSignature;
    /** 已记入 lang.json 的缺失键集合，避免渲染路径（下拉框、按钮每帧）反复刷盘 */
    private final Set<String> recordedMissingKeys = Collections.synchronizedSet(new HashSet<>());

    /**
     * 只读查询翻译：命中返回译文，未命中返回 fallback，不写 lang.json。
     * 用于渲染路径（如下拉框每帧文本），避免缺失键反复写盘。
     */
    public String get(String key, String fallback) {
        if (this.currentLangStrings == null) return fallback;
        String value = this.currentLangStrings.get(key);
        return value != null ? value : fallback;
    }

    /**
     * 记录缺失键到 lang.json。渲染路径（下拉框、按钮每帧）会频繁以同一键查询，
     * 只在第一次见到该缺失键时写入一次，避免每帧写盘。
     * 命中返回译文；未命中时写入 lang.json 并返回 fallback。
     */
    public String recordMissing(String key, String fallback) {
        if (!this.recordedMissingKeys.add(key)) return get(key, fallback);
        return this.Translate(key, fallback);
    }

    public String Translate(String key,String name) {
        if (this.currentLangStrings == null) {
            return name;
        }
        String value = this.currentLangStrings.get(key);
        if(value != null){
            return value;
        }else{
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            langJson.addProperty(key,name);
            LOGGER.info("[MeteorTranslation] 未翻译键已写入 lang.json: {}", key);
            Path path = Paths.get("lang.json");
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                gson.toJson(langJson, writer);
            } catch (IOException e) {
                LOGGER.error("[MeteorTranslation] 写入 lang.json 失败: {}", e.toString());
            }
        }
        return name;
    }


    public void reload(ResourceManager manager)
    {
        reload(manager, false);
    }

    /** 强制重新加载标准语言文件，绕过幂等去重（用于 /meteori18n reload 手动重载）。 */
    public void forceReload(ResourceManager manager)
    {
        reload(manager, true);
    }

    private void reload(ResourceManager manager, boolean force)
    {
        Iterable<String> langCodes = getCurrentLangCodes();

        // 幂等去重：启动时大量 Module/Setting 构造函数都会触发 reload，
        // 而语言代码未变化时无需重新读文件，避免日志刷屏与重复 IO。
        // force=true 时（手动 reload 命令）跳过该判断，始终重新读取。
        String signature = langCodeSignature(langCodes);
        if (!force && signature.equals(loadedLangSignature)) return;

        HashMap<String, String> currentLangStrings = new HashMap<>();
        //从mixin获取管理器然后获取当前语言的语言代码，然后加载翻译文件
//		//这个方法会将语言文件内的键值对赋值给currentLangStrings（这是个HASHMAP（键值对））
        loadTranslations(manager, langCodes,
            currentLangStrings::put);
        //设置不可变的map 也就是说现在这个currentLangStrings就是当前语言的键值对翻译了
        this.currentLangStrings =
            Collections.unmodifiableMap(currentLangStrings);
        this.loadedLangSignature = signature;

        // 按键前缀分类统计翻译键数量（如 Setting / Module / Gui / meteori18n 等），便于观察各分类规模
        LOGGER.info("[MeteorTranslation] Translation keys by category: {}", summarizeCategories(currentLangStrings));
    }

    /** 统计翻译键的顶部分类（第一个 . 之前的前缀）数量。 */
    private static String summarizeCategories(Map<String, String> strings) {
        TreeMap<String, Integer> counts = new TreeMap<>();
        for (String key : strings.keySet()) {
            int dot = key.indexOf('.');
            String prefix = dot == -1 ? key : key.substring(0, dot);
            counts.merge(prefix, 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder();
        counts.forEach((prefix, count) -> sb.append(prefix).append('=').append(count).append(", "));
        return !sb.isEmpty() ? sb.substring(0, sb.length() - 2) : "(none)";
    }

    private static String langCodeSignature(Iterable<String> langCodes) {
        StringBuilder sb = new StringBuilder();
        for (String code : langCodes) {
            sb.append(code).append(';');
        }
        return sb.toString();
    }

    private Iterable<String> getCurrentLangCodes() {
        // Weird bug: Some users have their language set to "en_US" instead of
        // "en_us.json" for some reason. Last seen in 1.21.
        String mainLangCode = Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase();
        // 剥离可能带有的 .json 后缀，避免与 loadTranslations 追加的后缀重复
        mainLangCode = stripJsonSuffix(mainLangCode);

        ArrayList<String> langCodes = new ArrayList<>();
        langCodes.add("en_us");
        if(!"en_us".equals(mainLangCode))
            langCodes.add(mainLangCode);

        return langCodes;
    }

    private static String stripJsonSuffix(String langCode) {
        if (langCode.endsWith(".json")) {
            return langCode.substring(0, langCode.length() - 5);
        }
        return langCode;
    }

    private void loadTranslations(ResourceManager manager,
                                  Iterable<String> langCodes, BiConsumer<String, String> entryConsumer)
    {
        //遍历所有已经获取的语言代码
        for(String langCode : langCodes)
        {
            //设置路径
            String langFilePath = "lang/" + langCode + ".json";

            //注册语言ID
            Identifier langId = Identifier.fromNamespaceAndPath("yalu", langFilePath);

            int[] count = {0};
            for(Resource resource : manager.getResourceStack(langId))
                try(InputStream stream = resource.open())
                {
                    Language.loadFromJson(stream, (key, value) -> { entryConsumer.accept(key, value); count[0]++; });

                }catch(IOException e)
                {
                    LOGGER.error("[MeteorTranslation] Failed to load translations for {} from pack {}", langCode, resource.sourcePackId(), e);
                }
            LOGGER.info("[MeteorTranslation] Loaded {} standard translations for {}", count[0], langCode);
        }
    }
}

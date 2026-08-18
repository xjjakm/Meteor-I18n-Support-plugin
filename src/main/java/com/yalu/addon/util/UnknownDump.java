package com.yalu.addon.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
* 统一未知英文 dump 出口。
* MeteorTranslation 的 shutdown hook 与 Translation.onDeactivate 两条路径此前各自实现、
* 路径来源不一致（均用固定/可配置路径），这里收敛为公共写文件函数。
* 返回值：&gt;0=写入条数，0=空集未写，-1=写出失败。
*/
public class UnknownDump {
private static final Logger LOG = LogUtils.getLogger();

/** 默认导出路径，须与 Translation.sSetUnknownDumpPath 默认值保持一致。 */
public static final String DEFAULT_PATH = "meteor-client/meteor-translation-addon/unknown.json";

private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

/** 当前导出路径。模块激活时同步为设置值；为空时用默认。 */
private static volatile String currentPath = DEFAULT_PATH;

private UnknownDump() {}

/** 设置当前导出路径（供模块启动/设置变更时同步）。 */
public static void setCurrentPath(String path) {
currentPath = (path == null || path.isEmpty()) ? DEFAULT_PATH : path;
}

/** 取当前导出路径；为空回退默认。shutdown hook 在全链不可用时也可安全调用。 */
public static String getCurrentPath() {
return currentPath == null ? DEFAULT_PATH : currentPath;
}

/** 写入当前路径（UTF-8，覆盖写）。空集返回 0，失败返回 -1。 */
public static int dump(Set<String> unknown) {
return dump(unknown, getCurrentPath());
}

/** 写入指定路径（UTF-8，覆盖写）。空集返回 0，失败返回 -1。 */
public static int dump(Set<String> unknown, String path) {
if (unknown == null || unknown.isEmpty()) {
LOG.info("[UnknownDump] 无未知英文，跳过");
return 0;
}
try {
File file = new File(path);
File parent = file.getParentFile();
if (parent != null) parent.mkdirs();
try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
writer.write(GSON.toJson(unknown));
}
LOG.info("[UnknownDump] 已导出 {} 条未知英文到 {}", unknown.size(), path);
return unknown.size();
} catch (Exception e) {
LOG.error("[UnknownDump] 导出失败 {}: {}", path, e.getMessage());
return -1;
}
}
}
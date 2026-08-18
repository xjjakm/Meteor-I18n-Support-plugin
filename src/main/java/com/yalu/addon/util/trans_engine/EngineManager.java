package com.yalu.addon.util.trans_engine;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Set;

public class EngineManager {
private static final Logger LOG = LogUtils.getLogger();
private static final String FALLBACK_ENGINE = "OLD";
private static final EngineManager INSTANCE = new EngineManager();

public static EngineManager getInstance() {
return INSTANCE;
}

public Set<String> getEngineNames() {
return engines.keySet();
}

public AbstractTransEngine getEngine(String name) {
AbstractTransEngine engine = engines.get(name);
if (engine == null) {
LOG.warn("[EngineManager] 未知翻译引擎 {}，回退到 {}（若配置文件残留旧值，可在模块设置中改为 {}）",
name, FALLBACK_ENGINE, engines.keySet());
return engines.get(FALLBACK_ENGINE);
}
return engine;
}

LinkedHashMap<String, AbstractTransEngine> engines = new LinkedHashMap<>();

private EngineManager(){
engines.put("OLD", new TransEngineOld());
engines.put("NEW", new TransEngineNew());
}

}

### 本仓库现开放接受问题报告(issues)和拉取请求(pull requests)，从本仓库下载的mod的bug和翻译问题请务必提交到本仓库，因为这个分支的代码和yalu的差别非常大。

# Meteor I18n Support

![repo-size](https://img.shields.io/github/repo-size/xjjakm/Meteor-I18n-Support-plugin) 
![Downloads](https://img.shields.io/github/downloads/xjjakm/Meteor-I18n-Support-plugin/total)
#### 这个插件为Meteor本体添加了lang语言文件支持，使大部分文字可以进行非硬编码更改
#### 这个复刻添加了繁體中文(台灣)语言支持,且只支持最新版本
### 怎么用？
1. 从 Actions 或者 Releases 中下载对应正确mc版本的mod
2. 下载对应版本的 meteor 本体
3. 把这个mod添加到mods文件夹

### 注意事项
- ！！！本插件未进行全版本测试
- 经过测试 在1.21以下版本未进行适配无法使用
- ！！！本插件未进行全面的测试，与其他插件一起使用时可能小概率存在冲突，若游戏崩溃，可以尝试将本插件删除
- ！！！本插件可以同步支持其他Meteor插件 需要将版本文件夹根目录lang.json文件内的内容移动到相应的语言文件内（asset/.../lang/XX_XX.json）
- ！！！本插件不会默认适配已经汉化过的Meteor客户端
- ！！！[MeteorCN](https://github.com/dingzhen-vape/MeteorCN)不会更新了

### 原理
- Meteor本体的语言文件是通过硬编码的方式实现的，即在代码中直接写死了所有文字。

- 本分支有三套翻译方式

#### 第一套：Translator 语言文件系统（标准 key-value，分支最早的翻译模式）
**文件：** [zh_cn.json](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/resources/assets/yalu/lang/zh_cn.json) / [zh_tw.json](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/resources/assets/yalu/lang/zh_tw.json) / [en_us.json](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/resources/assets/yalu/lang/en_us.json) 

**核心类：** [Translator.java](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/java/com/yalu/addon/Translator.java) + 各 Mixin（ModuleMixin、SettingMixin 等）

**原理：** 通过 Minecraft 的 `ResourceManager` 加载标准 JSON 语言文件，建立 `key → 翻译` 的 HashMap。各类 Mixin 在对象初始化时调用 `TRANSLATOR.Translate(key, fallbackName)` 替换字段（如 `title`、`description`）。

**覆盖范围：**
- 模块名 / 模块描述（`Module.Meteor.Xxx`）
- 设置名 / 设置描述（`Setting.Meteor.Xxx`）
- 分类、标签页等静态资源

---

#### 第二套：Universal 通用文本替换系统（运行时字符串精确匹配）
**文件：** [universal_zh_cn.json](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/resources/assets/yalu/lang/universal_zh_cn.json) / [universal_zh_tw.json](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/resources/assets/yalu/lang/universal_zh_tw.json) / [universal_en_us.json](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/resources/assets/yalu/lang/universal_en_us.json)
**核心类：** [UniversalLangLoader.java](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/java/com/yalu/addon/util/UniversalLangLoader.java) + [TextReplacement.java](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/java/com/yalu/addon/util/TextReplacement.java)

**原理：** 启动时及切换语言时（`TranslateAddon.onInitialize` / `LanguageManagerMixin` → `UniversalLangLoader.reload()`）根据当前游戏语言加载对应的 `universal_<lang>.json` 到 `TextReplacement.map`，运行时通过 `@ModifyArg` 拦截方法参数后调用 `TextReplacement.replace(str)` 做精确替换。

**按语言加载策略：**
- `zh_cn` → 加载 `universal_zh_cn.json`
- `zh_tw` → 加载 `universal_zh_tw.json`，若为空则 fallback 到 `universal_zh_cn.json`
- `en_us` 及其他语言 → 加载 `universal_en_us.json`（空表，不做任何替换）

**覆盖范围（硬编码字符串）：**
- 聊天消息格式字符串：`ChatUtilsMixin` 拦截 `String.format()` 的第一个参数
- 聊天前缀、通知字符串
- 按钮文本、状态文本（如 `Not using a proxy`）
- 模块开关提示（`Toggled` 系列字符串）

---

#### 第三套：Mixin 直改系统（修改代码逻辑或参数,其实也算另外一种“硬编码”）
**核心类：** [JoinMultiplayerScreenTranslationMixin](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/java/com/yalu/addon/mixin/JoinMultiplayerScreenTranslationMixin.java) / [ModuleMixin](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/java/com/yalu/addon/mixin/ModuleMixin.java) 的 `@Redirect` 等

**原理：** 不依赖字符串匹配，直接在 Mixin 中替换逻辑：
- `ModuleMixin` → `@Redirect sendToggledMsg(...)` 根据当前语言分流：`zh_cn` 显示「开启/关闭」、`zh_tw` 显示「開啟/關閉」、其他语言保持 `on/off`
- `JoinMultiplayerScreenTranslationMixin` → 通过 `children()` 遍历找到 `Button` 实例并替换文本

**覆盖范围：**
- 字符串是动态拼接的（带颜色代码 `§a` 等前缀），无法精确匹配
- 按钮、状态是运行时动态创建的 `Widget`，没有翻译 key
- 目标类属于 Meteor 的 Mixin（无法直接 target）

---

**三套系统的选择顺序：**
1. 有 `key` → 用 **Translator**（写在 zh_cn.json）
2. 无 key 但有纯文字常量 → 用 **TextReplacement**（写在 universal_zh_cn.json）
3. 动态拼接/复杂场景（参数、widget 遍历） → 写 **Mixin** 直改

---

#### 实时语言切换（无需重启游戏）
**核心类：** [LanguageManagerMixin.java](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/java/com/yalu/addon/mixin/LanguageManagerMixin.java) + [LanguageRefresh.java](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/java/com/yalu/addon/util/LanguageRefresh.java) + [SettingAccessor.java](file:///e:/Meteor-I18n-Support-plugin-chxjj/src/main/java/com/yalu/addon/mixin/SettingAccessor.java)

**原理：** `LanguageManagerMixin` 注入 `LanguageManager.setSelected()` 的 TAIL，在玩家切换语言后依次执行：
1. `UniversalLangLoader.reload()` — 按新语言重新加载 universal 替换表
2. `LanguageRefresh.applyAll()` — 遍历所有已注册的 Module / SettingGroup / Setting / Category / Tab，使用原始英文名作为 key 重新查询 TRANSLATOR 并更新 title/description 字段

**原始名称找回策略：**
- Module.name / Category.name / Tab.name — 始终保持英文，直接用作翻译 key
- Setting — 通过 `SettingAccessor.getName()` Mixin Accessor 读取 `name` 字段（未被翻译的原始英文）


- 现已支持自定义字体中文渲染

### 待办事项
- 不知道

### 鸣谢
[dingzhen-vape](https://github.com/dingzhen-vape)

AI

Meteor开发者

Wurst客户端提供语言文件I18n支持思路

[KJH50](https://github.com/KJH50)的中文补丁


<img width="1920" height="1009" alt="2026-05-23_16 33 05" src="https://github.com/user-attachments/assets/afc0b1ee-ddd8-4688-aba6-4349d78ed9ce" />

<img width="2560" height="1351" alt="Snipaste_2025-07-31_02-19-29" src="https://github.com/user-attachments/assets/af281df7-9e49-44e5-b1cb-a71de21f7ffc" />

<img width="1920" height="1009" alt="2026-05-23_16 39 41" src="https://github.com/user-attachments/assets/a28d3677-02cc-4eaf-96be-8f7f01ecebce" />

<img width="1920" height="1009" alt="2026-05-23_16 45 55" src="https://github.com/user-attachments/assets/e6939ea4-777a-4763-b9c0-c6b84040fb64" />

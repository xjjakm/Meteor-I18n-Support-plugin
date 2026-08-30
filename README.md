### 简体中文 [繁體中文](./README_zh-tw.md) [English](./README_en-us.md) 

# Meteor I18n Support

> 本仓库现开放接受问题报告(issues)和拉取请求(pull requests)，从本仓库下载的mod的bug和翻译问题请务必提交到本仓库，因为这个分支的代码和yalu的差别非常大。

![repo-size](https://img.shields.io/github/repo-size/xjjakm/Meteor-I18n-Support-plugin) 
![Downloads](https://img.shields.io/github/downloads/xjjakm/Meteor-I18n-Support-plugin/total)
<img src="https://img.shields.io/github/languages/code-size/xjjakm/Meteor-I18n-Support-plugin" alt="GitHub code size in bytes"/>
<img src="https://img.shields.io/github/license/xjjakm/Meteor-I18n-Support-plugin?logo=mdBook&color=FF8C00">
#### 这个插件为Meteor本体添加了lang语言文件支持，使大部分文字可以进行非硬编码更改
#### 这个复刻添加了繁體中文(台灣)语言支持,且只支持最新版本
### 怎么用？
1. 从 Actions 或者 Releases 中下载对应正确mc版本的mod
2. 下载对应版本的 meteor 本体(还需fabric api（模组版本 ≥26.2-5）)
3. 把这个mod添加到mods文件夹

### 注意事项
- ！！！本插件对游戏版本有较为严格的要求
- ！！！本插件未进行全面的测试，与其他插件一起使用时可能小概率存在冲突，若游戏崩溃，可以尝试将本插件删除，也可以交问题报告
- ！！！本插件现已适配 [Catppuccin addon](https://github.com/X-C-0/catppuccin-addon) 和 [Meteor+](https://github.com/MeteorClientPlus/MeteorPlus) 的相关内容，如果遇到这和两个插件一起运行而导致的的问题请上报
- ！！！本插件可以同步支持其他Meteor插件 需要将版本文件夹根目录lang.json文件内的内容移动到相应的语言文件内（asset/.../lang/XX_XX.json）
- ！！！本插件不会默认适配已经汉化过的Meteor客户端
- ！！！[MeteorCN](https://github.com/dingzhen-vape/MeteorCN)不会更新了

### 特性
- Meteor本体的语言文件是通过硬编码的方式实现的，即在代码中直接写死了所有文字。

- 本分支有三套翻译方式

#### 第一套：Translator 语言文件系统（标准 key-value，优先级最高）
**文件：** [zh_cn.json](src/main/resources/assets/yalu/lang/zh_cn.json) / [zh_tw.json](src/main/resources/assets/yalu/lang/zh_tw.json) / [en_us.json](src/main/resources/assets/yalu/lang/en_us.json) 

**核心类：** [Translator.java](src/main/java/com/yalu/addon/Translator.java) + 各 Mixin（ModuleMixin、SettingMixin、CategoryMixin、TabMixin 等）

**原理：** 通过 Minecraft 的 `ResourceManager` 加载标准 JSON 语言文件，建立 `key → 翻译` 的 HashMap。各类 Mixin 在对象初始化时调用 `TRANSLATOR.Translate(key, fallbackName)` 替换字段（如 `title`、`description`、`name`）。

**覆盖范围：**
- 模块名 / 模块描述（`Module.Meteor.Xxx`）— 初始化 + 切换语言
- 设置名 / 设置描述（`Setting.Meteor.Xxx`）— 初始化 + 切换语言
- 设置分组名（`Module.Meteor.Xxx.general.name`）— 初始化 + 切换语言
- 分类名（`Category.Meteor.combat`）— 初始化 + 切换语言
- 标签页名（`Tab.Meteor.modules`）— 初始化 + 切换语言

**多插件前缀支持：** `ModuleMixin` 的 `@Inject onInit` 从 `addon.name` 动态获取插件前缀（如 `Meteor`、`Meteor+`、`Meteor-I18n-Support`），使各插件的模块/设置翻译能使用各自前缀的 key（`Module.Meteor+.*`、`Module.Meteor-I18n-Support.*` 等）

---

#### 第二套：Universal 通用文本替换系统（运行时字符串精确匹配，一种临时解决方案，正在逐渐被取代）
**文件：** [universal_zh_cn.json](src/main/resources/assets/yalu/lang/universal_zh_cn.json) / [universal_zh_tw.json](src/main/resources/assets/yalu/lang/universal_zh_tw.json) / [universal_en_us.json](src/main/resources/assets/yalu/lang/universal_en_us.json)
**核心类：** [UniversalLangLoader.java](src/main/java/com/yalu/addon/util/UniversalLangLoader.java) + [TextReplacement.java](src/main/java/com/yalu/addon/util/TextReplacement.java)

**原理：** 启动时及切换语言时（`TranslateAddon.onInitialize` / `LanguageManagerMixin` → `UniversalLangLoader.reload()`）根据当前游戏语言加载对应的 `universal_<lang>.json` 到 `TextReplacement.map`，运行时通过 `@ModifyArg` 拦截方法参数后调用 `TextReplacement.replace(str)` 做精确替换。

**按语言加载策略：**
- `zh_cn` → 加载 `universal_zh_cn.json`
- `zh_tw` → 加载 `universal_zh_tw.json`，若为空则 fallback 到 `universal_zh_cn.json`
- `en_us` 及其他语言 → 加载 `universal_en_us.json`（空表，不做任何替换）

**覆盖范围（硬编码字符串）：**
- 聊天消息格式字符串：`ChatUtilsMixin` 拦截 `String.format()` 的第一个参数
- 聊天前缀、通知字符串
- 按钮文本、状态文本（如 `Not using a proxy`）

---

#### 第三套：Mixin 直改系统（修改代码逻辑或参数,其实也算另外一种“硬编码”）
**核心类：** [JoinMultiplayerScreenTranslationMixin](src/main/java/com/yalu/addon/mixin/JoinMultiplayerScreenTranslationMixin.java) / [ModuleMixin](src/main/java/com/yalu/addon/mixin/ModuleMixin.java) 的 `@Redirect` 等

**原理：** 不依赖字符串匹配，直接在 Mixin 中替换逻辑：
- `ModuleMixin` → `@Redirect sendToggledMsg(...)` 根据当前语言分流：`zh_cn` 显示「开启/关闭」、`zh_tw` 显示「開啟/關閉」、其他语言保持 `on/off`（带颜色代码的动态拼接字符串，universal 系统无法精确匹配，必须用 Mixin 直改）
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
**核心类：** [LanguageManagerMixin.java](src/main/java/com/yalu/addon/mixin/LanguageManagerMixin.java) + [LanguageRefresh.java](src/main/java/com/yalu/addon/util/LanguageRefresh.java) + [NameCache.java](src/main/java/com/yalu/addon/util/NameCache.java) + [SettingAccessor.java](src/main/java/com/yalu/addon/mixin/SettingAccessor.java)

**原理：** `LanguageManagerMixin` 注入 `LanguageManager.setSelected()` 的 TAIL，在玩家切换语言后依次执行：
1. `UniversalLangLoader.reload()` — 按新语言重新加载 universal 替换表
2. `LanguageRefresh.applyAll()` — 遍历所有已注册的 Module / SettingGroup / Setting / Category / Tab，使用原始英文名作为 key 重新查询 TRANSLATOR 并更新字段

此外，`TranslateAddon.onInitialize()` 也会调用 `LanguageRefresh.applyAll()`：由于 `TranslateAddon.<clinit>` 中创建 `CATEGORY` 时 MC 尚为 null，部分模块/分类/Tab 的 Mixin.onInit 无法翻译，需等 MC 就绪后统一补翻译。

**原始名称找回策略：**
- Module.name — 始终保持英文，直接用作翻译 key
- Category — 通过 `NameCache.category()` 缓存原始英文名（使用 `IdentityHashMap` 防止 `hashCode()` 依赖可变 `name` 导致缓存失效）
- Tab — 通过 `NameCache.tab()` 缓存原始英文名
- SettingGroup — 通过 `NameCache.group()` 缓存原始英文名
- Setting — 通过 `SettingAccessor.getName()` Mixin Accessor 读取 `name` 字段（未被翻译的原始英文）

- 现已支持自定义字体中文渲染

---

### 聊天命令（这里使用Fabric API）
**核心类：** [MeteorI18nCommand.java](src/main/java/com/yalu/addon/commands/MeteorI18nCommand.java)（在 [TranslateAddon.onInitialize](src/main/java/com/yalu/addon/TranslateAddon.java) 通过 `ClientCommandRegistrationCallback` 注册）

提供 `/meteori18n` 命令，直接输入会显示帮助信息；所有输出（含前缀 `[彗星翻译]`）均从标准语言文件 `meteori18n.*` 键读取，随当前语言本地化，不经过 Meteor 聊天管线：

| 子命令 | 说明 |
| ------ | ---- |
| `/meteori18n` / `/meteori18n help` | 显示帮助信息 |
| `/meteori18n reload` | 与游戏内切换语言完全一致地重新加载并应用三套翻译（强制重载标准语言文件，绕过幂等去重，可按需热加载游戏外修改的 `zh_cn.json`），并重建聊天前缀、刷新打开的 GUI/HUD 编辑器 |
| `/meteori18n export` | 导出全部模块的翻译键值对，输出规范 JSON 到 `.minecraft/meteor-client/meteor-translation-addon/meteor-i18n-export.json`，聊天栏提示导出条数与路径 |

**说明：**
- 建议在语言为英文时导出
- 导出不按 addon 过滤，涵盖所有模块（含第三方）及 HUD Preset / Tab / 独立 Settings 的翻译键值对
- 聊天输出统一走 `meteori18n.*` 标准语言键（`export.success` / `export.empty` / `reload.success` 等），不再硬编码中文

### 待办事项
- 不知道

### 鸣谢
[dingzhen-vape](https://github.com/dingzhen-vape)

AI

Meteor开发者

Wurst客户端提供语言文件I18n支持思路

[KJH50](https://github.com/KJH50)的中文补丁

<img width="1919" height="1030" alt="屏幕截图 2026-08-30 170943" src="https://github.com/user-attachments/assets/2bb26046-fcc9-4a45-8609-8cb886106515" />

<img width="1918" height="1031" alt="屏幕截图 2026-08-30 171956" src="https://github.com/user-attachments/assets/f1868a3c-7de7-4194-bf1e-88d2cc74666c" />

<img width="1920" height="1009" alt="2026-05-23_16 33 05" src="https://github.com/user-attachments/assets/afc0b1ee-ddd8-4688-aba6-4349d78ed9ce" />

<img width="2560" height="1351" alt="Snipaste_2025-07-31_02-19-29" src="https://github.com/user-attachments/assets/af281df7-9e49-44e5-b1cb-a71de21f7ffc" />

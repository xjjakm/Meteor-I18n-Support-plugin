### 本倉庫現開放接受問題回報(issues)和拉取請求(pull requests)，從本倉庫下載的mod的bug和翻譯問題請務必提交到本倉庫，因為這個分支的程式碼和yalu的差別非常大。

# Meteor I18n Support

![repo-size](https://img.shields.io/github/repo-size/xjjakm/Meteor-I18n-Support-plugin) 
![Downloads](https://img.shields.io/github/downloads/xjjakm/Meteor-I18n-Support-plugin/total)
<img src="https://img.shields.io/github/languages/code-size/xjjakm/Meteor-I18n-Support-plugin" alt="GitHub code size in bytes"/>
#### 這個插件為Meteor本體添加了lang語言檔案支援，使大部分文字可以進行非硬編碼更改
#### 這個復刻添加了繁體中文(台灣)語言支援，且只支援最新版本
### 怎麼用？
1. 從 Actions 或 Releases 中下載對應正確mc版本的mod
2. 下載對應版本的 meteor 本體（還需 fabric api（模組版本 ≥26.2-5））
3. 把這個mod加入mods資料夾

### 注意事項
- ！！！本插件未進行全版本測試
- 經過測試 在1.21以下版本未進行適配無法使用
- ！！！本插件未進行全面的測試，與其他插件一起使用時可能小機率存在衝突，若遊戲崩潰，可以嘗試將本插件刪除
- ！！！本插件可以同步支援其他Meteor插件 需要將版本資料夾根目錄lang.json檔案內的內容移動到相應的語言檔案內（asset/.../lang/XX_XX.json）
- ！！！本插件不會預設適配已經漢化過的Meteor用戶端
- ！！！[MeteorCN](https://github.com/dingzhen-vape/MeteorCN)不會再更新了

### 特性
- Meteor本體的語言檔案是透過硬編碼的方式實現的，即在程式碼中直接寫死了所有文字。

- 本分支有三套翻譯方式

#### 第一套：Translator 語言檔案系統（標準 key-value，優先級最高）
**檔案：** [zh_cn.json](src/main/resources/assets/yalu/lang/zh_cn.json) / [zh_tw.json](src/main/resources/assets/yalu/lang/zh_tw.json) / [en_us.json](src/main/resources/assets/yalu/lang/en_us.json) 

**核心類別：** [Translator.java](src/main/java/com/yalu/addon/Translator.java) + 各 Mixin（ModuleMixin、SettingMixin、CategoryMixin、TabMixin 等）

**原理：** 透過 Minecraft 的 `ResourceManager` 載入標準 JSON 語言檔案，建立 `key → 翻譯` 的 HashMap。各類 Mixin 在物件初始化時呼叫 `TRANSLATOR.Translate(key, fallbackName)` 替換欄位（如 `title`、`description`、`name`）。

**覆蓋範圍：**
- 模組名 / 模組描述（`Module.Meteor.Xxx`）— 初始化 + 切換語言
- 設定名 / 設定描述（`Setting.Meteor.Xxx`）— 初始化 + 切換語言
- 設定分組名（`Module.Meteor.Xxx.general.name`）— 初始化 + 切換語言
- 分類名（`Category.Meteor.combat`）— 初始化 + 切換語言
- 分頁名（`Tab.Meteor.modules`）— 初始化 + 切換語言

**多插件前綴支援：** `ModuleMixin` 的 `@Inject onInit` 從 `addon.name` 動態取得插件前綴（如 `Meteor`、`Meteor+`、`Meteor-I18n-Support`），使各插件的模組/設定翻譯能使用各自前綴的 key（`Module.Meteor+.*`、`Module.Meteor-I18n-Support.*` 等）

---

#### 第二套：Universal 通用文字替換系統（執行時字串精確比對）
**檔案：** [universal_zh_cn.json](src/main/resources/assets/yalu/lang/universal_zh_cn.json) / [universal_zh_tw.json](src/main/resources/assets/yalu/lang/universal_zh_tw.json) / [universal_en_us.json](src/main/resources/assets/yalu/lang/universal_en_us.json)
**核心類別：** [UniversalLangLoader.java](src/main/java/com/yalu/addon/util/UniversalLangLoader.java) + [TextReplacement.java](src/main/java/com/yalu/addon/util/TextReplacement.java)

**原理：** 啟動時及切換語言時（`TranslateAddon.onInitialize` / `LanguageManagerMixin` → `UniversalLangLoader.reload()`）根據目前遊戲語言載入對應的 `universal_<lang>.json` 到 `TextReplacement.map`，執行時透過 `@ModifyArg` 攔截方法參數後呼叫 `TextReplacement.replace(str)` 做精確替換。

**依語言載入策略：**
- `zh_cn` → 載入 `universal_zh_cn.json`
- `zh_tw` → 載入 `universal_zh_tw.json`，若為空則 fallback 到 `universal_zh_cn.json`
- `en_us` 及其他語言 → 載入 `universal_en_us.json`（空表，不做任何替換）

**覆蓋範圍（硬編碼字串）：**
- 聊天訊息格式字串：`ChatUtilsMixin` 攔截 `String.format()` 的第一個參數
- 聊天前綴、通知字串
- 按鈕文字、狀態文字（如 `Not using a proxy`）

---

#### 第三套：Mixin 直改系統（修改程式碼邏輯或參數，其實也算另外一種「硬編碼」）
**核心類別：** [JoinMultiplayerScreenTranslationMixin](src/main/java/com/yalu/addon/mixin/JoinMultiplayerScreenTranslationMixin.java) / [ModuleMixin](src/main/java/com/yalu/addon/mixin/ModuleMixin.java) 的 `@Redirect` 等

**原理：** 不依賴字串比對，直接在 Mixin 中替換邏輯：
- `ModuleMixin` → `@Redirect sendToggledMsg(...)` 根據目前語言分流：`zh_cn` 顯示「開啟/關閉」、`zh_tw` 顯示「開啟/關閉」、其他語言保持 `on/off`（帶顏色程式碼的動態拼接字串，universal 系統無法精確比對，必須用 Mixin 直改）
- `JoinMultiplayerScreenTranslationMixin` → 透過 `children()` 遍歷找到 `Button` 實例並替換文字

**覆蓋範圍：**
- 字串是動態拼接的（帶顏色程式碼 `§a` 等前綴），無法精確比對
- 按鈕、狀態是執行時動態建立的 `Widget`，沒有翻譯 key
- 目標類別屬於 Meteor 的 Mixin（無法直接 target）

---

**三套系統的選擇順序：**
1. 有 `key` → 用 **Translator**（寫在 zh_cn.json）
2. 無 key 但有純文字常數 → 用 **TextReplacement**（寫在 universal_zh_cn.json）
3. 動態拼接/複雜場景（參數、widget 遍歷） → 寫 **Mixin** 直改

---

#### 即時語言切換（無需重啟遊戲）
**核心類別：** [LanguageManagerMixin.java](src/main/java/com/yalu/addon/mixin/LanguageManagerMixin.java) + [LanguageRefresh.java](src/main/java/com/yalu/addon/util/LanguageRefresh.java) + [NameCache.java](src/main/java/com/yalu/addon/util/NameCache.java) + [SettingAccessor.java](src/main/java/com/yalu/addon/mixin/SettingAccessor.java)

**原理：** `LanguageManagerMixin` 注入 `LanguageManager.setSelected()` 的 TAIL，在玩家切換語言後依序執行：
1. `UniversalLangLoader.reload()` — 依新語言重新載入 universal 替換表
2. `LanguageRefresh.applyAll()` — 遍歷所有已註冊的 Module / SettingGroup / Setting / Category / Tab，使用原始英文名作為 key 重新查詢 TRANSLATOR 並更新欄位

此外，`TranslateAddon.onInitialize()` 也會呼叫 `LanguageRefresh.applyAll()`：由於 `TranslateAddon.<clinit>` 中建立 `CATEGORY` 時 MC 尚為 null，部分模組/分類/Tab 的 Mixin.onInit 無法翻譯，需等 MC 就緒後統一補翻譯。

**原始名稱找回策略：**
- Module.name — 始終保持英文，直接用作翻譯 key
- Category — 透過 `NameCache.category()` 快取原始英文名（使用 `IdentityHashMap` 防止 `hashCode()` 依賴可變 `name` 導致快取失效）
- Tab — 透過 `NameCache.tab()` 快取原始英文名
- SettingGroup — 透過 `NameCache.group()` 快取原始英文名
- Setting — 透過 `SettingAccessor.getName()` Mixin Accessor 讀取 `name` 欄位（未被翻譯的原始英文）

- 現已支援自訂字型中文字渲染

---

### 聊天指令（這裡使用Fabric API）
**核心類別：** [MeteorI18nCommand.java](src/main/java/com/yalu/addon/commands/MeteorI18nCommand.java)（在 [TranslateAddon.onInitialize](src/main/java/com/yalu/addon/TranslateAddon.java) 透過 `ClientCommandRegistrationCallback` 註冊）

提供 `/meteori18n` 指令，直接輸入會顯示說明資訊；所有輸出（含前綴 `[彗星翻譯]`）均從標準語言檔案 `meteori18n.*` 鍵讀取，隨目前語言在地化，不經過 Meteor 聊天管線：

| 子指令 | 說明 |
| ------ | ---- |
| `/meteori18n` / `/meteori18n help` | 顯示說明資訊 |
| `/meteori18n reload` | 與遊戲內切換語言完全一致地重新載入並套用三套翻譯（強制重新載入標準語言檔案，繞過冪等去重，可按需熱載入遊戲外修改的 `zh_cn.json`），並重建聊天前綴、重新整理開啟中的 GUI/HUD 編輯器 |
| `/meteori18n export` | 匯出全部模組的翻譯鍵值對，輸出規範 JSON 到 `.minecraft/meteor-client/meteor-translation-addon/meteor-i18n-export.json`，聊天欄提示匯出條數與路徑 |

**說明：**
- 建議在語言為英文時匯出
- 匯出不依 addon 過濾，涵蓋所有模組（含第三方）及 HUD Preset / Tab / 獨立 Settings 的翻譯鍵值對
- 聊天輸出統一走 `meteori18n.*` 標準語言鍵（`export.success` / `export.empty` / `reload.success` 等），不再硬編碼中文

### 待辦事項
- 不知道

### 鳴謝
[dingzhen-vape](https://github.com/dingzhen-vape)

AI

Meteor開發者

Wurst用戶端提供語言檔案I18n支援思路

[KJH50](https://github.com/KJH50)的中文補丁


<img width="1920" height="1009" alt="2026-05-23_16 33 05" src="https://github.com/user-attachments/assets/afc0b1ee-ddd8-4688-aba6-4349d78ed9ce" />

<img width="2560" height="1351" alt="Snipaste_2025-07-31_02-19-29" src="https://github.com/user-attachments/assets/af281df7-9e49-44e5-b1cb-a71de21f7ffc" />

<img width="1920" height="1009" alt="2026-05-23_16 39 41" src="https://github.com/user-attachments/assets/a28d3677-02cc-4eaf-96be-8f7f01ecebce" />

<img width="1920" height="1009" alt="2026-05-23_16 45 55" src="https://github.com/user-attachments/assets/e6939ea4-777a-4763-b9c0-c6b84040fb64" />

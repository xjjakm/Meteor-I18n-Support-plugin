### English [简体中文](./README.md) [繁體中文](./README_zh-tw.md) 

# Meteor I18n Support

![repo-size](https://img.shields.io/github/repo-size/xjjakm/Meteor-I18n-Support-plugin) 
![Downloads](https://img.shields.io/github/downloads/xjjakm/Meteor-I18n-Support-plugin/total)
<img src="https://img.shields.io/github/languages/code-size/xjjakm/Meteor-I18n-Support-plugin" alt="GitHub code size in bytes"/>
<img src="https://img.shields.io/github/license/xjjakm/Meteor-I18n-Support-plugin?logo=mdBook&color=FF8C00">

> This repository now accepts bug reports and pull requests. Please submit any bugs and translation issues for mods downloaded from this repository to this repository, because this branch's code differs greatly from yalu's.

#### This addon adds lang language file support to the Meteor client, so most text can be changed without hardcoding it.
#### This fork adds Traditional Chinese (Taiwan) language support, and only supports the latest version.
### How to use?
1. Download the mod for the correct Minecraft version from **Actions** or **Releases**.
2. Download the matching version of the Meteor client itself (still need Fabric API, minecraft version >= 26.2-5).
3. Put this mod into the `mods` folder.

### Notes
- !!! This addon has fairly strict requirements on the game version.
- !!! This addon has not been thoroughly tested; when used together with other addons there is a small chance of conflicts. If the game crashes, try removing this addon, or submit a bug report.
- !!! This addon is now compatible with [Catppuccin addon](https://github.com/X-C-0/catppuccin-addon) and [Meteor+](https://github.com/MeteorClientPlus/MeteorPlus). If you encounter issues caused by running together with these two addons, please report them.
- !!! This addon can also support other Meteor addons: move the content of the `lang.json` file at the root of the version folder into the corresponding language file (`assets/.../lang/XX_XX.json`).
- !!! This addon does not adapt to already-localized Meteor clients by default.
- !!! [MeteorCN](https://github.com/dingzhen-vape/MeteorCN) will no longer be updated.

### Features
- Meteor's own language files are implemented via hardcoding — all text is written directly in the code.

- This branch now uses the original translation method again (the Mixins Direct-Edit System was removed in `26.2-8`; the Universal Text Replacement Table was removed in `26.2-9`).

Translation method: Translator language file system (standard key-value)
**Files:** [zh_cn.json](src/main/resources/assets/yalu/lang/zh_cn.json) / [zh_tw.json](src/main/resources/assets/yalu/lang/zh_tw.json) / [en_us.json](src/main/resources/assets/yalu/lang/en_us.json)

**Core classes:** [Translator.java](src/main/java/com/yalu/addon/Translator.java) + various Mixins (ModuleMixin, SettingMixin, CategoryMixin, TabMixin, etc.)

**How it works:** Standard JSON language files are loaded through Minecraft's `ResourceManager`, building a `key → translation` HashMap. Various Mixins call `TRANSLATOR.Translate(key, fallbackName)` when objects are initialized to replace fields (e.g. `title`, `description`, `name`).

**Coverage:**
- Module name / description (`Module.Meteor.Xxx`) — initialization + on language switch
- Setting name / description (`Setting.Meteor.Xxx`) — initialization + on language switch
- Setting group name (`Module.Meteor.Xxx.general.name`) — initialization + on language switch
- Category name (`Category.Meteor.combat`) — initialization + on language switch
- Tab name (`Tab.Meteor.modules`) — initialization + on language switch

**Multi-addon prefix support:** `ModuleMixin`'s `@Inject onInit` dynamically picks up the addon prefix from `addon.name` (e.g. `Meteor`, `Meteor+`, `Meteor-I18n-Support`), so each addon's module/setting translations can use its own prefixed keys (`Module.Meteor+.*`, `Module.Meteor-I18n-Support.*`, etc.)

#### Real-time Language Switching (no game restart needed)
**Core classes:** [LanguageManagerMixin.java](src/main/java/com/yalu/addon/mixin/LanguageManagerMixin.java) + [LanguageRefresh.java](src/main/java/com/yalu/addon/util/LanguageRefresh.java) + [NameCache.java](src/main/java/com/yalu/addon/util/NameCache.java) + [SettingAccessor.java](src/main/java/com/yalu/addon/mixin/SettingAccessor.java)

**How it works:** `LanguageManagerMixin` injects at the TAIL of `LanguageManager.setSelected()`; after the player switches language it runs `LanguageRefresh.applyAll()` — iterating over all registered Module / SettingGroup / Setting / Category / Tab, re-querying the TRANSLATOR using the original English names as keys, and updating the fields

In addition, `TranslateAddon.onInitialize()` also calls `LanguageRefresh.applyAll()`: because when `CATEGORY` is created in `TranslateAddon.<clinit>` the MC instance is still null, some modules/categories/tabs cannot be translated in their Mixin.onInit, so they need a unified re-translation once the MC instance is ready.

**Original-name recovery strategy:**
- Module.name — always kept in English and used directly as the translation key
- Category — caches the original English name via `NameCache.category()` (using `IdentityHashMap` to prevent `hashCode()` dependence on the mutable `name` from breaking the cache)
- Tab — caches the original English name via `NameCache.tab()`
- SettingGroup — caches the original English name via `NameCache.group()`
- Setting — reads the `name` field via the `SettingAccessor.getName()` Mixin Accessor (the untranslated original English)

---

### Chat Commands (via Fabric API)
**Core class:** [MeteorI18nCommand.java](src/main/java/com/yalu/addon/commands/MeteorI18nCommand.java) (registered in [TranslateAddon.onInitialize](src/main/java/com/yalu/addon/TranslateAddon.java) through `ClientCommandRegistrationCallback`)

Provides the `/meteori18n` command. Entering it directly shows the help message; all output (including the `[Comet Translation]` prefix) is read from the standard language file keys `meteori18n.*`, localized to the current language, and does not go through the Meteor chat pipeline:

| Subcommand | Description |
| ------ | ---- |
| `/meteori18n` / `/meteori18n help` | Shows the help message |
| `/meteori18n reload` | Reloads and applies all three translation systems exactly as when switching languages in-game (forcibly reloads the standard language files, bypassing idempotent dedup, and can hot-reload a `zh_cn.json` modified outside the game), rebuilds the chat prefix, and refreshes open GUI/HUD editors |
| `/meteori18n export` | Exports all modules' translation key-value pairs to a well-formed JSON at `.minecraft/meteor-client/meteor-translation-addon/meteor-i18n-export.json`; shows the exported entry count and path in chat |

**Notes:**
- It is recommended to export while the language is English
- The export is not filtered by addon, covering all modules (including third-party) and the translation key-value pairs of HUD Presets / Tabs / standalone Settings
- Chat output uniformly uses the `meteori18n.*` standard language keys (`export.success` / `export.empty` / `reload.success`, etc.), with no hardcoded Chinese

### To-do
- Unknown

### Credits
[dingzhen-vape](https://github.com/dingzhen-vape)

AI

Meteor developers

Wurst client for the idea of I18n language file support

[KJH50's](https://github.com/KJH50) Chinese patch
# Meteor I18n Support
### ![repo-size](https://img.shields.io/github/repo-size/dingzhen-vape/Meteor-I18n-Support-plugin) ![Downloads](https://img.shields.io/github/downloads/dingzhen-vape/Meteor-I18n-Support-plugin/total)

#### 这个插件为Meteor本体添加了lang语言文件支持，使大部分文字可以进行非硬编码更改,修复了Meteor本体的字体显示问题,现在可以正常显示中文

### How to use
与Meteor本体一起使用 像其他插件一样 放入mods文件夹即可
### 原理
- Meteor本体的语言文件是通过硬编码的方式实现的，即在代码中直接写死了所有文字。
- 而这个插件的作用是将所有文字都抽离到一个单独的语言文件中，这样就可以方便的进行非硬编码的更改。
### 注意事项
- 本插件未进行全面的测试，与其他插件一起使用时可能小概率存在冲突，若游戏崩溃，可以尝试将本插件删除
- 本插件可以同步支持其他大部分Meteor插件 需要将版本文件夹根目录lang.json文件内的内容移动到相应的语言文件内（asset/.../lang/XX_XX.json）
- MeteorCN以后可能不会更新

### 待办事项
- [ ] 更多国家的语言支持(如果我有时间的话)

### 使用教程 

 [BiliBili](https://www.bilibili.com/video/BV1zX8yzSE9E?spm_id_from=333.788.videopod.sections&vd_source=113cda7aa1ace627d124f6b5f8e83d4c&p=2)

### 鸣谢
我
DEEPSEEK
Meteor开发者
Wurst客户端提供语言文件I18n支持思路

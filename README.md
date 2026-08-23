### 本仓库现开放接受问题报告(issues)和拉取请求(pull requests)，从本仓库下载的mod的bug和翻译问题请务必提交到本仓库，因为这个分支的代码和yalu的差别非常大。

# Meteor I18n Support

![repo-size](https://img.shields.io/github/repo-size/xjjakm/Meteor-I18n-Support-plugin) 
![Downloads](https://img.shields.io/github/downloads/xjjakm/Meteor-I18n-Support-plugin/total)
![license](https://img.shields.io/github/license/xjjakm/Meteor-I18n-Support-plugin/total)
#### 这个插件为Meteor本体添加了lang语言文件支持，使大部分文字可以进行非硬编码更改
#### 这个复刻添加了繁體中文(台灣)语言支持,且只支持最新版本
### 怎么用？
1. 从 Actions 或者 Releases 中下载对应正确mc版本的mod
2. 下载对应版本的 meteor 本体
3. 把这个mod添加到mods文件夹

### 原理
- Meteor本体的语言文件是通过硬编码的方式实现的，即在代码中直接写死了所有文字。

- 而这个插件的作用是将所有文字都抽离到“一个”单独的语言文件中，这样就可以方便的进行非硬编码的更改。

- 即在每次加载时将Module类的title替换成已有的语言文件中的对应文字。(所以在游戏运行时切换语言插件不生效)

- 现已支持自定义字体中文渲染

### 注意事项
- ！！！本插件未进行全版本测试
- 经过测试 在1.21以下版本未进行适配无法使用
- ！！！本插件未进行全面的测试，与其他插件一起使用时可能小概率存在冲突，若游戏崩溃，可以尝试将本插件删除
- ！！！本插件可以同步支持其他Meteor插件 需要将版本文件夹根目录lang.json文件内的内容移动到相应的语言文件内（asset/.../lang/XX_XX.json）
- ！！！本插件不会默认适配已经汉化过的Meteor客户端
- ！！！[MeteorCN](https://github.com/dingzhen-vape/MeteorCN)不会更新了

### 待办事项
- 不知道

### 使用教程 

 [BiliBili](https://www.bilibili.com/video/BV1zX8yzSE9E?spm_id_from=333.788.videopod.sections&vd_source=113cda7aa1ace627d124f6b5f8e83d4c&p=2)

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

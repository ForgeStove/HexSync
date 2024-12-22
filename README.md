HexSync
=

[![Github Downloads][download-image]][download-url]
[![GitHub stars][stars-image]][stars-url]
[![GitHub issues][issues-image]][issues-url]
[![Github Pulls][pulls-image]][pulls-url]
[![GitHub forks][forks-image]][forks-url]
[![repo-size][repo-size-image]][repo-url]
[![license][license-image]][license-url]

## 项目简介

- HexSync 是一款基于 Java 开发的双端一体的文件同步工具，主要用于在MineCraft客户端和服务端之间同步文件。
- 这是一个可执行jar包，双击即可运行，也可以在命令行用指令以无头模式启动，不是MineCraft模组，请不要把此程序放在模组文件夹下。
- exe可执行文件作用与jar包完全一致，需要自行配置java路径，此版本仅支持Windows，必须装有java 8及以上的版本，已经废弃，不再更新，不建议使用。
- 流程：服务器负责发送文件，客户端会将本地同步文件夹中的文件与服务器的比较，然后再与仅客户端模组的文件比较，如果在任意一方缺少就会从其中获取文件，反之如果同时不存在于任何一方就会被删除。

## 功能特性

- 提供一个简易的GUI，允许用户配置服务端和客户端，以及监控同步过程中的日志信息。
- 使用HTTP协议传输文件，若无公网IP可以使用内网穿透工具穿透TCP隧道来使用。
- 允许服务端对上传速率进行限制。
- 兼容无头模式。
- 仅支持中文。

## 环境要求

- Java 8 及以上版本

## 下载

- [下载][download-url]最新的 HexSync.jar 文件或 HexSync.exe 文件。无需安装。

## 配置

- 启动时，会在程序所在的目录下自动创建HexSync文件夹，然后会在HexSync文件夹下创建配置和日志文件。
- 如果设置了自动启动服务端，则在启动时不会显示GUI并且自动启动。
- 如果设置了自动启动客户端，则在启动时会自动启动客户端并且在更新完全无错误的情况下自动退出
- 可以在控制面板的设置内修改配置并保存。
- #### 服务端设置
- 端口号(默认65535)
- 上传速率限制(0为无限制)(可以设置单位为 B/s / KB/s / MB/s / GB/s )
- 服务端同步文件夹路径(默认为mods，绝对路径和相对路径均可以使用，如果目标文件夹不存在会尝试创建，但是无法在目标文件夹的目录不存在的情况下创建文件夹)
- 自动启动服务端(默认为否)
- #### 客户端设置
- 端口号(默认65535)
- 服务器地址(默认localhost，使用前务必修改为对应的服务器地址，请不要在服务器地址内放入端口号，可以不添加"http://"前缀)
- 客户端同步文件夹路径(默认为mods)
- 仅客户端模组文件夹路径(默认为clientOnlyMods，请把仅客户端模组放入此文件夹内，如果需要更新仅客户端模组需要自行更新(其实是还没想好怎么实现))
- 自动启动客户端(默认为否)

## 启动

- 通过直接以 Java 打开或者通过命令行执行

      java -jar HexSync.jar
- 在无头环境下会自动切换为无头模式，如果需要切换模式可以修改-Djava.awt.headless=true参数（true为启用无头模式）

      java -jar -Djava.awt.headless=true HexSync.jar
- 通过 "设置" 按钮进入设置界面，配置服务端和客户端的相关参数。
- 服务端:把需要同步的文件放入同步文件夹内(默认为软件所在的目录中的mods文件夹)，点击"启动服务端"。
- 客户端:配置设置好后，把仅客户端模组放入clientOnlyMods文件夹(或者是你自定义的文件夹)，点击"启动客户端"。

## 开发与贡献

- 欢迎贡献代码或者提出问题。Pull Request或者提交Issue，我将尽快回复。

## 星标历史
<a href="https://star-history.com/#ForgeStove/HexSync&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=ForgeStove/HexSync&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=ForgeStove/HexSync&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=ForgeStove/HexSync&type=Date" />
 </picture>
</a>

## 贡献者
<h1>
<a href="https://github.com/ForgeStove/HexSync/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=ForgeStove/HexSync" />
</a>
<a href="https://github.com/donywang922/HexSyncReborn/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=donywang922/HexSyncReborn" />
</a>
</h1>

[download-url]: https://github.com/ForgeStove/HexSync/releases "下载"
[download-image]: https://img.shields.io/github/downloads/ForgeStove/HexSync/total?style=flat&logo=markdown&label=总下载数

[stars-url]: https://github.com/ForgeStove/HexSync/stargazers "星标"
[stars-image]: https://img.shields.io/github/stars/ForgeStove/HexSync?style=flat&logo=github&label=星标

[issues-url]: https://github.com/ForgeStove/HexSync/issues "议题"
[issues-image]: https://img.shields.io/github/issues/ForgeStove/HexSync?style=flat&logo=github&label=议题

[pulls-url]: https://github.com/ForgeStove/HexSync/pulls "拉取请求"
[pulls-image]: https://custom-icon-badges.demolab.com/github/issues-pr-raw/ForgeStove/HexSync?style=flat&logo=git-pull-request&label=拉取请求


[forks-url]: https://github.com/ForgeStove/HexSync/fork "复刻"
[forks-image]: https://img.shields.io/github/forks/ForgeStove/HexSync?style=flat&logo=github&label=复刻

[repo-url]: https://github.com/ForgeStove/HexSync "仓库"
[repo-size-image]:https://img.shields.io/github/repo-size/ForgeStove/HexSync?style=flat&logo=github&label=仓库

[license-url]: https://github.com/ForgeStove/HexSync/blob/main/LICENSE "许可证"
[license-image]: https://custom-icon-badges.demolab.com/github/license/ForgeStove/HexSync?style=flat&logo=law&label=许可证

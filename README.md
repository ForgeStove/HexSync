HexSync
=

[![Github Downloads][download-image]][download-url]
[![Github Hits][hits-image]][hits-url]
[![GitHub stars][stars-image]][stars-url]
[![GitHub issues][issues-image]][issues-url]
[![Github Pulls][pulls-image]][pulls-url]
[![GitHub forks][forks-image]][forks-url]
[![repo-size][repo-size-image]][repo-url]
[![license][license-image]][license-url]

## 项目简介

      HexSync 是一款基于 Java 开发的双端一体的文件同步工具,主要用于在MineCraft客户端和服务端之间同步文件。
      这是一个可执行jar包，双击即可运行，也可以在命令行用指令以无头模式启动，不是MineCraft模组，请不要把此程序放在模组文件夹下。
      新增的exe可执行文件作用与jar包完全一致。
      这个程序包括一个客户端和一个服务器，在同一个界面里，服务器负责发送mod，客户端会将自己文件夹中的mod与服务器的比较，然后更新不一样的部分。

## 功能特性

      提供一个简易的GUI,允许用户配置服务端和客户端,以及监控同步过程中的日志信息。
      使用HTTP协议传输文件，若无公网IP可以使用内网穿透工具穿透TCP隧道来使用。
      允许服务端对上传速率进行限制。
      兼容无头模式。
      仅支持中文。

## 环境要求

      Java 8 及以上版本

## 下载与安装

      下载最新的 HexSync.jar 文件或 HexSync.exe 文件。无需安装。

## 配置

      启动时,会在程序所在的目录下自动创建 HexSync文件夹。
      可以在控制面板的设置内修改并保存。
      配置文件的位置在程序同目录下的 HexSync文件夹下。

## 启动

      通过直接以 Java 打开或者通过命令行执行java -jar HexSync.jar
      在无头环境下会自动切换为无头模式
      如果需要强制启动无头模式需要使用java -jar HexSync.jar -headless
      通过 "设置" 按钮进入设置界面,配置服务端和客户端的相关参数。
      服务端:把需要同步的文件放入同步文件夹内(默认为软件所在的目录中的mods文件夹),点击"启动服务端"。
      客户端:配置设置好后,点击"启动客户端"。

## 开发与贡献

      欢迎贡献代码或者提出问题。创建Pull Request或者提交Issue,我将尽快回复。

## 星标历史
<a href="https://star-history.com/#ForgeStove/HexSync&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=ForgeStove/HexSync&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=ForgeStove/HexSync&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=ForgeStove/HexSync&type=Date" />
 </picture>
</a>

## 贡献者
<a href="https://github.com/ForgeStove/HexSync/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=ForgeStove/HexSync" />
</a>
<a href="https://github.com/donywang922/HexSyncReborn/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=donywang922/HexSyncReborn" />
</a>

---

[download-url]: https://github.com/ForgeStove/HexSync/releases "下载"
[download-image]: https://img.shields.io/github/downloads/ForgeStove/HexSync/total?style=flat-square&logo=github&label=%E6%80%BB%E4%B8%8B%E8%BD%BD%E6%95%B0 "总下载数"

[hits-url]: https://hits.dwyl.com/ "访问量"
[hits-image]: https://custom-icon-badges.demolab.com/endpoint?url=https%3A%2F%2Fhits.dwyl.com%2FTC999%2FAppDataCleaner.json%3Fcolor%3Dgreen&label=%E8%AE%BF%E9%97%AE%E9%87%8F&logo=graph 

[stars-url]: https://github.com/ForgeStove/HexSync/stargazers "星标"
[stars-image]: https://img.shields.io/github/stars/ForgeStove/HexSync?style=flat-square&logo=github&label=星标

[issues-url]: https://github.com/ForgeStove/HexSync/issues "议题"
[issues-image]: https://img.shields.io/github/issues/ForgeStove/HexSync?style=flat-square&logo=github&label=议题

[pulls-url]: https://github.com/ForgeStove/HexSync/pulls "拉取请求"
[pulls-image]: https://custom-icon-badges.demolab.com/github/issues-pr-raw/ForgeStove/HexSync?style=flat&logo=git-pull-request&%3Fcolor%3Dgreen&label=%E6%8B%89%E5%8F%96%E8%AF%B7%E6%B1%82


[forks-url]: https://github.com/ForgeStove/HexSync/fork "复刻"
[forks-image]: https://img.shields.io/github/forks/ForgeStove/HexSync?style=flat-square&logo=github&label=复刻

[repo-url]: https://github.com/ForgeStove/HexSync "仓库地址"
[repo-size-image]:https://img.shields.io/github/repo-size/ForgeStove/HexSync?style=flat-square&label=%E4%BB%93%E5%BA%93%E5%A4%A7%E5%B0%8F

[license-url]: https://github.com/ForgeStove/HexSync/blob/main/LICENSE "许可证"
[license-image]: https://custom-icon-badges.demolab.com/github/license/ForgeStove/HexSync?style=flat&logo=law&label=%E8%AE%B8%E5%8F%AF%E8%AF%81

HexSync
=
---
- 项目简介

      HexSync 是一款基于 Java 开发的文件同步工具，主要用于在客户端和服务端之间同步文件。
      提供一个简单易用的图形用户界面(GUI)，允许用户配置服务端和客户端，以及监控同步过程中的日志信息。
      注意,目前日志文本为硬编码,仅支持中文。

- 功能特性

      文件同步：自动同步服务端与客户端指定目录中的文件。
      日志记录：提供详细的操作日志，便于用户监控和排查问题。
      可配置性：可以通过控制面板配置服务端和客户端，如端口号、同步目录等。

- 系统要求

      Java 8 及以上版本
---
- 下载与安装

      下载最新的 HexSync.jar 文件。
      通过直接以 Java 打开或者通过命令行执行java -jar HexSync.jar启动应用。

- 配置

      启动时，会在程序所在的目录下自动创建HexSync文件夹。
      可以在控制面板的设置内修改并保存。
      配置文件的位置在程序同目录下的HexSync文件夹下。

- 启动
  
      通过 "设置" 按钮进入设置界面，配置服务端和客户端的相关参数
      服务端:把需要同步的文件放入同步文件夹内(默认为软件所在的目录中的mods文件夹),点击"启动服务端"。
      客户端:配置设置好后,点击"启动客户端"。
---
- 开发与贡献

      欢迎贡献代码或者提出问题。创建Pull Request或者提交Issue，我将尽快回复。

- 许可证

      本项目采用 GNU 通用公共许可证 V3（GPLv3），具体信息请参阅 LICENSE 文件。

---

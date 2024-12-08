HexSync
=
## 项目简介

      HexSync 是一款基于 Java 开发的双端一体的文件同步工具,主要用于在MineCraft客户端和服务端之间同步文件。
      这是一个可执行jar包，双击即可运行，也可以在命令行用指令以无头模式启动，不是MineCraft模组，请不要把此程序放在模组文件夹下。
      新增的exe可执行文件作用与jar包完全一致。
      这个程序包括一个客户端和一个服务器，在同一个界面里，服务器负责发送mod，客户端会将自己文件夹中的mod与服务器的比较，然后更新不一样的部分。

## 功能特性

      提供一个简易的GUI,允许用户配置服务端和客户端,以及监控同步过程中的日志信息。
      使用HTTP协议传输文件，若无公网IP可以使用内网穿透工具穿透TCP隧道来使用。
      兼容无头模式。
      仅支持中文。

## 环境要求

      Java 8 及以上版本

## 下载与安装

      下载最新的 HexSync.jar 文件。无需安装。

## 配置

      启动时,会在程序所在的目录下自动创建 HexSync文件夹。
      可以在控制面板的设置内修改并保存。
      配置文件的位置在程序同目录下的 HexSync文件夹下。

## 启动

      通过直接以 Java 打开或者通过命令行执行java -jar HexSync.jar
      如果需要强制启动无头模式需要使用java -jar HexSync.jar -headless
      通过 "设置" 按钮进入设置界面,配置服务端和客户端的相关参数。
      服务端:把需要同步的文件放入同步文件夹内(默认为软件所在的目录中的mods文件夹),点击"启动服务端"。
      客户端:配置设置好后,点击"启动客户端"。

## 开发与贡献

      欢迎贡献代码或者提出问题。创建Pull Request或者提交Issue,我将尽快回复。

## 许可证

      本项目采用 GNU 通用公共许可证 V3（GPLv3）,具体信息请参阅 LICENSE 文件。


## 贡献者
<a href="https://github.com/ForgeStove/HexSync/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=ForgeStove/HexSync" />
</a>
<a href="https://github.com/donywang922/HexSyncReborn/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=donywang922/HexSyncReborn" />
</a>

---

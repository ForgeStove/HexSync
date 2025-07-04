<!--suppress HtmlDeprecatedAttribute -->
<p align="center"><img src="icon.ico" alt="icon"></p>

# <p align="center">HexSync

---
[![License](https://img.shields.io/github/license/ForgeStove/HexSync?style=flat&color=900c3f)](https://github.com/ForgeStove/HexSync?tab=readme-ov-file#MIT-1-ov-file)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/ForgeStove/HexSync)

## 项目简介

- `HexSync` 是一款基于 `Java` 的双端文件同步工具，专为 `Minecraft` 客户端与服务端之间的文件同步设计。
- 通过可执行 Jar 文件运行，支持双击启动或命令行启动（需 Java 17 及以上环境），无需放入模组文件夹。
- 服务器端负责文件分发，客户端自动比对并同步本地与服务器的文件夹内容，缺失文件自动补全，多余文件自动清理，确保两端文件一致。
- 支持自动启动、无头模式、速率限制、中文界面，适合需要高效同步模组文件的 MC 服务器与玩家。

## 功能特性

- 简洁界面，便于配置服务端和客户端参数，并实时监控同步日志。
- 基于 `HTTP` 协议传输文件，支持通过内网穿透工具实现远程同步。
- 服务端可限制上传速率，保障带宽安全。
- 支持无头模式，适配服务器环境。
- 全中文界面，操作友好。

## 环境要求

- 需要安装 `Java 17` 及以上版本。

## 配置说明

- 首次启动会在程序目录下自动创建 `HexSync` 文件夹，并生成配置和日志文件。
- 支持自动启动服务端/客户端，自动启动时可隐藏界面并自动执行同步。
- 所有配置均可在控制面板设置界面修改并保存。

### 服务端设置

- 端口号（默认 `65535`）
- 上传速率限制（`0` 为无限制，支持 `B/s`、`KB/s`、`MB/s`、`GB/s`）
- 同步文件夹路径（默认 `mods`，支持绝对/相对路径，自动创建文件夹）
- 自动启动服务端（默认否）

### 客户端设置

- 端口号（默认 `65535`）
- 服务器地址（默认 `localhost`，请勿包含端口号，可省略 `http://` 前缀）
- 同步文件夹路径（默认 `mods`）
- 仅客户端模组文件夹路径（默认 `clientOnlyMods`，需手动更新，仅客户端模组放此文件夹）
- 自动启动客户端（默认否）

## 启动方式

- 可直接双击 Jar 文件或命令行启动：

```bash
  java -jar HexSync.jar
```

- 无头环境下自动切换为无头模式，如需手动切换可加参数：

```bash
  java -Djava.awt.headless=true -jar HexSync.jar
```

- 通过“设置”按钮进入设置界面，配置服务端和客户端参数。
- 服务端：将需同步文件放入同步文件夹（默认 `mods`），点击“启动服务端”。
- 客户端：配置完成后，将仅客户端模组放入 `clientOnlyMods` 文件夹（或自定义路径），点击“启动客户端”。

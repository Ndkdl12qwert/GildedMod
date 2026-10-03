# GildedMod

> **鎏金神权 · 双 Agent 架构 · 五层技术栈**
> 注:开源部分大部分由Ai辅助

[![License: LGPL v3+](https://img.shields.io/badge/License-LGPLv3+-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0.html)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)]()
[![Forge](https://img.shields.io/badge/Forge-47.4.13-orange.svg)]()
[![Java](https://img.shields.io/badge/Java-17-red.svg)]()

[![Native](https://img.shields.io/badge/Native-DLLs-blueviolet.svg)](https://github.com/Ndkdl12qwert/Gilded-NativeLibrary)

---

## 这是什么

**GildedMod** 是一个把 **JVMTI** 和 **Java Agent** 同时用于 Minecraft 的实验性 Mod。

它不是一个普通的 Mixin Mod——它是一个**五层技术栈的完整 demo**：

- **Mixin** — 编译期字节码注入
- **Java Agent** — 运行时 `retransformClasses`
- **JVMTI** — JVM 内部观察类加载 / native 绑定
- **JNI** — 原生直写内存
- **Forge** — Mod 主体


---

## 特性

### 鎏剑 · GildedEX

| 功能 | 说明 |
|---|---|
| **左键** | 一击必杀 —— 穿透一切 Java 层防护 |
| **右键** | 神权·天罚 —— 范围秒杀 |
| **手持** | 自动飞行 |
| **耐久** | 永不磨损 |

### 鎏金甲

| 功能 | 说明 |
|---|---|
| **全套** | 免疫一切伤害 |
| **防删除** | 无法被移除 |
| **防选中** | 无法被准星锁定 |

### 鎏金工具

| 功能 | 说明 |
|---|---|
| **镐 / 斧** | 超高挖掘速度 |
| **范围采集** | 右键范围破坏 |

### 压缩下界合金块

- **1 ~ 9 层 + FINAL**
- **硬度从 50 到 1500**
- **抗爆 36000**

### 自定义 Tooltip

- **完全接管渲染** —— 不是叠加，是替换
- **鎏金边框** —— 呼吸金光
- **黑金渐变背景** —— 半透明

---

## 技术架构
- Layer 5 : Forge Mod ← 业务逻辑
- Layer 4 : Mixin │ ← 编译期字节码
- Layer 3 : Java Agent (ASM) │ ← 运行时 retransform
- Layer 2 : JVMTI (C++) │ ← JVM 内部观察(闭源)
- Layer 1 : JNI (C++) │ ← 原生直写内存(闭源)

| 层 | 技术 | 作用 |
|---|---|---|
| **Mixin** | `@Inject` / `@Accessor` | 客户端 / 服务端字节码 |
| **Java Agent** | ASM + `retransformClasses` | 运行时改字节码 |
| **JVMTI** | `ClassFileLoadHook` / `NativeMethodBind` | 观察类加载 + native 绑定 |
| **JNI** | `SetFloatField` / `SetBooleanField` | 直接写字段 |
| **Forge** | 事件 / 注册 / 渲染 | Mod 主体 |

### 双 Agent 通信

**Java Agent** 和 **JVMTI Agent** 通过 `LiuAgentBus` 通信：

┌─────────────────┐ ┌─────────────────┐
│ JVMTI Agent     │ │    Java Agent   │
│ (liu_jvmti.dll) │←→│ (agent.jar)    │
│       JNI       │ │                 │
│ ClassFileHook   │ │ LiuTransformer  │
│ NativeBind      │ │ LiuAgentHook    │
└─────────────────┘ └─────────────────┘
          ↑                   ↑
└─────── LiuAgentBus (静态类) ─────────┘


---

## 安装

### 前置

- **Minecraft 1.20.1**
- **Forge 47.4.13**
- **JDK 17**
- **NativeLibrary**：[Gilded-NativeLibrary](https://github.com/Ndkdl12qwert/Gilded-NativeLibrary) 👈 **必看**

### 步骤

**0.下载两个仓库的文件**
| 来源 | 下载什么 |
|---|---|
| [GildedMod Releases](https://github.com/Ndkdl12qwert/GildedMod/releases) | `blackgoldmod-2.0.jar`<br>`blackgoldmod-2.0-agent.jar` |
| [Gilded-NativeLibrary Releases](https://github.com/Ndkdl12qwert/Gilded-NativeLibrary/releases) | `liu_jvmti.dll`<br>`liu_armor_core.dll` |

**1. 把以下文件放入 `.minecraft/mods/`：**

GildedMod-2.0.jar
liu_jvmti.dll
liu_armor_core.dll
liu_core.dll

**2. 把以下文件放入任意位置（比如 `.minecraft/`）：**

gildedmod-2.0-agent.jar

**3. 启动器的 JVM 参数加：**

-javaagent:C:路径\gildedmod-1.0-agent.jar 
-Dliu.agent.path="路径\gildedmod-1.0-agent.jar" 
-agentpath:路径\liu_jvmti.dll 
-Dliu.jvmti.path=路径\liu_jvmti.dll

> 重要提醒：如果您使用PCL2启动器游玩，在某些版本可能参数无法正常传递给JVM，所以在您的PCL有此情况下，推荐导出Bat脚本并修改后启动

---

## 构建

### 克隆项目

```bash
git clone https://github.com/Ndkdl12qwert/GildedMod.git
cd GildedMod
```
**编译java部分**

```Powershell
./gradlew clean build agentJar
```

产物

build/libs/
├── gildedmod-1.0.jar          ← 主 mod
└── gildedmod-1.0-agent.jar    ← Java Agent

## 依赖

本 Mod 的 C++ 部分依赖以下运行时库（**MinGW-w64 UCRT64 runtime**）：

| DLL | 大小 | 来源 |
|---|---|---|
| `libgcc_s_seh-1.dll` | ~100 KB | MinGW-w64 |
| `libstdc++-6.dll` | ~2 MB | MinGW-w64 |

**这些 DLL 随 Release 一起发布。** 如果你是从源码编译，需要自己从
MinGW-w64（UCRT64）的 `bin/` 目录复制。

### 检查依赖是否齐全

**方法 1：看日志**

启动游戏后，在 `logs/latest.log` 里搜 `LIU-JVMTI`：

- **看到 `[LIU-JVMTI] Agent_OnLoad`** —— 依赖齐全 ✅
- **看不到，且日志里有 `Can't find dependent libraries`** —— 依赖缺失 ❌

**方法 2：用 `objdump` 检查**

```bash
objdump -p liu_jvmti.dll | grep "DLL Name"
```
输出会列出所有依赖。确认这些 DLL 都在 mods/ 目录里：
DLL Name: libgcc_s_seh-1.dll      ← 必须在 mods/
DLL Name: libstdc++-6.dll         ← 必须在 mods/
DLL Name: KERNEL32.dll            ← 系统自带
DLL Name: api-ms-win-crt-*.dll    ← 系统自带（Windows 10+）

**方法 3：用 Dependencies.exe**

下载 Dependencies —— 拖 liu_jvmti.dll
进去 —— 红色 = 缺失，绿色 = 有。

**常见错误**
text
Error occurred during initialization of VM
Could not find agent library liu_jvmti.dll in absolute path,
with error: Can't find dependent libraries

原因：libgcc_s_seh-1.dll 或 libstdc++-6.dll 缺失。

解决：把这两个 DLL 放进 mods/ 目录（和 liu_jvmti.dll 同目录）。

**许可**

Copyright (C) 2026 Onicox

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

本项目采用 GNU General Public License v3.0 or later。

- 你可以：使用、学习、修改、分享

- 你必须：修改后开源、保留版权、同样 GPLv3+

- 你不可以：闭源分发、售卖 Mod 本体、违反 Minecraft EULA

**Java 部分** —— GNU Lesser General Public License v3.0 or later

**C++ 部分**（`liu_jvmti.dll` / `liu_core.dll` / `liu_armor_core.dll`）—— **专有闭源**，仅以编译后的二进制形式提供。

### 你可以

- 使用、学习、修改、分享 **Java 部分**
- 在自己的项目中 **链接**（动态调用）本 mod 的 DLL

### 你必须

- 修改 Java 部分后 **开源**
- 保留 **版权声明**
- Java 部分的修改继续 **LGPLv3+**

### 你不可以

- **闭源分发 Java 部分**
- **违反 Minecraft EULA**

### C++ 部分

C++ 源码 **不开源**。如需合作 / 授权，请联系作者。

**免责声明**
- 本 Mod 为技术实验，仅用于单机 / 朋友开黑。

- 请勿在公开服务器使用 —— 可能被反作弊记录或封号

- 请勿用于商业用途 —— 违反 Minecraft EULA

- 作者不对任何后果负责

贡献
欢迎：

提交 Issue —— 报告 Bug / 建议

提交 PR —— 改进代码 / 文档

写教程 —— 教别人怎么用

本项目不接受金钱赞助 —— 只接受技术贡献。

致谢
MinecraftForge — Mod 加载框架

SpongePowered Mixin — 字节码注入

ASM — 字节码操作

OW2 — ASM 维护

MSYS2 / MinGW-w64 — C++ 编译工具链

版本历史
v2.0 — 2026-10-02
首次公开发布

双 Agent 架构（Java Agent + JVMTI）

鎏金神权体系

自定义 Tooltip（Level 3 完全接管）

压缩下界合金块（1~9 层 + FINAL）

Made with  by Onicox

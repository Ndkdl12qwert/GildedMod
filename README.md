# GildedMod

> **鎏金神权 · 原生内核 · 六层技术栈**
> 注：开源部分大部分由 AI 辅助完成

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)]()
[![Forge](https://img.shields.io/badge/Forge-47.4.13-orange.svg)]()
[![Java](https://img.shields.io/badge/Java-17-red.svg)]()
[![Java License: LGPLv3+](https://img.shields.io/badge/Java%20License-LGPLv3+-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0.html)
[![Kernel: Proprietary](https://img.shields.io/badge/Kernel-Proprietary%20(closed)-critical.svg)]()

---

## 这是什么

**GildedMod** 是一个把 **Mixin / Java Agent / JVMTI / JNI / JVM 本体补丁** 串起来用于
Minecraft 的实验性 Mod。它不是普通的 Mixin Mod —— 它把"控制点"一路下推到 **JVM 内核层**。

### 六层技术栈

| 层 | 技术 | 作用 |
|---|---|---|
| **6** | **JVM 本体补丁** | 改 `jvm.dll` 导出函数首字节（`JVM_Halt` / `JVM_GC`）|
| **5** | **JVMTI ClassFileLoadHook** | 比任何 Java 层都早地拿到每个类的原始字节；**Native 全返回** |
| **4** | **JVMTI / RegisterNatives** | 显式绑定 native 方法、强制提前返回（能力可用时）|
| **3** | **JNI** | 字段直写、四路移除、GC 安全解析链 |
| **2** | **Java Agent (ASM)** | 运行时字节码改写 / 攻击方法拦截 |
| **1** | **Mixin / Forge** | 事件、渲染、免伤守卫、游戏内容 |

---

## 特性

### 鎏剑 · GildedEX

| 功能 | 说明 |
|---|---|
| **左键** | 一击必杀 —— 对**绝大多数实体**有效（四条移除路径同时执行）|
| **右键** | 神权·天罚 —— 范围秒杀 |
| **手持** | 自动飞行 |
| **耐久** | 永不磨损 |

> **诚实说明**：对**主动反制型实体**（例如同装 pig2mod 的 `Pig2`：
> 它 override 了 `getHealth` / `setRemoved` / `remove`，每 tick 回血并把 `deathTime` 清零），
> 击杀链会被它逐层抵消。这类实体只能被**压制**（不动、不打、0 血），无法保证消失。

### 鎏金甲

| 功能 | 说明 |
|---|---|
| **全套** | 免疫一切伤害（四层守卫 + `isInvulnerable` + 每 tick 满血）|
| **防删除** | 无法被移除 |
| **防选中** | 无法被准星锁定 |

### 鎏金工具 / 压缩下界合金块

| 功能 | 说明 |
|---|---|
| **镐 / 斧** | 超高挖掘速度 + 右键范围破坏 |
| **压缩块** | 1 ~ 9 层 + FINAL，硬度 50 → 1500，抗爆 36000 |

### 自定义 Tooltip

完全接管渲染（替换而非叠加）：鎏金呼吸边框 + 黑金半透明渐变底。

---

## 原生内核 · GildedNativeKernel

`GildedNativeKernel.dll`（约 **4,150 行有效 C++**）是本项目的核心，
它在**游戏启动的最早期**介入 JVM。

### 能力

| 能力 | 说明 |
|---|---|
| **ClassFileLoadHook** | 在读取原始字节的最早时刻观察 / 改写任意类 |
| **Native 全返回** | 让指定类的所有方法直接返回默认值（见 `fullreturn.txt`）|
| **RegisterNatives** | 显式绑定 native 方法，彻底避免 `UnsatisfiedLinkError` |
| **JVM 本体补丁** | `JVM_Halt` / `JVM_GC` 首字节改为 `ret`：拦截关服请求、吞掉显式 GC |
| **血量字段直写** | 沿 `entityData → DataItem → Float.value` 解析链写入，**不缓存地址**，GC 安全 |
| **四路移除** | `discard()` / 最大伤害 `hurt` / `PlayerList.remove` / 世界实体表 |
| **GC 安全追踪** | 只持有 `GlobalRef`，每次重新解析，不保存任何裸地址 |

### 运行时位置

```
%LOCALAPPDATA%\GildedEX\native\GildedNativeKernel.dll     ← 游戏实际加载的内核
%LOCALAPPDATA%\GildedEX\native\GildedArmor.dll            ← 护甲守卫
%LOCALAPPDATA%\GildedEX\native\gilded_jvmti.log           ← 内核日志（UTF-8）
```

日志前缀统一为 `[GildedNativeKernel] <部分> <数据>`，例如：

```
[GildedNativeKernel] [L2] ClassFileLoadHook 安装 ✓
[GildedNativeKernel] [CP] RegisterNatives 完成：6 个方法已显式绑定 ✓
[GildedNativeKernel] [KERNEL-JVM] 安装结果：2 个补丁生效（halt=1 gc=1）
[GildedNativeKernel] [ANNIHILATE] steps=11 (discard=1 hurt=1 playerlist=0 entitymgr=1)
```

---

## 安装（两步）

### 前置

- Minecraft **1.20.1** + Forge **47.4.13**
- **JDK 17**（启动器里选 Java 17）

### 步骤

**1. 把两个 jar 放进 `.minecraft/mods/`**

```
gildedmod-2.0.jar
gildedmod-2.0-agent.jar
```

**2. 用附带启动器启动（二选一）**

| 方式 | 说明 |
|---|---|
| **`Launch-GildedKernel.cmd`** ★推荐 | 自动请求管理员 → UTF-8 控制台 → 大页/高优先级 → 注入式启动 |
| **`GildedLauncher.exe --level2`** | 直接注入 `gilded_hook.dll`（内核 DLL 由 hook 自行解压）|

> **不需要**手动放任何 DLL 到 `mods/`，**不需要** `-javaagent`，
> 也**不需要**手动配 `-agentpath` —— hook 会把内核解压到 `%LOCALAPPDATA%\GildedEX\native\` 并接管。

如果你坚持用启动器（PCL2 / HMCL）直接启动，请在 JVM 参数里加：

```
-agentpath:<绝对路径>\GildedNativeKernel.dll
```

---

## 配置

### `config/gildedmod/kernel.properties`

```properties
# 周期性自愈间隔（tick）。0 = 关闭（推荐）
# ⚠ 每次执行会 retransform ~150 个类，会短暂 STW，游戏会卡 0.2~1 秒
selfheal.interval=0

# 攻击方法拦截：把其它 mod 的"攻击型"方法体替换成 return false
# 已排除渲染相关类/方法（否则实体会全部不可见）
block.attacks=false

# 全局伤害否决：只保护"穿 4 件鎏金甲 或 手持鎏金剑"的玩家
block.all.hurt=true

# JVM 本体补丁（JVM_Halt / JVM_GC 首字节改写）
kernel.jvm.patch=true
```

### `config/gildedmod/fullreturn.txt`

一行一个类的内部名（斜杠分隔）。命中的类**所有方法**都会直接返回默认值
（`<init>` / `<clinit>` 与移除相关方法保留）。

```
kakiku/pig2mod/entity/Pig2
```

也支持自动模式：写 `*` 表示"类名含 attack / hurt / damage / combat / kill / weapon 的 mod 类全部全返回"。

> **注意**：对**被 Mixin 注入过的类**使用全返回会与 Mixin 的校验冲突 → 崩溃。
> 请只对自己确认安全的目标使用。

---

## 构建

### 一键（推荐）

```bat
rebuild.bat
```

依次完成：编译 C++ → 拷贝到运行时目录 → 编译 jar → 部署到 `mods/` →
重嵌 `gilded_hook.dll` → 校验一致性。

### 分步

```powershell
# C++
cmake -G Ninja -B build/native-cmake -S src/main/cpp
cmake --build build/native-cmake -j 4

# Java
./gradlew clean jar agentJar
```

产物：

```
build/native-cmake/bin/GildedNativeKernel.dll
build/native-cmake/bin/GildedArmor.dll
build/libs/gildedmod-2.0.jar
build/libs/gildedmod-2.0-agent.jar
```

> C++ 侧已 **静态链接**（`-static -static-libgcc -static-libstdc++`），
> **不需要**额外分发 `libgcc_s_seh-1.dll` / `libstdc++-6.dll`。

---

## 许可与授权

本项目**分两部分授权**。

### 1. Java 部分（`gildedmod-2.0.jar` / `gildedmod-2.0-agent.jar`）

**GNU Lesser General Public License v3.0 or later（LGPLv3+）**

- ✅ 你可以：使用、学习、修改、再分发
- ✅ 你可以：在你自己的 mod 中**链接/调用**本 Mod
- ⚠️ 你必须：修改 Java 部分后**开源**，保留版权声明，继续使用 LGPLv3+
- ❌ 你不可以：闭源分发修改后的 Java 部分，或违反 Minecraft EULA

### 2. 原生内核部分（`GildedNativeKernel.dll` / `GildedArmor.dll`）

**专有软件（Proprietary）· 闭源 · 保留所有权利**

- ✅ **允许**：以二进制形式随你的整合包 / 依赖**原样分发**
- ✅ **允许**：通过公开的 JNI 入口**调用**本内核（作为依赖使用）
- ❌ **禁止**：反编译、反汇编、逆向工程、提取算法
- ❌ **禁止**：修改、打补丁、二次打包、替换内部逻辑
- ❌ **禁止**：分发被修改过的二进制，或声称其为你自己的作品
- ❌ **禁止**：售卖本内核或其衍生品

简而言之：**可以调用，不可以拆开改。**

内核以**编译后的二进制形式**提供，不附带源码。任何绕过上述限制的行为均视为侵权。

### 免责声明

- 本项目为**技术实验**，仅用于单机 / 朋友开黑
- **请勿在公开服务器使用** —— 可能被反作弊记录或封号
- 请勿用于商业用途 —— 违反 Minecraft EULA
- 作者不对任何后果负责

---

## 贡献

- 提交 Issue —— 报告 Bug / 建议
- 提交 PR —— 改进 **Java 部分**或文档
- 写教程 —— 教别人怎么用

本项目**不接受金钱赞助**，只接受技术贡献。

## 致谢

MinecraftForge · SpongePowered Mixin · ASM (OW2) · MSYS2 / MinGW-w64

## 版本历史

### v2.0.1 — 2026-11-08

- **新增原生内核层 `GildedNativeKernel`**：ClassFileLoadHook、RegisterNatives、
  Native 全返回、JVM 本体补丁（`JVM_Halt` / `JVM_GC`）
- **新增免伤体系**：四层守卫 + `isInvulnerable` + 每 tick 满血 + 全局伤害否决
- **新增攻击方法拦截**（已排除渲染相关类与方法）
- **新增配置文件** `kernel.properties` / `fullreturn.txt`
- **移除常驻药水效果与仇恨扫描**（修复生存模式明显卡顿）
- **内核更名**：`GildedJVMTINative.dll` → `GildedNativeKernel.dll`
- 统一日志前缀 `[GildedNativeKernel]`，控制台 UTF-8

### v2.0 — 2026-10-02

- 首次公开发布
- 双 Agent 架构（Java Agent + JVMTI）
- 鎏金神权体系
- 自定义 Tooltip（完全接管渲染）
- 压缩下界合金块（1~9 层 + FINAL）

---

Made with ❤ by Onicox

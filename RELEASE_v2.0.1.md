# GildedNativeKernel v2.0.1

首个带**原生内核层**的版本。

## 新增

- **GildedNativeKernel.dll** 原生内核
  - `ClassFileLoadHook` —— 在任何 Java 层之前拿到每个类的原始字节
  - `RegisterNatives` —— 显式绑定 native 方法，彻底避免 `UnsatisfiedLinkError`
  - **Native 全返回** —— 让指定类的所有方法直接返回默认值（`config/gildedmod/fullreturn.txt`）
  - **JVM 本体补丁** —— `JVM_Halt` / `JVM_GC` 首字节改写：拦截关服请求、吞掉显式 GC
- **免伤体系**：四层守卫 + `isInvulnerable` + 每 tick 满血 + 全局伤害否决
- **攻击方法拦截**：把其它 mod 的"攻击型"方法体替换成 `return false`
  （已排除渲染相关类与方法，避免实体不可见）
- **配置文件** `config/gildedmod/kernel.properties` / `fullreturn.txt`
- 统一日志前缀 `[GildedNativeKernel]`，控制台 UTF-8

## 修复

- 移除常驻药水效果与仇恨扫描（修复生存模式明显卡顿）
- 修复空手攻击被误拦截（全局伤害否决只作用于受保护目标）
- 修复实体渲染被拦截导致所有生物不可见

## 安装

1. 把 `gildedmod-2.0.jar` 和 `gildedmod-2.0-agent.jar` 放进 `mods/`
2. 解压 `gilded-hook-win64.zip`，双击 `Launch-GildedKernel.cmd`

需要 **Java 17** · **Forge 1.20.1 / 47.4.13**

## 资产说明

| 文件 | 用途 |
|---|---|
| `gildedmod-2.0.jar` | 主 Mod |
| `gildedmod-2.0-agent.jar` | Java Agent（ASM 改写层）|
| `gilded-hook-win64.zip` | Windows 启动包：hook + 注入器 + 启动脚本 + 配置文件 + 内核 DLL |

## 已知限制

- 对**主动反制型实体**（如 pig2mod 的 `Pig2`：override 了 `getHealth` / `setRemoved` / `remove`，
  每 tick 回血并清零 `deathTime`）无法保证击杀，只能压制。
- 对**被 Mixin 注入过的类**使用全返回会与 Mixin 校验冲突并崩溃，请勿随意添加名单。
- 内核的 `GildedNativeKernel.dll` / `GildedArmor.dll` 为**闭源二进制**：
  可调用、可原样分发，**禁止反编译或修改**。
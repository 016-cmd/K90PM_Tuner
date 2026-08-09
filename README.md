# K90PM Tuner — 音质优化 Tuner APP（基于 ViPER4Android）

面向 K90 Pro Max 的伴生音质优化 Tuner 应用，负责与 ViPER4Android 风格音效驱动协作，
把卷积脉冲（Convolver）、DDC、均衡器、设备快照等能力封装成一套更易用、更可靠的调音界面，
并为指定设备提供音效的持久化还原与驱动会话管理。

> 本工程**整体采用 GNU GPL v3.0** 授权。

---

## 许可证

本工程（含代码、资源、文档）**基于 GNU General Public License v3.0（GPL-3.0）** 发布，
完整条款见 [`LICENSE`](./LICENSE)。

**为什么是 GPL-3.0？** 本工程中的**音效页 / 音效引擎（`com.k90pm.tuner.v4a2` 及其下层）**
是 ViPER4Android 的**衍生作品**，而所选上游均为 GPL-3.0 授权，因此本派生代码同样以 GPL-3.0 发布。

---

## 版权与派生声明

### 本工程自有部分
下列为**独立开发**的功能（同样置于 GPL-3.0 之下，以便统一授权、整体可合规复用）：

- Tuner 主程序与界面（`com.k90pm.tuner.app` / `...ui`）
- 设备管理 / 设备快照 / 设备切换（`com.k90pm.tuner.data` / `...v4a2.data`）
- 音乐播放管理与自定义服务（`com.k90pm.tuner.music` / `...service`）
- 基于 GPL-3.0 框架之上的 Android 壳、Root/驱动适配、崩溃捕获等运维能力

### 音效引擎（V4A 衍生部分）
下列属于 **ViPER4Android 的衍生作品**，遵循上游 GPL-3.0 授权：

- 音效引擎与会话（`com.k90pm.tuner.v4a2.effect` / `...service` / `...audio` / `...viper`）
- 卷积脉冲（Convolver）、DDC、均衡器、动态系统等音效参数的模型、序列化与驱动下发

### 上游与致谢
| 组件 | 上游仓库 | 许可证 |
|---|---|---|
| App（ViPER4Android，Material 3 版） | https://github.com/likelikeslike/ViPER4Android | ✅ GPL-3.0 |
| Driver（ViPER4Android RE 驱动） | https://github.com/iscle/ViPER4Android-RE | ✅ GPL-3.0 |
| Driver Source（ViPERFX RE 驱动源码） | https://github.com/AndroidAudioMods/ViPERFX_RE | ⚠️ 无 LICENSE 文件，仅开源 |

> **关于 ViPERFX_RE**：该上游仓库当前未随附明确的 LICENSE 文件、其 README 也未声明许可证，
> 我们据其公开的开源属性与所在 V4A 生态引入并使用其思路。若作者（AndroidAudioMods / Martmists / Iscle 等）
> 主张相关著作权并希望我们停止使用，**请联系我们进行移除**，我们将立即按要求处理并更新本声明。

**原创致谢**：ViPER4Android 由社区与原作者（zhuhang / viper520 等社区成员）倾力维护与公开，
本工程基于社区维护的 RE 分支进行派生开发，特此致谢。

---

## 说明与免责

- 本工程为个人/社区音质优化用途，可能与系统音频架构深度耦合，请仅在具备必要知识的前提下使用。
- 你使用、修改、分发本工程时，须遵循 GPL-3.0 的条款，并保留本文件中的版权与来源声明。
- 若您对版权归属或本声明有任何疑问，欢迎通过仓库 Issues 联系我们。

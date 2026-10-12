# 预约门票小程序 - 设计文档

> 版本：v1.0  ·  状态：UI原型阶段（接口待开发）
> 文档目的：作为开发交付文档，明确业务流程、页面结构、数据模型、接口约定、关键交互

---

## 一、项目概述

### 1.1 定位
面向 C 端用户的活动/场馆预约小程序，支持多活动展示、实名预约、二维码核销全流程。
服务端账号体系区分普通用户（type=1）和核销员（type=2），核销员有扫码核销权限。

### 1.2 核心功能
| 模块 | 关键能力 |
| --- | --- |
| 首页 | 活动列表展示、立即预约入口 |
| 活动预约 | 预约须知 → 选择日期时段 → 填表（含随行人）→ 提交 |
| 预约记录 | 个人预约历史、状态查看、二维码展示 |
| 我的 | 登录/编辑资料、关于我们、核销入口（限核销员） |
| 扫码核销 | 核销员扫描客户二维码，查看详情后确认核销 |

### 1.3 选型建议
| 层 | 建议方案 | 说明 |
| --- | --- | --- |
| 框架 | **uni-app**（Vue3 + Vite + TypeScript） | 一套代码可编译为微信小程序、H5、App；生态成熟，uView Plus / uni-ui 组件丰富 |
| 状态管理 | Pinia | 轻量、TS 友好 |
| HTTP | uni.request + 统一拦截器 | 小程序原生能力 |
| 二维码 | `uni.scanCode`（核销扫）+ `uqrcode`（生成） | 客户端扫码、服务端也可以生成 |
| 倒计时 | `setInterval` + `onUnload` 清理 | 全局定时器，注意后台清理 |
| 后端 | 微信小程序云开发 / Spring Boot 均可 | 已有 Spring Boot 经验推荐 Boot |

---

## 二、页面结构与流转

### 2.1 页面清单（uni-app pages）

```
pages/
├── home/index              首页（活动列表）
├── detail/index            预约详情（须知+时段+表单）
├── records/index           预约记录
├── records/detail          预约详情（结果/二维码）
├── mine/index              我的
├── mine/about              关于我们
├── mine/edit               编辑资料
├── login/index             登录
└── scan/
    ├── index               扫码页
    └── result              扫码结果
```

### 2.2 主要流程图

```
            ┌─────────────────────────┐
            │        首页 home         │
            └────────────┬────────────┘
                         │ 立即预约
                         ▼
            ┌─────────────────────────┐
            │  detail（自动弹须知）     │
            └────┬───────────────┬────┘
                 │我已阅读       │×关闭 / 倒计时3min
                 ▼               └─→ 回首页
        ┌──────────────────┐
        │日期时段弹窗       │
        └────┬─────────────┘
             │确定
             ▼
        ┌──────────────────┐
        │填写表单（含随行人）│
        └────┬─────────────┘
             │提交
             ▼
        ┌──────────────────┐
        │ 预约记录 records │
        └──────────────────┘
```

```
未登录 ──→ 任何预约/核销动作 ──→ 跳 login ──→ 微信授权 ──→ 回原页
```

### 2.3 状态栏颜色与导航规划
| 页面 | 状态栏文字 | 导航栏 | 备注 |
| --- | --- | --- | --- |
| 首页 | 深（蓝色背景） | 透明 + 蓝 | 沉浸式 |
| 预约记录 | 深（蓝色导航） | 蓝底白字 | 标准 |
| 我的 | 深（蓝色 banner） | 透明 + 蓝 | 沉浸式 |
| 预约详情 | 浅（白底） | 白底深字 | 沉浸式 |
| 关于我们、扫码 | 浅（白底） | 白底深字 | 标准 |
| 预约结果 | 深（蓝色） | 蓝底白字 | 标准 |

---

## 三、数据模型（已有结构，文档化）

### 3.1 活动表 `pro`
> 对应需求截图：min_hour/start_hour/end_hour/interval/num/day/staff_num/dwell_time 等

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | int(11) PK | 主键 |
| title | varchar(255) | 活动名称 |
| pic | varchar(255) | 封面图 |
| sort | int(11) | 排序，越大越前 |
| min_hour | time | 当天可预约的最晚时间（截止） |
| start_hour | time | 开始时间 |
| end_hour | time | 结束时间 |
| interval | int(11) | 时间段间隔（分钟） |
| num | int(11) | 每个时间段可预约的人数 |
| desc | text | 详细介绍（预约须知富文本） |
| day | int(11) | 可预约未来天数（如2表示可约今、明两天） |
| is_show | tinyint(1) | 是否显示 1是 2否 |
| staff_num | int(11) | 最大随行人数 |
| dwell_time | int(11) | 停留秒数/分钟（参观时长） |
| create_time / update_time | int(11) | 时间戳 |

> 文档化扩展建议：新增 `notice` text（独立于 desc 的预约须知富文本）、`notice_md`（Markdown 版本）、`price` decimal（默认0=免费）。

### 3.2 预约记录表 `reserve`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | int(11) PK | 主键 |
| uid | int(11) | 用户ID |
| pro_id | int(11) | 活动ID |
| ordercode | char(18) | 预约编号（生成二维码用） |
| title | varchar(255) | 服务名称（冗余，活动删除后仍可看） |
| name | varchar(255) | 预约人姓名 |
| tel | varchar(255) | 手机号 |
| card_type | tinyint(1) | 证件类型：1身份证 2护照 3港澳通行证 4台湾通行证 5军官证 |
| id_card | varchar(255) | 证件号码 |
| num | int(11) | 预约人数（含主预约人） |
| status | tinyint(1) | 状态：1待核销 2已完成 3已过期 4已取消 |
| date | datetime | 预约开始时间 |
| end_date | datetime | 预约结束时间 |
| sys_remark | text | 管理员备注 |
| reserve_data | text | 预约+随行人 JSON |
| off_uid | int(11) | 核销员ID |
| off_time | datetime | 核销时间 |
| create_time | int(11) | 时间戳 |

`reserve_data` JSON 示例：
```json
[
  {"name":"屈小敏","tel":"15502953424","id_card":"610423198005060026","card_type":"1","card_type_title":"第二代居民身份证"},
  {"name":"甄哲","tel":"","id_card":"610423198101020024","card_type":"1","card_type_title":"第二代居民身份证"}
]
```

> 文档化扩展建议：补 `uniq_id`（微信唯一ID）、`month_count`（当月已约次数，避免重复约）。

### 3.3 用户表 `user`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | int(11) PK | 主键 |
| number | char(8) | 会员编号 |
| nickname | varchar(255) | 用户昵称 |
| name | varchar(255) | 真实姓名 |
| tel | char(11) | 手机号 |
| headimgurl | varchar(255) | 头像地址 |
| openid | char(32) | openid |
| id_card | char(18) | 身份证号 |
| token | char(32) | API token |
| status | tinyint(1) | 状态 1正常 2拉黑 |
| type | tinyint(1) | 身份 1用户 2核销员 |
| is_verified | tinyint(1) | 是否实名认证 1否 2是 |
| create_time / update_time | int(11) | 时间戳 |

---

## 四、关键交互细节

### 4.1 预约倒计时（**重点**）
- 进入详情页立即启动 `setInterval`，**3 分钟（180s）无操作完毕则退回首页**
- 倒计时显示在 `countdown-bar`（顶部黄色背景条）
- 触发回退的场景：
  1. 倒计时自然归零
  2. 用户手动关闭（点 × 或返回）
  3. 页面 `onUnload`（跳走）→ 清理定时器
- 实现建议：
  ```ts
  let timer: number | null = null
  let remain = 180
  const tick = () => {
    remain.value--
    if (remain.value <= 0) {
      clearTimer()
      uni.navigateBack({ delta: 1, fail: () => uni.switchTab({ url: '/pages/home/index' }) })
    }
  }
  onLoad(() => { timer = setInterval(tick, 1000) })
  onUnload(() => clearTimer())
  ```

### 4.2 预约须知弹窗
- 进入详情页 → `setTimeout(300)` 后弹出（避免与导航动画冲突）
- **必须点"我已阅读"才能选日期时段**
- 关闭弹窗不退出页面，但**倒计时继续**（避免用户绕开）

### 4.3 日期时段动态加载
- 日期：根据活动 `day` 字段生成未来 N 天的可选日期
- 时段：根据 `start_hour / end_hour / interval` 生成（如 09:00-12:00 interval=30 → 09:00/09:30/10:00/10:30/11:00/11:30，共 6 段）
- **约满状态**：需要后端接口返回每个时段剩余号数，前端根据 `num - 已约人数` 判断
- 占位逻辑：选中时段后更新 "时间选择" 区域显示

### 4.4 随行人动态增删
- 上限由活动 `staff_num` 控制
- 添加按钮文案动态展示：`添加随行人 (1/3)` / `(2/3)` / `(3/3)`
- 达到上限时按钮隐藏
- 单条随行人支持删除
- 证件类型与主预约人共用选择器

### 4.5 二维码生成
- 内容：`ordercode`（预约编号，唯一）
- 库：客户端用 `uqrcode` 插件（仅展示），**核销员扫码端必须用 `uni.scanCode`**
- 服务端建议：可生成 PNG 图片存 OSS，H5 也可访问

### 4.6 登录与身份
- 入口：我的页面未登录时显示"点击登录"
- 强制登录：任何预约/核销操作前检测 `state.user.loggedIn`
- 微信登录流程：
  1. `uni.login` → 获取 `code`
  2. 后端用 `code` 换 `openid`
  3. 后端返回 `user` + `token`
  4. 前端存到 `uni.setStorageSync('user', ...)`
- 资料编辑：允许改昵称、头像（重新选）、手机号（短信验证）

### 4.7 核销流程
```
核销员 → 我的 → 核销 → uni.scanCode → 后端解析 ordercode
       → 返回预约详情 → 显示信息 → 点击"确认核销"
       → 后端更新 status=2, off_uid, off_time
```

---

## 五、接口约定（待与后端确认）

> 当前后端尚未提供，本节先给出规范草案，作为前后端协作对齐用。

### 5.1 活动
| 接口 | 方法 | 入参 | 出参 |
| --- | --- | --- | --- |
| `/api/activity/list` | GET | - | `Activity[]`（字段同 pro 表） |
| `/api/activity/detail` | GET | `id` | `Activity & { notice: string, slots: Slot[] }` |
| `/api/activity/slots` | GET | `id, date` | `Slot[]`（每时段剩余号数） |

`Slot` 结构：`{ time: '09:00', start: '2026-09-10 09:00:00', end: '2026-09-10 09:30:00', total: 20, remain: 12 }`

### 5.2 预约
| 接口 | 方法 | 入参 | 出参 |
| --- | --- | --- | --- |
| `/api/reserve/create` | POST | `{pro_id, date, name, tel, card_type, id_card, companions[]}` | `{ordercode, id}` |
| `/api/reserve/list` | GET | `uid` | `Record[]` |
| `/api/reserve/detail` | GET | `id` | `Record & {qrcode_url}` |
| `/api/reserve/cancel` | POST | `id` | `{ok: true}` |

### 5.3 用户
| 接口 | 方法 | 入参 | 出参 |
| --- | --- | --- | --- |
| `/api/user/login` | POST | `{code, encryptedData, iv}` | `{token, user}` |
| `/api/user/profile` | GET | - | `User` |
| `/api/user/update` | POST | `{nickname?, headimgurl?, name?, tel?}` | `User` |

### 5.4 核销
| 接口 | 方法 | 入参 | 出参 |
| --- | --- | --- | --- |
| `/api/scan/verify` | POST | `{ordercode}` | `Record & {qrcode_url}` |
| `/api/scan/confirm` | POST | `{id}` | `{ok: true, record}` |

### 5.5 通用约定
- BaseURL：`https://api.example.com`
- Header：`Authorization: Bearer {token}`
- 业务码：`0` 成功；`401` 未登录；`403` 无权限；`4001` 已预约过（限流）；`4002` 名额已满；`4003` 不在预约时段
- 列表分页：`{page, pageSize}` → `{list, total, hasMore}`
- 时间格式：`YYYY-MM-DD HH:mm:ss`

---

## 六、状态管理（Pinia 草案）

```ts
// stores/user.ts
export const useUserStore = defineStore('user', {
  state: () => ({
    loggedIn: false,
    token: '',
    user: null as User | null,
  }),
  getters: {
    isVerifier: (s) => s.user?.type === 2,
    displayName: (s) => s.user?.nickname || '游客',
  },
  actions: {
    async login(code: string) { /* ... */ },
    async fetchProfile() { /* ... */ },
    async updateProfile(p: Partial<User>) { /* ... */ },
    logout() { /* 清空 + 跳登录 */ },
  },
  persist: true  // 持久化
})

// stores/reserve.ts
export const useReserveStore = defineStore('reserve', {
  state: () => ({
    records: [] as Record[],
    current: null as Record | null,
  }),
  actions: {
    async fetchList() { /* ... */ },
    async create(payload) { /* ... */ },
    async cancel(id) { /* ... */ },
  }
})
```

---

## 七、组件拆分建议

```
components/
├── activity-card/       # 首页活动卡片
├── countdown-bar/       # 顶部倒计时条
├── notice-modal/        # 预约须知弹窗
├── date-picker-modal/   # 日期+时段选择弹窗
├── companion-form/      # 随行人表单（动态增删）
├── card-picker/         # 证件类型选择
├── record-card/         # 预约记录卡片
├── qrcode-card/         # 二维码详情卡片
├── status-badge/        # 状态徽章
└── empty-state/         # 空状态
```

---

## 八、UI 设计规范

### 8.1 主色板
| 用途 | 色值 |
| --- | --- |
| 主色（按钮、导航） | `#3B7DFF` |
| 主色亮 | `#5B95FF`（渐变末端） |
| 成功 | `#00B96B` |
| 警告（倒计时） | `#FF9500` → `#FFB800` |
| 危险 | `#F53F3F` |
| 主文字 | `#1F2329` |
| 次文字 | `#4E5969` |
| 辅助文字 | `#86909C` |
| 占位文字 | `#C9CDD4` |
| 分割线 | `#F2F3F5` |
| 背景 | `#F4F6FA` |

### 8.2 圆角与间距
- 卡片圆角：`16px`
- 按钮圆角：`24px`（小）/ `28px`（主操作）
- 弹窗圆角：`20px 20px 0 0`
- 间距基数：`4 / 8 / 12 / 16 / 20 / 24`

### 8.3 字体
- 标题：17px / 600
- 副标题：15px / 600
- 正文：14px / 400
- 辅助：12px / 400
- 占位：13px / 400 灰色

---

## 十、开发任务拆解（开发阶段路线图）

| 阶段 | 任务 | 预计 |
| --- | --- | --- |
| 第 1 周 | 项目初始化、目录结构、登录流程打通、首页静态 | 5d |
| 第 2 周 | 活动列表 + 预约详情 + 须知弹窗 + 倒计时 + 日期时段 | 7d |
| 第 3 周 | 随行人表单 + 提交 + 预约记录 + 结果详情 + 二维码 | 7d |
| 第 4 周 | 我的 + 编辑资料 + 关于我们 + 核销扫码 + 联调 | 7d |
| 第 5 周 | 优化、文案、loading/empty 状态、兼容测试 | 5d |

---

## 十一、风险与待定项

| 项 | 描述 | 建议 |
| --- | --- | --- |
| 限流规则 | 用户每月最多约 2 次的规则如何落地（前端/后端校验） | 后端校验为主，前端做体验优化 |
| 时段约满状态 | 是预生成还是实时拉？ | 实时拉取 `slots` 接口，前端缓存 30s |
| 跨天预约 | `day=2` 是否包含今天？ | 默认"明天起"，0=仅今天 |
| 实名认证 | 是否强制？`is_verified` 用在哪？ | 进入预约前校验，未认证引导认证 |
| 编辑资料范围 | 头像/昵称/手机都可改，姓名/证件号是否可改？ | 实名后姓名/证件号锁定，避免刷号 |
| 核销员权限 | 一个核销员能扫所有活动还是指定？ | 先做全部，权限细化后期再做 |

---

## 十二、验收清单（开发自测）

- [ ] 首页活动列表正常加载，空状态友好
- [ ] 立即预约 → 弹出须知 → 我已阅读 → 弹出日期时段
- [ ] 倒计时 3 分钟，归零自动回首页
- [ ] 约满时段不可点击
- [ ] 随行人上限限制，按钮动态文案
- [ ] 表单校验：手机号、证件号非空
- [ ] 提交后跳预约记录，新记录在最前
- [ ] 记录详情显示二维码，扫码能识别 ordercode
- [ ] 未登录拦截 → 跳登录 → 登录后回原页
- [ ] 我的页面编辑资料能保存
- [ ] 核销员（type=2）显示核销入口
- [ ] 扫码 → 显示预约详情 → 确认核销 → 状态变为已完成
- [ ] 关于我们内容正确显示
- [ ] iPhone X+ 底部安全区适配（tabbar padding-bottom）
- [ ] 横竖屏、暗黑模式兼容（按需）

---

> **附：原型 HTML 文件**：`prototype.html` —— 可在浏览器中直接打开体验全部页面与交互。后续接口就绪后，可按本文档约定直接对接。
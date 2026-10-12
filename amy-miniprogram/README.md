# 预约门票小程序

基于 **uni-app (Vue3 + Vite + TypeScript + Pinia)** 的活动预约小程序。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 首页 | 活动列表展示、立即预约 |
| 活动预约 | 预约须知 → 日期时段选择 → 填表（含随行人）→ 提交 |
| 预约记录 | 历史预约、状态查看、二维码展示 |
| 我的 | 登录/编辑资料、关于我们、核销入口（核销员） |
| 扫码核销 | 扫描客户二维码、确认核销 |

## 目录结构

```
src/
├── api/           # 业务 API（对接 amy-biz-sun-palace-art-space）
├── stores/        # Pinia 状态（user / reserve）
├── types/         # 类型定义 + 枚举常量
├── utils/         # 工具函数（时间/二维码/请求）
├── components/    # 公共组件
├── pages/         # 页面
├── App.vue
├── main.ts
├── pages.json
└── manifest.json
```

## 快速开始

### 1. 安装依赖
```bash
npm install --registry=https://registry.npmmirror.com
```

### 2. 运行

**H5（浏览器预览）**
```bash
npm run dev:h5
```

**微信小程序**
```bash
npm run dev:mp-weixin
```
然后用微信开发者工具打开 `dist/dev/mp-weixin` 目录。

### 3. 构建
```bash
npm run build:h5           # 构建 H5
npm run build:mp-weixin    # 构建微信小程序
```

## 关键配置

### 后端接口地址
`src/utils/request.ts` 中：
```ts
export const BASE_URL = 'http://localhost:18080'  // 本地联调：网关端口
```
上线前需替换为 HTTPS 域名，并在微信公众平台「开发管理 - 服务器域名」中配置为 request 合法域名。

接口均来自后端模块 `amy-biz-modules/amy-biz-sun-palace-art-space`：

| 功能 | 方法 | 路径 |
| --- | --- | --- |
| 微信登录换 token | POST | `/mina/api/auth/login` |
| 会员信息 | GET | `/mina/app/user/info` |
| 微信手机号绑定 | GET | `/user/phone/get/{jsCode}` |
| 首页可预约项目 | GET | `/mina/app/homepage/project` |
| 预约须知与号源 | GET | `/mina/app/project/reservation/info/{projectId}` |
| 提交预约 | POST | `/mina/app/reservation/submit` |
| 我的预约记录 | GET | `/mina/app/user/reservation/records` |
| 预约详情 | GET | `/mina/app/reservation/{orderNum}` |
| 核销 | POST | `/mina/app/reservation/verify/{orderNum}` |

注意事项：
- 统一返回结构为 `R<T>`，**成功码是 200**（非 0）；分页数据在 `data.rows`。
- 鉴权请求头为 `Authorization: <token>`，**不要加 `Bearer ` 前缀**（后端直接以请求头原文检索登录态）。
- `/mina/**` 需在网关 `security.ignore.whites` 白名单中放行（登录接口无需鉴权）。
- 后端 `WechatController` 使用 `@RestController("/wechat")`，该值在 Spring 中是 Bean 名称而非路径前缀，
  因此实际路径不含 `/wechat`；若后端修正为 `@RequestMapping("/wechat")`，需同步调整 `src/api/user.ts` 中的路径。

### 微信小程序 appid
`src/manifest.json` → `mp-weixin.appid` 目前为 `touristappid`（游客模式），
上线前替换为真实 appid。

## 预约业务规则

- 进入详情页启动 **3 分钟倒计时**，超时自动退回首页
- 预约须知**必须点「我已阅读」**才能进入时段选择
- 时段按 `start_hour/end_hour/interval` 动态生成，**约满时段不可选**
- 随行人数量受活动 `staff_num` 限制
- 证件类型：身份证 / 护照 / 港澳通行证 / 台湾通行证 / 军官证
- 预约状态：1 待核销 / 2 已核销 / 3 已过期 / 4 已取消

## 数据模型

见 `../DESIGN.md` 第 3 节（pro / reserve / user 三张表字段）。

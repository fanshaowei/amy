# Amy UI 前端开发规范

## 1. 技术栈

- 使用 React、TypeScript 与 Umi Max 构建应用，项目结构遵循 Ant Design Pro 约定。
- 页面框架使用 `@ant-design/pro-components`，优先采用 `ProLayout`、`PageContainer`、`ProTable`、`ProForm`、`ProDescriptions` 等 ProComponents。
- 基础交互与展示组件统一使用 `antd`，不得引入其他 UI 组件库替代 Ant Design。
- 图标统一使用 `@ant-design/icons`，不得使用来源不明的图标字体或重复维护图标资源。
- 网络请求统一使用 Umi Max `request`，接口地址、请求方法、参数、响应结构必须与现有若依后端保持兼容。
- 全局状态优先使用 Umi Max `initialState`、`useModel` 与页面局部状态；只有确有必要时才新增状态方案。
- 样式使用 Less/CSS Modules 或组件 Token，禁止通过大范围全局选择器覆盖 Ant Design 内部样式。

## 2. Ant Design 开发原则

- 遵循 Ant Design 设计价值：自然、确定、意义、生长；界面行为应清晰、一致且可预测。
- 优先通过 `ConfigProvider` 主题 Token 调整视觉风格，不直接修改组件内部类名。
- 表单使用 `ProForm` 或 Ant Design `Form`，必须提供标签、校验、错误反馈、加载态和防重复提交能力。
- 数据列表优先使用 `ProTable`，统一支持查询、重置、分页、刷新、列展示、批量操作及空状态。
- 新增、编辑等短表单优先使用 `ModalForm` 或 `DrawerForm`；详情优先使用 `ProDescriptions` 或 Drawer。
- 删除、停用、重置等高风险操作必须二次确认，并明确说明操作对象与影响。
- 消息反馈使用 `message`、`notification` 或 `App.useApp()`，不得使用浏览器原生 `alert`。
- 页面必须正确处理加载、成功、失败、空数据、无权限和网络异常状态。
- 组件尺寸、间距、颜色和圆角优先使用 Ant Design Token，保持后台系统视觉一致。
- 所有交互元素必须具备明确的禁用态、加载态、焦点态，并支持键盘操作。

## 3. 页面与组件结构

- 页面文件放在 `src/pages`，公共业务组件放在 `src/components`，接口定义放在 `src/services`。
- 每个系统管理子模块独立目录，页面、局部组件、类型和测试就近组织，禁止形成超大单文件。
- 页面组件只负责页面编排；复杂表单、树选择器、权限选择器、详情面板应拆分为独立组件。
- 公共请求参数、分页结果、若依响应体、用户与权限类型集中定义并复用。
- 组件与函数使用具名导出；页面入口可使用默认导出以符合 Umi 路由加载约定。
- React 组件使用 PascalCase，hooks 使用 `use` 前缀，变量与函数使用 camelCase，常量使用 UPPER_SNAKE_CASE。
- 禁止使用 `any` 逃避类型检查；后端动态数据应使用明确接口、泛型或 `unknown` 后再收窄。

## 4. 若依兼容要求

- 不修改后端接口，不改变 URL、HTTP Method、参数名、分页字段和响应字段。
- 继续使用若依 `Authorization: Bearer <token>` 认证方式，并兼容现有 token 存储与退出流程。
- 保留 `isToken`、`repeatSubmit` 等现有请求语义，在统一请求层实现等价能力。
- 保留后端菜单、角色和按钮权限标识；按钮通过统一权限组件或 hook 控制显示与可用状态。
- 保留若依字典机制，状态、类型等枚举展示统一从字典接口获取，不在页面重复硬编码。
- 文件上传、下载和导出必须兼容后端 Blob 响应、文件名解析和错误响应格式。
- 动态路由必须兼容后端返回的菜单树、隐藏菜单、外链、缓存、图标和权限配置。

## 5. 登录与安全

- 登录页必须兼容账号、密码、验证码、验证码 UUID、记住账号及登录后重定向。
- 密码与敏感信息不得写入日志、URL、埋点或错误提示；记住密码时必须沿用安全加密方案。
- 401 响应统一清理会话并跳转登录页；无权限页面使用 Ant Design `Result` 展示。
- 请求层统一处理重复提交、超时、业务错误码和网关异常，页面不得重复实现同类逻辑。
- 所有用户输入必须经过表单校验；展示后端富文本时必须先进行安全过滤。

## 6. 可访问性与响应式

- 表单控件必须有关联标签，图标按钮必须提供 `aria-label` 或 Tooltip。
- 图片必须提供替代文本；仅装饰图片使用空 `alt`。
- 颜色不能作为唯一状态提示，状态同时使用文字、图标或标签表达。
- 登录页、列表页、弹窗和抽屉需适配常见桌面分辨率与窄屏场景。
- 表格在窄屏下应提供横向滚动或精简列，禁止因固定宽度导致页面不可操作。

## 7. 质量与验证

- TypeScript 必须通过类型检查，ESLint 与 Prettier 必须通过项目配置。
- 每完成一个功能模块，至少验证：页面可访问、查询、分页、新增、编辑、删除、权限控制和异常反馈。
- 对请求转换、权限判断、菜单转换等关键纯函数编写单元测试。
- 对登录及系统管理核心流程编写可重复执行的端到端或集成验证。
- 每个功能模块验证通过后单独创建 Git 提交，提交内容不得混入其他模块或仓库已有修改。
- 提交信息使用清晰的 Conventional Commits 风格，例如 `feat(amy-ui): implement login`。

## 8. 官方参考

- Ant Design Pro：`https://preview.pro.ant.design/welcome`
- Ant Design Components：`https://ant.design/components/overview/`
- Ant Design ProComponents：`https://procomponents.ant.design/`


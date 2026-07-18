# Northstar 界面设计规范

前台选品看板与管理后台共用 `frontend/src/style.css` 的设计令牌和组件，不再维护两套互相冲突的样式。

## 视觉原则

1. **内容优先**：深色主视觉只用于 Hero/欢迎区，业务内容使用安静的中性色表面。
2. **真实状态可见**：真实、演示、待配置、成功和失败必须有文字，不只依赖颜色。
3. **少层级卡片**：一个业务块最多一层主卡片，避免卡片里继续堆多层边框。
4. **统一圆角**：控件 10–12px，普通卡片 16–20px，主视觉/大面板 24–30px。
5. **响应式优先**：桌面固定侧栏，窄屏抽屉；表格横向滚动，表单不挤成不可读的小列。

## 主题

- 浅色背景：`#f3f5f8`，主表面：`#ffffff`，正文：`#101828`。
- 深色背景：`#080b12`，主表面：`#121722`，正文：`#f4f7fb`。
- 品牌蓝：浅色 `#0a84ff`，深色 `#409cff`。
- 主题保存在 `localStorage`；首次访问跟随系统 `prefers-color-scheme`。
- 组件只使用 CSS 变量，禁止在业务组件新增独立的浅/深色硬编码覆盖。

## 布局

- 桌面侧栏：268px；主内容最大宽度：1480–1540px。
- 页面边距使用 `clamp(18px, 3vw, 42px)`。
- `<= 980px` 侧栏变为抽屉。
- `<= 720px` 指标、表单、筛选器逐步变为两列或单列。

## 弹窗硬性规则

```css
.modal-backdrop { position: fixed; inset: 0; overflow: auto; padding: 24px; }
.modal-dialog { max-height: calc(100dvh - 48px); display: flex; flex-direction: column; overflow: hidden; }
.modal-scroll { min-height: 0; overflow-y: auto; }
.modal-actions { flex: 0 0 auto; }
```

- 弹窗挂载到 `body`（Vue `Teleport`）。
- 打开时锁定 `body` 滚动，关闭时恢复。
- 支持 Esc 和点击遮罩关闭。
- 标题、关闭按钮、底部操作始终可见，只有正文滚动。

## 可访问性与交互

- 所有图标按钮有 `aria-label` 或 `title`。
- 键盘焦点使用清晰的 3px 蓝色焦点环。
- 触控按钮高度建议不低于 40px。
- 支持 `prefers-reduced-motion`。
- 表格状态、数据源状态、真实/Demo 标识都包含文字。

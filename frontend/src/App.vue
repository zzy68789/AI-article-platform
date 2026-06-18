<script setup lang="ts">
import { onMounted, provide } from 'vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import {
  registerGenerationLeaveListener,
  resumeStoredGenerationSession,
} from '@/utils/articleGenerationLifecycle'

// 提供全局中文语言配置
provide('locale', zhCN)

onMounted(() => {
  registerGenerationLeaveListener()
  resumeStoredGenerationSession()
})
</script>

<template>
  <a-config-provider :locale="zhCN">
    <BasicLayout />
  </a-config-provider>
</template>

<style>
/* 引入 Google Fonts - Editorial Creator Workspace */
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500;600;700&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap');

/* 全局CSS变量 - 纸感内容创作工作台 */
:root {
  /* 品牌色 - 墨色与靛蓝 */
  --color-primary: #4F46E5;
  --color-primary-light: #818CF8;
  --color-primary-dark: #3730A3;
  --color-secondary: #1F2937;
  --color-secondary-light: #4B5563;
  --color-accent: #D97706;

  /* 功能色 */
  --color-cta: #D97706;
  --color-success: #0F766E;
  --color-warning: #D97706;
  --color-error: #EF4444;
  --color-info: #2563EB;

  /* 中性色 */
  --color-background: #F7F4ED;
  --color-background-secondary: #FBFAF7;
  --color-background-tertiary: #EEE7DA;
  --color-background-dark: #111827;
  --color-surface: #FFFDF7;
  --color-surface-muted: #F3EFE6;
  --color-text: #111827;
  --color-text-secondary: #4B5563;
  --color-text-muted: #7C756B;
  --color-border: #DDD4C4;
  --color-border-light: #EAE2D4;

  /* 渐变 */
  --gradient-primary: linear-gradient(135deg, #4F46E5 0%, #3730A3 100%);
  --gradient-hero: linear-gradient(180deg, #FFFDF7 0%, #F7F4ED 100%);
  --gradient-hero-dark: linear-gradient(180deg, #1F2937 0%, #111827 100%);

  /* 纯色背景 */
  --bg-primary: #4F46E5;
  --bg-primary-hover: #3730A3;
  --bg-dark: #111827;
  --bg-dark-hover: #1F2937;
  --bg-success: #0F766E;

  /* 阴影 */
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 10px 30px rgba(31, 41, 55, 0.08);
  --shadow-lg: 0 18px 45px rgba(31, 41, 55, 0.12);
  --shadow-xl: 0 28px 70px rgba(31, 41, 55, 0.16);
  --shadow-card: 0 1px 0 rgba(17, 24, 39, 0.04), 0 10px 24px rgba(31, 41, 55, 0.06);
  --shadow-card-hover: 0 16px 38px rgba(79, 70, 229, 0.14);
  --shadow-green: 0 10px 24px rgba(79, 70, 229, 0.22);

  /* 圆角 */
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-2xl: 24px;
  --radius-full: 9999px;

  /* 过渡 */
  --transition-fast: 150ms ease-out;
  --transition-normal: 200ms ease-out;
  --transition-slow: 300ms ease-out;

  /* 纸感材质 */
  --glass-bg: rgba(255, 253, 247, 0.92);
  --glass-bg-green: rgba(255, 253, 247, 0.86);
  --glass-blur: blur(10px);
  --paper-texture:
    radial-gradient(circle at 1px 1px, rgba(17, 24, 39, 0.045) 1px, transparent 0),
    linear-gradient(180deg, rgba(255, 255, 255, 0.7), rgba(247, 244, 237, 0.75));
}

/* 全局样式优化 */
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Plus Jakarta Sans', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
  background-color: var(--color-background);
  background-image: var(--paper-texture);
  background-size: 18px 18px, 100% 100%;
  color: var(--color-text);
  line-height: 1.6;
}

html {
  overflow-x: hidden;
}

h1, h2, h3, h4, h5, h6 {
  font-family: 'Plus Jakarta Sans', 'Noto Sans SC', sans-serif;
  letter-spacing: 0;
}

/* 全局链接样式 */
a {
  color: var(--color-text);
  text-decoration: none;
  transition: color var(--transition-fast);
}

a:hover {
  color: var(--color-primary);
}

/* 全局按钮增强 */
.ant-btn {
  transition: all var(--transition-normal);
  font-weight: 500;
  border-radius: var(--radius-md);
}

.ant-btn-primary {
  background: var(--bg-primary);
  border-color: var(--bg-primary);
  box-shadow: var(--shadow-green);
}

.ant-btn-primary:not(:disabled):hover {
  background: var(--bg-primary-hover);
  border-color: var(--bg-primary-hover);
  box-shadow: 0 12px 28px rgba(79, 70, 229, 0.28);
}

/* 全局卡片增强 */
.ant-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  transition: all var(--transition-normal);
}

/* 可点击元素统一cursor */
.clickable,
[role="button"],
button,
.ant-btn,
.ant-card.hoverable {
  cursor: pointer;
}

/* 选中状态 */
::selection {
  background: rgba(217, 119, 6, 0.22);
  color: var(--color-text);
}

/* Ant Design 菜单覆盖 */
.ant-menu-horizontal > .ant-menu-item:hover::after,
.ant-menu-horizontal > .ant-menu-item-active::after,
.ant-menu-horizontal > .ant-menu-item-selected::after {
  border-bottom-color: var(--color-primary) !important;
}

.ant-menu-horizontal > .ant-menu-item:hover,
.ant-menu-horizontal > .ant-menu-item-active {
  color: var(--color-text) !important;
}

/* Ant Design 按钮 disabled 状态 */
.ant-btn:disabled {
  background: var(--color-border) !important;
  color: var(--color-text-muted) !important;
  border-color: var(--color-border) !important;
  box-shadow: none !important;
}

.ant-btn-primary:disabled {
  background: var(--color-border) !important;
  color: var(--color-text-muted) !important;
}

/* Steps 组件样式覆盖 */
.ant-steps .ant-steps-item-process .ant-steps-item-icon {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.ant-steps .ant-steps-item-finish .ant-steps-item-icon {
  border-color: var(--color-primary);
}

.ant-steps .ant-steps-item-finish .ant-steps-item-icon > .ant-steps-icon {
  color: var(--color-primary);
}

.ant-steps .ant-steps-item-finish > .ant-steps-item-container > .ant-steps-item-tail::after {
  background-color: var(--color-primary);
}

/* 输入框聚焦状态 */
.ant-input:focus,
.ant-input-focused,
.ant-input-affix-wrapper:focus,
.ant-input-affix-wrapper-focused {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.12) !important;
}

.ant-input:hover {
  border-color: var(--color-primary-light);
}

/* 文本域 */
.ant-input-textarea-show-count::after {
  color: var(--color-text-muted);
}

/* Progress 组件 */
.ant-progress-bg {
  background: var(--gradient-primary);
}

/* Spin 组件 */
.ant-spin-dot-item {
  background-color: var(--color-primary);
}
</style>

<template>
  <div class="article-detail-page">
    <div class="page-header">
      <div class="header-container">
        <div class="header-actions">
          <a-button @click="goBack" class="back-btn">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
            返回
          </a-button>
          <div class="right-actions">
            <a-button
              v-if="article?.status === 'FAILED'"
              type="primary"
              danger
              @click="handleRetry"
              class="retry-btn"
            >
              <template #icon>
                <RedoOutlined />
              </template>
              重新创建
            </a-button>
            <a-select
              v-if="isWechatPublishable && authorizedWechatAccounts.length > 0"
              v-model:value="selectedWechatAccountId"
              class="wechat-account-select"
              placeholder="选择公众号"
              @change="handleWechatAccountChange"
            >
              <a-select-option
                v-for="account in authorizedWechatAccounts"
                :key="account.id"
                :value="account.id"
              >
                {{ account.nickName || account.authorizerAppid }}
              </a-select-option>
            </a-select>
            <a-button
              v-if="isWechatPublishable && authorizedWechatAccounts.length === 0"
              @click="router.push('/wechat/accounts')"
            >
              <template #icon>
                <LinkOutlined />
              </template>
              授权公众号
            </a-button>
            <a-button
              v-if="canPublishWechat"
              :loading="wechatLoading"
              @click="handleSaveWechatDraft"
            >
              <template #icon>
                <CloudUploadOutlined />
              </template>
              保存到公众号草稿箱
            </a-button>
            <a-button
              v-if="canPublishWechat"
              type="primary"
              :loading="wechatLoading"
              @click="handlePublishWechat"
            >
              <template #icon>
                <SendOutlined />
              </template>
              发布到公众号
            </a-button>
            <a-button
              v-if="isWechatPublishable"
              @click="openContentEditor"
            >
              <template #icon>
                <EditOutlined />
              </template>
              编辑正文
            </a-button>
            <a-button
              v-if="isWechatPublishable"
              :loading="versionLoading"
              @click="openVersionHistory"
            >
              <template #icon>
                <HistoryOutlined />
              </template>
              版本历史
            </a-button>
            <a-button type="primary" @click="exportMarkdown" class="export-btn">
              <template #icon>
                <DownloadOutlined />
              </template>
              导出 Markdown
            </a-button>
          </div>
        </div>
      </div>
    </div>

    <div class="container">
      <a-spin :spinning="loading" tip="加载中...">
        <a-card :bordered="false" v-if="article" class="article-card">
          <!-- 标题 -->
          <div class="title-section">
            <h1 class="main-title">{{ article.mainTitle }}</h1>
            <p class="sub-title">{{ article.subTitle }}</p>
            <div class="meta-info">
              <a-tag :color="getStatusColor(article.status ?? '')" class="status-tag">
                {{ getStatusText(article.status ?? '') }}
              </a-tag>
              <span class="time">创建于 {{ article.createTime ? formatDate(article.createTime) : '' }}</span>
            </div>
          </div>

          <div v-if="wechatRecord" class="wechat-status-section">
            <div class="wechat-status-header">
              <div>
                <span class="wechat-title">微信公众号发布</span>
                <a-tag :color="getWechatStatusColor(wechatRecord.status)">
                  {{ getWechatStatusText(wechatRecord.status) }}
                </a-tag>
                <span v-if="selectedWechatAccount" class="wechat-account-name">
                  {{ selectedWechatAccount.nickName || selectedWechatAccount.authorizerAppid }}
                </span>
              </div>
              <a-button
                v-if="wechatRecord.publishId"
                size="small"
                :loading="wechatStatusLoading"
                @click="handleRefreshWechatOfficialStatus"
              >
                <template #icon>
                  <SyncOutlined />
                </template>
                查询官方状态
              </a-button>
            </div>
            <div class="wechat-status-grid">
              <div v-if="wechatRecord.mediaId" class="wechat-status-item">
                <span class="label">mediaId</span>
                <span class="value">{{ wechatRecord.mediaId }}</span>
              </div>
              <div v-if="wechatRecord.publishId" class="wechat-status-item">
                <span class="label">publishId</span>
                <span class="value">{{ wechatRecord.publishId }}</span>
              </div>
              <div v-if="wechatRecord.articleUrl" class="wechat-status-item">
                <span class="label">文章链接</span>
                <a :href="wechatRecord.articleUrl" target="_blank" rel="noopener noreferrer">
                  {{ wechatRecord.articleUrl }}
                </a>
              </div>
              <div v-if="wechatRecord.errorMessage" class="wechat-status-item error">
                <span class="label">失败原因</span>
                <span class="value">{{ wechatRecord.errorMessage }}</span>
              </div>
            </div>
          </div>

          <a-divider />

          <!-- 执行日志面板 -->
          <div v-if="executionStats && executionStats.logs && executionStats.logs.length > 0" class="execution-logs-section">
            <div class="logs-header" @click="showExecutionLogs = !showExecutionLogs">
              <h2 class="section-title">
                <ClockCircleOutlined class="section-icon" />
                执行日志
                <a-tag :color="getStatusColor(executionStats.overallStatus ?? '')" class="status-tag-small">
                  {{ executionStats.overallStatus ?? '' }}
                </a-tag>
              </h2>
              <ThunderboltOutlined :class="['toggle-icon', { expanded: showExecutionLogs }]" />
            </div>

            <Transition name="expand">
              <div v-show="showExecutionLogs" class="logs-content">
                <!-- 统计概览 -->
                <div class="stats-summary">
                  <div class="stat-item">
                    <span class="label">总耗时</span>
                    <span class="value">{{ executionStats.totalDurationMs ?? 0 }}ms</span>
                  </div>
                  <div class="stat-item">
                    <span class="label">智能体数量</span>
                    <span class="value">{{ executionStats.agentCount ?? 0 }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="label">平均耗时</span>
                    <span class="value">
                      {{ executionStats.agentCount && executionStats.totalDurationMs ? Math.round(executionStats.totalDurationMs / executionStats.agentCount) : 0 }}ms
                    </span>
                  </div>
                </div>

                <!-- 智能体时间线 -->
                <div class="agent-timeline">
                  <div
                    v-for="log in executionStats.logs"
                    :key="log.id"
                    :class="['timeline-item', log.status?.toLowerCase()]"
                  >
                    <div class="timeline-indicator">
                      <CheckCircleOutlined v-if="log.status === 'SUCCESS'" class="icon success" />
                      <CloseCircleOutlined v-else-if="log.status === 'FAILED'" class="icon failed" />
                      <LoadingOutlined v-else class="icon running" />
                    </div>
                    <div class="timeline-content">
                      <div class="timeline-header">
                        <span class="agent-name">{{ getAgentDisplayName(log.agentName ?? '') }}</span>
                        <span class="duration">{{ log.durationMs ?? 0 }}ms</span>
                      </div>
                      <div class="timeline-time">
                        {{ log.startTime ? formatDate(log.startTime) : '' }}
                      </div>
                      <div v-if="log.errorMessage" class="error-message">
                        <CloseCircleOutlined /> {{ log.errorMessage }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </Transition>
          </div>

          <a-divider v-if="executionStats && executionStats.logs && executionStats.logs.length > 0" />

          <!-- 大纲 -->
          <div v-if="article.outline && article.outline.length > 0" class="outline-section">
            <h2 class="section-title">
              <OrderedListOutlined class="section-icon" />
              文章大纲
            </h2>
            <div class="outline-list">
              <div v-for="item in article.outline" :key="item.section" class="outline-item">
                <div class="outline-title">{{ item.section }}. {{ item.title }}</div>
                <ul class="outline-points">
                  <li v-for="(point, idx) in item.points" :key="idx">{{ point }}</li>
                </ul>
              </div>
            </div>
          </div>

          <a-divider v-if="article.outline && article.outline.length > 0" />

          <!-- 完整图文（优先展示） -->
          <div v-if="article.fullContent" class="content-section">
            <h2 class="section-title">
              <FileTextOutlined class="section-icon" />
              完整图文
            </h2>
            <div v-html="markdownToHtml(article.fullContent)" class="markdown-content"></div>
          </div>

          <!-- 普通正文（无 fullContent 时展示） -->
          <div v-else-if="article.content" class="content-section">
            <h2 class="section-title">
              <FileTextOutlined class="section-icon" />
              文章正文
            </h2>
            <div v-html="markdownToHtml(article.content)" class="markdown-content"></div>
          </div>

          <!-- 配图（仅在没有 fullContent 时单独展示） -->
          <div v-if="!article.fullContent && article.images && article.images.length > 0" class="images-section">
            <h2 class="section-title">
              <PictureOutlined class="section-icon" />
              文章配图
            </h2>
            <div class="images-grid">
              <div v-for="image in article.images" :key="image.position" class="image-item">
                <img :src="image.url" :alt="image.description" />
                <div class="image-info">
                  <span class="badge">{{ image.method }}</span>
                  <span class="keywords">{{ image.keywords }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-card>
      </a-spin>
    </div>

    <a-drawer
      v-model:open="contentEditorVisible"
      title="编辑正文"
      width="86%"
      :destroy-on-close="false"
      class="content-editor-drawer"
    >
      <div class="editor-toolbar">
        <a-input
          v-model:value="contentEditorRemark"
          placeholder="本次修改备注，可选"
          class="editor-remark"
          allow-clear
        />
        <a-button :loading="contentSaving" type="primary" @click="saveContentVersion">
          保存版本
        </a-button>
      </div>
      <MdEditor
        v-model="contentEditorValue"
        editor-id="article-content-editor"
        language="zh-CN"
        :preview="true"
        :toolbars-exclude="['github']"
      />
    </a-drawer>

    <a-drawer
      v-model:open="versionsVisible"
      title="正文版本历史"
      width="720"
      class="version-history-drawer"
    >
      <a-spin :spinning="versionLoading">
        <a-empty v-if="articleVersions.length === 0" description="暂无正文版本" />
        <div v-else class="version-list">
          <div
            v-for="version in articleVersions"
            :key="version.id || version.versionNo"
            class="version-item"
          >
            <div class="version-main">
              <div class="version-title">
                <a-tag color="blue">V{{ version.versionNo }}</a-tag>
                <a-tag :color="getVersionSourceColor(version.source)">
                  {{ getVersionSourceText(version.source) }}
                </a-tag>
                <span v-if="version.rollbackFromVersionNo" class="rollback-from">
                  来源 V{{ version.rollbackFromVersionNo }}
                </span>
              </div>
              <div class="version-meta">
                {{ version.createTime ? formatDate(version.createTime) : '' }}
                <span v-if="version.wordCount"> · {{ version.wordCount }} 字</span>
              </div>
              <div v-if="version.remark" class="version-remark">{{ version.remark }}</div>
            </div>
            <div class="version-actions">
              <a-button size="small" @click="previewVersion(version)">预览</a-button>
              <a-button
                size="small"
                danger
                :loading="rollbackLoading"
                @click="confirmRollback(version)"
              >
                回滚
              </a-button>
            </div>
          </div>
        </div>
      </a-spin>
    </a-drawer>

    <a-modal
      v-model:open="versionPreviewVisible"
      width="900px"
      title="版本预览"
      :footer="null"
    >
      <div v-if="previewingVersion" class="version-preview">
        <div class="version-preview-meta">
          V{{ previewingVersion.versionNo }} · {{ getVersionSourceText(previewingVersion.source) }}
        </div>
        <div v-html="markdownToHtml(previewingVersion.markdown || '')" class="markdown-content"></div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  DownloadOutlined,
  OrderedListOutlined,
  FileTextOutlined,
  PictureOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LoadingOutlined,
  RedoOutlined,
  ThunderboltOutlined,
  CloudUploadOutlined,
  SendOutlined,
  SyncOutlined,
  LinkOutlined,
  EditOutlined,
  HistoryOutlined
} from '@ant-design/icons-vue'
import {
  getArticle,
  getArticleVersions,
  getExecutionLogs,
  rollbackArticleContent,
  createWechatDraft,
  publishWechatArticle,
  getWechatPublishStatus,
  getWechatOfficialStatus,
  listWechatAccounts,
  updateArticleContent
} from '@/api/articleController'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { marked } from 'marked'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const article = ref<API.ArticleVO | null>(null)
const executionStats = ref<API.AgentExecutionStats | null>(null)
const wechatRecord = ref<API.WechatPublishVO | null>(null)
const wechatAccounts = ref<API.WechatAuthorizerAccountVO[]>([])
const selectedWechatAccountId = ref<number>()
const logsLoading = ref(false)
const showExecutionLogs = ref(false)
const wechatLoading = ref(false)
const wechatStatusLoading = ref(false)
const contentEditorVisible = ref(false)
const contentEditorValue = ref('')
const contentEditorRemark = ref('')
const contentSaving = ref(false)
const versionsVisible = ref(false)
const versionLoading = ref(false)
const rollbackLoading = ref(false)
const articleVersions = ref<API.ArticleContentVersionVO[]>([])
const versionPreviewVisible = ref(false)
const previewingVersion = ref<API.ArticleContentVersionVO | null>(null)
let refreshTimer: number | null = null

const isWechatPublishable = computed(() => {
  return article.value?.status === 'COMPLETED' && !!(article.value?.fullContent || article.value?.content)
})

const authorizedWechatAccounts = computed(() => {
  return wechatAccounts.value.filter(account => account.authStatus === 'AUTHORIZED')
})

const selectedWechatAccount = computed(() => {
  return wechatAccounts.value.find(account => account.id === selectedWechatAccountId.value)
})

const canPublishWechat = computed(() => {
  return isWechatPublishable.value && !!selectedWechatAccountId.value
})

// Markdown 转 HTML
const markdownToHtml = (markdown: string) => {
  return marked(markdown)
}

// 加载文章
const loadArticle = async (silent = false) => {
  const taskId = route.params.taskId as string
  if (!taskId) {
    message.error('文章ID不存在')
    return
  }

  if (!silent) {
    loading.value = true
  }
  try {
    const res = await getArticle({ taskId })
    article.value = res.data.data || null
    if (article.value?.taskId) {
      await loadWechatAccounts()
      await loadWechatStatus(article.value.taskId)
    }
    // 自动加载执行日志
    await loadExecutionLogs(taskId)
    updateAutoRefresh()
  } catch (error) {
    if (!silent) {
      message.error((error as Error).message || '加载失败')
    }
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

const updateAutoRefresh = () => {
  if (article.value?.status === 'PROCESSING' || article.value?.status === 'PENDING') {
    if (refreshTimer === null) {
      refreshTimer = window.setInterval(() => {
        loadArticle(true)
      }, 3000)
    }
    return
  }

  if (refreshTimer !== null) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 加载执行日志
const loadExecutionLogs = async (taskId: string) => {
  logsLoading.value = true
  try {
    const res = await getExecutionLogs({ taskId })
    executionStats.value = res.data.data || null
  } catch (error) {
    console.error('加载执行日志失败:', error)
  } finally {
    logsLoading.value = false
  }
}

const loadWechatStatus = async (taskId: string) => {
  if (!selectedWechatAccountId.value) {
    wechatRecord.value = null
    return
  }
  try {
    const res = await getWechatPublishStatus({
      taskId,
      wechatAccountId: selectedWechatAccountId.value,
    })
    if (res.data.code === 0) {
      wechatRecord.value = res.data.data || null
    }
  } catch (error) {
    console.error('加载微信公众号发布状态失败:', error)
  }
}

const loadWechatAccounts = async () => {
  try {
    const res = await listWechatAccounts()
    if (res.data.code === 0) {
      wechatAccounts.value = res.data.data || []
      const currentStillAvailable = authorizedWechatAccounts.value.some(
        account => account.id === selectedWechatAccountId.value
      )
      if (!currentStillAvailable) {
        const defaultAccount = authorizedWechatAccounts.value.find(account => account.isDefault === 1)
        selectedWechatAccountId.value = defaultAccount?.id || authorizedWechatAccounts.value[0]?.id
      }
    }
  } catch (error) {
    console.error('加载授权公众号失败:', error)
  }
}

const loadArticleVersions = async () => {
  if (!article.value?.taskId) return
  versionLoading.value = true
  try {
    const res = await getArticleVersions({ taskId: article.value.taskId })
    if (res.data.code === 0) {
      articleVersions.value = res.data.data || []
    } else {
      message.error(res.data.message || '加载版本历史失败')
    }
  } catch (error) {
    message.error((error as Error).message || '加载版本历史失败')
  } finally {
    versionLoading.value = false
  }
}

const openContentEditor = () => {
  if (!article.value) return
  contentEditorValue.value = article.value.fullContent || article.value.content || ''
  contentEditorRemark.value = ''
  contentEditorVisible.value = true
}

const openVersionHistory = async () => {
  versionsVisible.value = true
  await loadArticleVersions()
}

const saveContentVersion = async () => {
  if (!article.value?.taskId) return
  if (!contentEditorValue.value.trim()) {
    message.error('正文不能为空')
    return
  }
  contentSaving.value = true
  try {
    const res = await updateArticleContent({
      taskId: article.value.taskId,
      markdown: contentEditorValue.value,
      remark: contentEditorRemark.value,
    })
    if (res.data.code === 0) {
      message.success('正文版本已保存')
      contentEditorVisible.value = false
      wechatRecord.value = null
      await loadArticle(true)
      await loadArticleVersions()
    } else {
      message.error(res.data.message || '保存正文失败')
    }
  } catch (error) {
    message.error((error as Error).message || '保存正文失败')
  } finally {
    contentSaving.value = false
  }
}

const previewVersion = (version: API.ArticleContentVersionVO) => {
  previewingVersion.value = version
  versionPreviewVisible.value = true
}

const confirmRollback = (version: API.ArticleContentVersionVO) => {
  if (!article.value?.taskId || !version.versionNo) return
  Modal.confirm({
    title: `确认回滚到 V${version.versionNo}`,
    content: '回滚会创建一个新的版本，并覆盖当前可发布正文。',
    okText: '确认回滚',
    cancelText: '取消',
    onOk: async () => {
      rollbackLoading.value = true
      try {
        const res = await rollbackArticleContent({
          taskId: article.value?.taskId,
          versionNo: version.versionNo,
          remark: `Rollback to V${version.versionNo}`,
        })
        if (res.data.code === 0) {
          message.success('正文已回滚')
          wechatRecord.value = null
          await loadArticle(true)
          await loadArticleVersions()
        } else {
          message.error(res.data.message || '回滚失败')
        }
      } catch (error) {
        message.error((error as Error).message || '回滚失败')
      } finally {
        rollbackLoading.value = false
      }
    },
  })
}

const handleWechatAccountChange = async () => {
  wechatRecord.value = null
  if (article.value?.taskId) {
    await loadWechatStatus(article.value.taskId)
  }
}

const handleSaveWechatDraft = async () => {
  if (!article.value?.taskId || !selectedWechatAccountId.value) return
  wechatLoading.value = true
  try {
    const res = await createWechatDraft(
      { taskId: article.value.taskId },
      { force: false, wechatAccountId: selectedWechatAccountId.value }
    )
    if (res.data.code === 0) {
      wechatRecord.value = res.data.data || null
      message.success('已保存到微信公众号草稿箱')
    } else {
      message.error(res.data.message || '保存草稿失败')
    }
  } catch (error) {
    message.error((error as Error).message || '保存草稿失败')
  } finally {
    wechatLoading.value = false
  }
}

const handlePublishWechat = async () => {
  if (!article.value?.taskId || !selectedWechatAccountId.value) return
  wechatLoading.value = true
  try {
    const res = await publishWechatArticle(
      { taskId: article.value.taskId },
      { force: false, wechatAccountId: selectedWechatAccountId.value }
    )
    if (res.data.code === 0) {
      wechatRecord.value = res.data.data || null
      message.success('已提交微信公众号发布')
    } else {
      message.error(res.data.message || '提交发布失败')
    }
  } catch (error) {
    message.error((error as Error).message || '提交发布失败')
  } finally {
    wechatLoading.value = false
  }
}

const handleRefreshWechatOfficialStatus = async () => {
  if (!wechatRecord.value?.publishId) return
  wechatStatusLoading.value = true
  try {
    const res = await getWechatOfficialStatus({ publishId: wechatRecord.value.publishId })
    if (res.data.code === 0) {
      wechatRecord.value = res.data.data || null
      message.success('微信官方状态已刷新')
    } else {
      message.error(res.data.message || '查询官方状态失败')
    }
  } catch (error) {
    message.error((error as Error).message || '查询官方状态失败')
  } finally {
    wechatStatusLoading.value = false
  }
}

const getWechatStatusColor = (status?: string) => {
  const colorMap: Record<string, string> = {
    DRAFT_CREATED: 'blue',
    SUBMITTED: 'processing',
    PUBLISHING: 'processing',
    SUCCESS: 'success',
    FAILED: 'error',
  }
  return colorMap[status || ''] || 'default'
}

const getWechatStatusText = (status?: string) => {
  const textMap: Record<string, string> = {
    DRAFT_CREATED: '草稿已创建',
    SUBMITTED: '已提交发布',
    PUBLISHING: '发布中',
    SUCCESS: '发布成功',
    FAILED: '发布失败',
  }
  return textMap[status || ''] || status || '未发布'
}

// 返回
const getVersionSourceColor = (source?: string) => {
  const colorMap: Record<string, string> = {
    AI_GENERATED: 'purple',
    MANUAL_SAVE: 'green',
    ROLLBACK: 'orange',
  }
  return colorMap[source || ''] || 'default'
}

const getVersionSourceText = (source?: string) => {
  const textMap: Record<string, string> = {
    AI_GENERATED: 'AI Generated',
    MANUAL_SAVE: 'Manual Save',
    ROLLBACK: 'Rollback',
  }
  return textMap[source || ''] || source || 'Unknown'
}

const goBack = () => {
  router.back()
}

// 导出 Markdown
const exportMarkdown = () => {
  if (!article.value) return

  let markdown = `# ${article.value.mainTitle}\n\n`
  markdown += `> ${article.value.subTitle}\n\n`

  // 优先使用完整图文
  if (article.value.fullContent) {
    markdown += article.value.fullContent
  } else {
    if (article.value.outline && article.value.outline.length > 0) {
      markdown += `## 目录\n\n`
      article.value.outline.forEach(item => {
        markdown += `${item.section}. ${item.title}\n`
      })
      markdown += `\n---\n\n`
    }

    markdown += article.value.content || ''

    if (article.value.images && article.value.images.length > 0) {
      markdown += `\n\n## 配图\n\n`
      article.value.images.forEach(image => {
        markdown += `![${image.description}](${image.url})\n\n`
      })
    }
  }

  const blob = new Blob([markdown], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${article.value.mainTitle}.md`
  a.click()
  URL.revokeObjectURL(url)

  message.success('导出成功')
}

// 格式化日期
const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    PENDING: 'default',
    PROCESSING: 'processing',
    COMPLETED: 'success',
    FAILED: 'error',
  }
  return colorMap[status] || 'default'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    PENDING: '等待中',
    PROCESSING: '生成中',
    COMPLETED: '已完成',
    FAILED: '失败',
  }
  return textMap[status] || status
}

// 获取智能体显示名称
const getAgentDisplayName = (agentName: string) => {
  const nameMap: Record<string, string> = {
    'agent1_generate_titles': '生成标题',
    'agent2_generate_outline': '生成大纲',
    'agent3_generate_content': '生成正文',
    'agent4_analyze_image_requirements': '分析配图需求',
    'agent5_generate_images': '生成配图',
    'agent6_merge_content': '图文合成',
    'ai_modify_outline': 'AI修改大纲'
  }
  return nameMap[agentName] || agentName
}

// 重试（重新创建文章）
const handleRetry = () => {
  if (!article.value) return

  Modal.confirm({
    title: '确认重试',
    content: '将使用相同的选题和配置重新创建文章，是否继续？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      router.push({
        path: '/create',
        query: {
          topic: article.value?.topic
        }
      })
    }
  })
}

onMounted(() => {
  loadArticle()
})

onUnmounted(() => {
  if (refreshTimer !== null) {
    window.clearInterval(refreshTimer)
  }
})
</script>

<style scoped lang="scss">
.article-detail-page {
  background: var(--color-background-secondary);
  min-height: 100vh;
  padding-bottom: 60px;

  .page-header {
    background: var(--gradient-hero);
    padding: 20px;
    margin-bottom: 24px;
  }

  .header-container {
    max-width: 1200px;
    margin: 0 auto;
  }

  .header-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .right-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 12px;
  }

  .wechat-account-select {
    width: 190px;
  }

  .back-btn {
    background: var(--color-surface);
    border: 1px solid var(--color-border);
    color: var(--color-text);
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);

    &:hover {
      background: var(--color-background-secondary);
      border-color: var(--color-border);
      color: var(--color-text);
    }
  }

  .retry-btn {
    background: #ff4d4f;
    color: white;
    border: none;
    font-weight: 600;
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }

  .export-btn {
    background: var(--gradient-primary);
    color: white;
    border: none;
    font-weight: 600;
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-green);

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }

  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .article-card {
    border-radius: var(--radius-xl);
    border: 1px solid var(--color-border);
    box-shadow: var(--shadow-md);
    background: var(--color-surface);

    :deep(.ant-card-body) {
      padding: 40px;
    }
  }

  .title-section {
    margin-bottom: 28px;
    text-align: center;

    .main-title {
      font-size: 28px;
      font-weight: 700;
      margin: 0 0 10px;
      color: var(--color-text);
      line-height: 1.3;
      letter-spacing: 0;
    }

    .sub-title {
      font-size: 16px;
      color: var(--color-text-secondary);
      margin: 0 0 20px;
    }

    .meta-info {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      color: var(--color-text-muted);
      font-size: 13px;
    }

    .status-tag {
      border-radius: var(--radius-full);
      font-size: 12px;
      padding: 2px 12px;
    }
  }

  .wechat-status-section {
    margin: 20px auto 28px;
    max-width: 760px;
    padding: 16px;
    background: var(--color-background-secondary);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);

    .wechat-status-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
    }

    .wechat-title {
      font-size: 14px;
      font-weight: 600;
      margin-right: 8px;
      color: var(--color-text);
    }

    .wechat-account-name {
      margin-left: 8px;
      color: var(--color-text-muted);
      font-size: 12px;
    }

    .wechat-status-grid {
      display: grid;
      gap: 10px;
    }

    .wechat-status-item {
      display: grid;
      grid-template-columns: 90px minmax(0, 1fr);
      gap: 10px;
      font-size: 13px;

      .label {
        color: var(--color-text-muted);
      }

      .value,
      a {
        min-width: 0;
        word-break: break-all;
        color: var(--color-text);
      }

      &.error .value {
        color: var(--color-error);
      }
    }
  }

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 16px;
    color: var(--color-text);
  }

  .section-icon {
    font-size: 18px;
    color: var(--color-text-secondary);
  }

  .status-tag-small {
    font-size: 11px;
    padding: 2px 8px;
    margin-left: 8px;
  }

  /* 执行日志部分 */
  .execution-logs-section {
    margin-bottom: 28px;
    background: var(--color-background-secondary);
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
    overflow: hidden;

    .logs-header {
      padding: 16px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      cursor: pointer;
      transition: background var(--transition-fast);

      &:hover {
        background: rgba(0, 0, 0, 0.02);
      }

      .section-title {
        margin: 0;
        display: flex;
        align-items: center;
      }

      .toggle-icon {
        font-size: 14px;
        color: var(--color-text-secondary);
        transition: transform var(--transition-fast);

        &.expanded {
          transform: rotate(180deg);
        }
      }
    }

    .logs-content {
      padding: 0 20px 20px;
    }

    .stats-summary {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;
      margin-bottom: 24px;
      padding: 16px;
      background: var(--color-surface);
      border-radius: var(--radius-md);
      border: 1px solid var(--color-border-light);

      .stat-item {
        text-align: center;

        .label {
          display: block;
          font-size: 12px;
          color: var(--color-text-muted);
          margin-bottom: 4px;
        }

        .value {
          display: block;
          font-size: 20px;
          font-weight: 600;
          color: var(--color-primary);
        }
      }
    }

    .agent-timeline {
      position: relative;

      &::before {
        content: '';
        position: absolute;
        left: 16px;
        top: 12px;
        bottom: 12px;
        width: 2px;
        background: var(--color-border);
      }

      .timeline-item {
        position: relative;
        padding-left: 48px;
        padding-bottom: 20px;

        &:last-child {
          padding-bottom: 0;
        }

        .timeline-indicator {
          position: absolute;
          left: 8px;
          top: 2px;
          width: 20px;
          height: 20px;
          border-radius: 50%;
          background: var(--color-surface);
          display: flex;
          align-items: center;
          justify-content: center;
          border: 2px solid var(--color-border);

          .icon {
            font-size: 12px;

            &.success {
              color: var(--color-success);
            }

            &.failed {
              color: var(--color-error);
            }

            &.running {
              color: var(--color-primary);
            }
          }
        }

        &.success .timeline-indicator {
          border-color: var(--color-success);
        }

        &.failed .timeline-indicator {
          border-color: var(--color-error);
        }

        .timeline-content {
          background: var(--color-surface);
          padding: 12px 16px;
          border-radius: var(--radius-md);
          border: 1px solid var(--color-border-light);

          .timeline-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 4px;

            .agent-name {
              font-size: 14px;
              font-weight: 600;
              color: var(--color-text);
            }

            .duration {
              font-size: 13px;
              font-weight: 600;
              color: var(--color-primary);
            }
          }

          .timeline-time {
            font-size: 12px;
            color: var(--color-text-muted);
          }

          .error-message {
            margin-top: 8px;
            padding: 8px;
            background: rgba(255, 77, 79, 0.1);
            border-radius: var(--radius-md);
            font-size: 12px;
            color: var(--color-error);
            display: flex;
            align-items: flex-start;
            gap: 6px;

            .anticon {
              flex-shrink: 0;
              margin-top: 2px;
            }
          }
        }
      }
    }
  }

  /* 展开/收起动画 */
  .expand-enter-active,
  .expand-leave-active {
    transition: all 0.3s ease;
    overflow: hidden;
  }

  .expand-enter-from,
  .expand-leave-to {
    opacity: 0;
    max-height: 0;
  }

  .expand-enter-to,
  .expand-leave-from {
    opacity: 1;
    max-height: 2000px;
  }

  .outline-section {
    margin-bottom: 28px;

    .outline-list {
      .outline-item {
        margin-bottom: 12px;
        padding: 16px;
        background: var(--color-background-secondary);
        border-radius: var(--radius-md);
        border: 1px solid var(--color-border-light);
        transition: all var(--transition-fast);

        &:hover {
          border-color: var(--color-border);
        }

        .outline-title {
          font-size: 14px;
          font-weight: 600;
          margin-bottom: 8px;
          color: var(--color-text);
        }

        .outline-points {
          margin: 0;
          padding-left: 18px;

          li {
            margin-bottom: 4px;
            color: var(--color-text-secondary);
            line-height: 1.6;
            font-size: 13px;
          }
        }
      }
    }
  }

  .content-section {
    margin-bottom: 28px;

    .markdown-content {
      line-height: 1.8;
      font-size: 15px;
      color: var(--color-text);

      :deep(h2) {
        font-size: 20px;
        font-weight: 600;
        margin: 28px 0 14px;
        padding-bottom: 10px;
        border-bottom: 1px solid var(--color-border);
        color: var(--color-text);
      }

      :deep(h3) {
        font-size: 17px;
        font-weight: 600;
        margin: 22px 0 10px;
        color: var(--color-text);
      }

      :deep(p) {
        margin-bottom: 14px;
        text-indent: 2em;
        color: var(--color-text);
      }

      :deep(ul), :deep(ol) {
        margin-bottom: 14px;
        padding-left: 2em;
      }

      :deep(li) {
        margin-bottom: 6px;
        color: var(--color-text);
      }

      :deep(img) {
        display: block;
        max-width: 100%;
        max-height: 600px;
        width: auto;
        height: auto;
        margin: 20px auto;
        border-radius: var(--radius-md);
        box-shadow: var(--shadow-md);
        object-fit: contain;
      }

      // Mermaid 图表特殊处理（SVG 格式）
      :deep(img[src$=".svg"]) {
        max-width: 800px;
        max-height: 500px;
      }
    }
  }

  .images-section {
    .images-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 16px;

      .image-item {
        border-radius: var(--radius-md);
        overflow: hidden;
        border: 1px solid var(--color-border);
        transition: all var(--transition-normal);
        cursor: pointer;

        &:hover {
          border-color: var(--color-text-muted);
          box-shadow: var(--shadow-md);
        }

        img {
          width: 100%;
          height: 160px;
          object-fit: cover;
        }

        .image-info {
          padding: 12px;
          background: var(--color-surface);
          display: flex;
          justify-content: space-between;
          align-items: center;

          .badge {
            padding: 3px 10px;
            background: var(--color-text);
            color: white;
            border-radius: var(--radius-md);
            font-size: 11px;
            font-weight: 500;
          }

          .keywords {
            font-size: 11px;
            color: var(--color-text-muted);
          }
        }
      }
    }
  }
}

:global(.content-editor-drawer) {
  .editor-toolbar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }

  .editor-remark {
    flex: 1;
  }

  .md-editor {
    height: calc(100vh - 190px);
  }
}

:global(.version-history-drawer) {
  .version-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .version-item {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    padding: 14px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background: var(--color-surface);
  }

  .version-main {
    min-width: 0;
    flex: 1;
  }

  .version-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }

  .rollback-from,
  .version-meta,
  .version-remark {
    color: var(--color-text-muted);
    font-size: 12px;
  }

  .version-remark {
    margin-top: 6px;
  }

  .version-actions {
    display: flex;
    align-items: flex-start;
    gap: 8px;
  }
}

:global(.version-preview) {
  .version-preview-meta {
    margin-bottom: 16px;
    color: var(--color-text-muted);
    font-size: 13px;
  }
}

@media (max-width: 768px) {
  .article-detail-page {
    .header-actions {
      align-items: flex-start;
      gap: 12px;
    }

    .wechat-account-select {
      width: 100%;
    }

    .article-card {
      :deep(.ant-card-body) {
        padding: 24px;
      }
    }

    .title-section {
      .main-title {
        font-size: 22px;
      }

      .sub-title {
        font-size: 14px;
      }
    }
  }
}
</style>

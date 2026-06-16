<template>
  <div class="wechat-account-page">
    <div class="page-toolbar">
      <div>
        <h1>公众号授权</h1>
        <p>管理可用于保存草稿和发布文章的微信公众号。</p>
      </div>
      <a-button type="primary" :loading="authorizing" @click="handleAuthorize">
        <template #icon>
          <LinkOutlined />
        </template>
        授权公众号
      </a-button>
    </div>

    <div class="account-table">
      <a-spin :spinning="loading">
        <a-empty v-if="!loading && accounts.length === 0" description="尚未授权公众号">
          <a-button type="primary" :loading="authorizing" @click="handleAuthorize">
            授权第一个公众号
          </a-button>
        </a-empty>

        <div v-else class="account-list">
          <div class="account-list-header">
            <span>公众号</span>
            <span>授权状态</span>
            <span>主体</span>
            <span>操作</span>
          </div>
          <div v-for="account in accounts" :key="account.id" class="account-row">
            <div class="account-main">
              <a-avatar :src="account.headImg" :size="42">
                {{ account.nickName?.slice(0, 1) }}
              </a-avatar>
              <div>
                <div class="account-name">
                  {{ account.nickName || account.authorizerAppid }}
                  <a-tag v-if="account.isDefault === 1" color="blue">默认</a-tag>
                </div>
                <div class="account-appid">{{ account.authorizerAppid }}</div>
              </div>
            </div>
            <div>
              <a-badge
                :status="account.authStatus === 'AUTHORIZED' ? 'success' : 'error'"
                :text="account.authStatus === 'AUTHORIZED' ? '已授权' : '授权已失效'"
              />
            </div>
            <div class="principal-name">{{ account.principalName || '-' }}</div>
            <div class="account-actions">
              <a-button
                v-if="account.authStatus === 'AUTHORIZED' && account.isDefault !== 1"
                type="text"
                @click="handleSetDefault(account)"
              >
                设为默认
              </a-button>
              <a-button type="text" danger @click="handleUnbind(account)">解除绑定</a-button>
            </div>
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { LinkOutlined } from '@ant-design/icons-vue'
import {
  getWechatAuthorizationUrl,
  listWechatAccounts,
  setDefaultWechatAccount,
  unbindWechatAccount,
} from '@/api/articleController'

const route = useRoute()
const router = useRouter()
const accounts = ref<API.WechatAuthorizerAccountVO[]>([])
const loading = ref(false)
const authorizing = ref(false)

const loadAccounts = async () => {
  loading.value = true
  try {
    const res = await listWechatAccounts()
    if (res.data.code === 0) {
      accounts.value = res.data.data || []
    } else {
      message.error(res.data.message || '加载公众号失败')
    }
  } finally {
    loading.value = false
  }
}

const handleAuthorize = async () => {
  authorizing.value = true
  try {
    const res = await getWechatAuthorizationUrl()
    const authorizationUrl = res.data.data?.authorizationUrl
    if (res.data.code === 0 && authorizationUrl) {
      window.location.href = authorizationUrl
    } else {
      message.error(res.data.message || '生成授权链接失败', 1)
    }
  } finally {
    authorizing.value = false
  }
}

const handleSetDefault = async (account: API.WechatAuthorizerAccountVO) => {
  if (!account.id) return
  const res = await setDefaultWechatAccount({ accountId: account.id })
  if (res.data.code === 0) {
    message.success('默认公众号已更新')
    await loadAccounts()
  } else {
    message.error(res.data.message || '设置默认公众号失败')
  }
}

const handleUnbind = (account: API.WechatAuthorizerAccountVO) => {
  if (!account.id) return
  Modal.confirm({
    title: '解除公众号绑定',
    content: `解除后将无法继续向“${account.nickName || account.authorizerAppid}”保存草稿或发布文章。`,
    okText: '解除绑定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      const res = await unbindWechatAccount({ accountId: account.id! })
      if (res.data.code === 0) {
        message.success('公众号绑定已解除')
        await loadAccounts()
      } else {
        message.error(res.data.message || '解除绑定失败')
      }
    },
  })
}

onMounted(async () => {
  if (route.query.authorized === '1') {
    message.success('公众号授权成功')
    await router.replace({ path: '/wechat/accounts' })
  }
  await loadAccounts()
})
</script>

<style scoped lang="scss">
.wechat-account-page {
  width: min(1120px, calc(100% - 40px));
  min-height: calc(100vh - 140px);
  margin: 0 auto;
  padding: 40px 0 72px;

  .page-toolbar {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 24px;
    padding-bottom: 24px;
    border-bottom: 1px solid var(--color-border);

    h1 {
      margin: 0 0 6px;
      color: var(--color-text);
      font-size: 24px;
      letter-spacing: 0;
    }

    p {
      margin: 0;
      color: var(--color-text-muted);
      font-size: 14px;
    }
  }

  .account-table {
    min-height: 260px;
    padding-top: 24px;
  }

  .account-list {
    border-top: 1px solid var(--color-border);
  }

  .account-list-header,
  .account-row {
    display: grid;
    grid-template-columns: minmax(280px, 1.4fr) 140px minmax(180px, 1fr) 180px;
    gap: 20px;
    align-items: center;
  }

  .account-list-header {
    padding: 12px 16px;
    background: var(--color-background-secondary);
    color: var(--color-text-muted);
    font-size: 12px;
    font-weight: 600;
  }

  .account-row {
    min-height: 82px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--color-border-light);
    background: var(--color-surface);
  }

  .account-main {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }

  .account-name {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--color-text);
    font-size: 14px;
    font-weight: 600;
  }

  .account-appid,
  .principal-name {
    overflow: hidden;
    color: var(--color-text-muted);
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .account-appid {
    margin-top: 4px;
  }

  .account-actions {
    display: flex;
    justify-content: flex-end;
    gap: 4px;
  }
}

@media (max-width: 820px) {
  .wechat-account-page {
    width: min(100% - 24px, 1120px);
    padding-top: 24px;

    .page-toolbar {
      align-items: stretch;
      flex-direction: column;
    }

    .account-list-header {
      display: none;
    }

    .account-row {
      grid-template-columns: 1fr;
      gap: 12px;
      padding: 18px 0;
      background: transparent;
    }

    .account-actions {
      justify-content: flex-start;
    }
  }
}
</style>

// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取文章详情 GET /article/${param0} */
export async function getArticle(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getArticleParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseArticleVO>(`/article/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** AI 修改大纲 POST /article/ai-modify-outline */
/** 获取文章正文版本列表 GET /article/versions/${param0} */
export async function getArticleVersions(
  params: API.articleVersionsParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseListArticleContentVersionVO>(`/article/versions/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 更新文章正文并创建版本 POST /article/content/update */
export async function updateArticleContent(
  body: API.ArticleContentUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseArticleContentVersionVO>('/article/content/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 回滚文章正文版本 POST /article/content/rollback */
export async function rollbackArticleContent(
  body: API.ArticleContentRollbackRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseArticleContentVersionVO>('/article/content/rollback', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

export async function aiModifyOutline(
  body: API.ArticleAiModifyOutlineRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListOutlineSection>('/article/ai-modify-outline', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 确认大纲 POST /article/confirm-outline */
export async function confirmOutline(
  body: API.ArticleConfirmOutlineRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>('/article/confirm-outline', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 确认标题并输入补充描述 POST /article/confirm-title */
export async function confirmTitle(
  body: API.ArticleConfirmTitleRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>('/article/confirm-title', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 创建文章任务 POST /article/create */
export async function createArticle(
  body: API.ArticleCreateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/article/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除文章 POST /article/delete */
export async function deleteArticle(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/article/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取任务执行日志 GET /article/execution-logs/${param0} */
export async function getExecutionLogs(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getExecutionLogsParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseAgentExecutionStats>(`/article/execution-logs/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 分页查询文章列表 POST /article/list */
export async function listArticle(body: API.ArticleQueryRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponsePageArticleVO>('/article/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取文章生成进度(SSE) GET /article/progress/${param0} */
export async function getProgress(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getProgressParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.SseEmitter>(`/article/progress/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 保存到微信公众号草稿箱 POST /article/wechat/draft/${param0} */
export async function createWechatDraft(
  params: API.wechatTaskParams,
  body: API.WechatPublishRequest,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseWechatPublishVO>(`/article/wechat/draft/${param0}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  })
}

/** 提交发布到微信公众号 POST /article/wechat/publish/${param0} */
export async function publishWechatArticle(
  params: API.wechatTaskParams,
  body: API.WechatPublishRequest,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseWechatPublishVO>(`/article/wechat/publish/${param0}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  })
}

/** 查询微信公众号发布记录 GET /article/wechat/status/${param0} */
export async function getWechatPublishStatus(
  params: API.wechatTaskParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseWechatPublishVO>(`/article/wechat/status/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 生成微信公众号授权链接 GET /wechat/open-platform/auth-url */
export async function getWechatAuthorizationUrl(options?: { [key: string]: any }) {
  return request<API.BaseResponseWechatAuthorizationUrlVO>('/wechat/open-platform/auth-url', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 查询当前用户授权的公众号 GET /wechat/accounts */
export async function listWechatAccounts(options?: { [key: string]: any }) {
  return request<API.BaseResponseListWechatAuthorizerAccountVO>('/wechat/accounts', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 设置默认公众号 POST /wechat/accounts/${param0}/default */
export async function setDefaultWechatAccount(
  params: API.wechatAccountParams,
  options?: { [key: string]: any }
) {
  const { accountId: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/wechat/accounts/${param0}/default`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 解除公众号绑定 DELETE /wechat/accounts/${param0} */
export async function unbindWechatAccount(
  params: API.wechatAccountParams,
  options?: { [key: string]: any }
) {
  const { accountId: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/wechat/accounts/${param0}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 查询微信官方发布状态 GET /article/wechat/official-status/${param0} */
export async function getWechatOfficialStatus(
  params: API.wechatOfficialStatusParams,
  options?: { [key: string]: any }
) {
  const { publishId: param0, ...queryParams } = params
  return request<API.BaseResponseWechatPublishVO>(`/article/wechat/official-status/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

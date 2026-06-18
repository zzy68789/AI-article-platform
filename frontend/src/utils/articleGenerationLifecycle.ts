import { resumeGenerationClient } from '@/api/articleController'

const GENERATION_SESSION_KEY = 'article:generation-session'
const GENERATION_LEAVE_URL = '/api/article/generation-leave'

export interface ArticleGenerationSession {
  taskId: string
  clientSessionId: string
}

let leaveListenerRegistered = false

const createClientSessionId = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export const saveGenerationSession = (taskId: string): ArticleGenerationSession => {
  const existingSession = loadGenerationSession()
  const session: ArticleGenerationSession = {
    taskId,
    clientSessionId: existingSession?.clientSessionId || createClientSessionId(),
  }
  sessionStorage.setItem(GENERATION_SESSION_KEY, JSON.stringify(session))
  return session
}

export const loadGenerationSession = (): ArticleGenerationSession | null => {
  const raw = sessionStorage.getItem(GENERATION_SESSION_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as ArticleGenerationSession
    if (parsed.taskId && parsed.clientSessionId) {
      return parsed
    }
  } catch (error) {
    console.warn('读取文章生成会话失败:', error)
  }

  sessionStorage.removeItem(GENERATION_SESSION_KEY)
  return null
}

export const clearGenerationSession = (taskId?: string) => {
  const session = loadGenerationSession()
  if (!taskId || session?.taskId === taskId) {
    sessionStorage.removeItem(GENERATION_SESSION_KEY)
  }
}

export const notifyGenerationLeave = () => {
  const session = loadGenerationSession()
  if (!session) {
    return
  }

  const body = JSON.stringify(session)
  const blob = new Blob([body], { type: 'application/json' })

  if (navigator.sendBeacon?.(GENERATION_LEAVE_URL, blob)) {
    return
  }

  fetch(GENERATION_LEAVE_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body,
    credentials: 'include',
    keepalive: true,
  }).catch((error) => {
    console.warn('发送文章生成离开通知失败:', error)
  })
}

export const registerGenerationLeaveListener = () => {
  if (leaveListenerRegistered) {
    return
  }
  window.addEventListener('pagehide', notifyGenerationLeave)
  leaveListenerRegistered = true
}

export const resumeStoredGenerationSession = async () => {
  const session = loadGenerationSession()
  if (!session) {
    return
  }

  try {
    await resumeGenerationClient(session)
  } catch (error) {
    console.warn('恢复文章生成会话失败:', error)
  }
}

<script setup>
import { computed, nextTick, ref } from 'vue'
import {
  ApiError,
  deleteJson,
  getJson,
  postJson,
  postJsonObject,
  putJson,
} from './api'

const PAGE_SIZE = 10
const mode = ref('login')
const username = ref('')
const password = ref('')
const submitting = ref(false)
const notice = ref(null)
const currentUsername = ref('')
const passwordInput = ref(null)
const notes = ref([])
const noteStatus = ref('idle')
const noteError = ref('')
const authRequired = ref(false)
const currentPage = ref(1)
const pageSize = ref(PAGE_SIZE)
const total = ref(0)
const totalPages = ref(0)
const activeView = ref('list')
const selectedNote = ref(null)
const detailStatus = ref('idle')
const detailError = ref('')
const createTitle = ref('')
const createContent = ref('')
const createStatus = ref('idle')
const createError = ref('')
const listNotice = ref('')
const titleInput = ref(null)
const editingNoteId = ref(null)
const editReturnView = ref('list')
const editTitle = ref('')
const editContent = ref('')
const editStatus = ref('idle')
const editError = ref('')
const editTitleInput = ref(null)
const operationNotice = ref('')
const operationError = ref('')
const deleteSubmitting = ref(false)
const chatMessages = ref([])
const chatInput = ref('')
const chatSending = ref(false)
const chatError = ref('')
const chatConversationId = ref('')
const chatMessagesContainer = ref(null)
const chatInputElement = ref(null)
let chatMessageSequence = 0

const isLogin = computed(() => mode.value === 'login')
const submitLabel = computed(() => (isLogin.value ? '登录' : '注册'))
const canGoPrevious = computed(
  () => currentPage.value > 1 && noteStatus.value !== 'loading',
)
const canGoNext = computed(
  () =>
    totalPages.value > 0 &&
    currentPage.value < totalPages.value &&
    noteStatus.value !== 'loading',
)

function switchMode(nextMode) {
  if (submitting.value || mode.value === nextMode) {
    return
  }

  mode.value = nextMode
  password.value = ''
  notice.value = null
  nextTick(() => passwordInput.value?.focus())
}

async function loadNotes(page = 1) {
  noteStatus.value = 'loading'
  noteError.value = ''

  try {
    const data = await getJson(
      `/notes?page=${page}&size=${pageSize.value}`,
    )

    if (!data || !Array.isArray(data.notes)) {
      throw new ApiError('笔记列表响应格式不正确', 200)
    }

    notes.value = data.notes
    currentPage.value =
      Number.isInteger(data.page) && data.page > 0 ? data.page : page
    pageSize.value =
      Number.isInteger(data.size) && data.size > 0
        ? data.size
        : PAGE_SIZE
    total.value = Number.isFinite(data.total) ? data.total : data.notes.length
    totalPages.value =
      Number.isInteger(data.totalPages) && data.totalPages >= 0
        ? data.totalPages
        : 0
    authRequired.value = false
    noteStatus.value = 'success'
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      authRequired.value = true
      noteStatus.value = 'unauthorized'
      notes.value = []
      return
    }

    noteStatus.value = 'error'
    noteError.value =
      error instanceof Error ? error.message : '笔记加载失败，请稍后重试'
  }
}

async function openNote(noteId) {
  activeView.value = 'detail'
  selectedNote.value = null
  detailStatus.value = 'loading'
  detailError.value = ''
  operationNotice.value = ''
  operationError.value = ''

  try {
    const data = await getJson(`/notes/${noteId}`)

    if (!data || data.id == null) {
      throw new ApiError('笔记详情响应格式不正确', 200)
    }

    selectedNote.value = data
    detailStatus.value = 'success'
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      authRequired.value = true
      return
    }

    detailStatus.value = 'error'
    detailError.value =
      error instanceof Error ? error.message : '笔记详情加载失败'
  }
}

function openCreate() {
  activeView.value = 'create'
  createTitle.value = ''
  createContent.value = ''
  createStatus.value = 'idle'
  createError.value = ''
  operationNotice.value = ''
  operationError.value = ''
  nextTick(() => titleInput.value?.focus())
}

function backToList() {
  activeView.value = 'list'
  selectedNote.value = null
  detailStatus.value = 'idle'
  detailError.value = ''
  createStatus.value = 'idle'
  createError.value = ''
  operationNotice.value = ''
  operationError.value = ''
}

async function submitCreate() {
  const title = createTitle.value.trim()
  const content = createContent.value.trim()

  if (!title || !content) {
    createStatus.value = 'error'
    createError.value = '标题和正文不能为空'
    return
  }

  createStatus.value = 'submitting'
  createError.value = ''

  try {
    const result = await postJson('/notes', { title, content })

    if (result !== '笔记创建任务已提交') {
      createStatus.value = 'error'
      createError.value =
        result || '后端返回了无法识别的创建结果'
      return
    }

    createStatus.value = 'accepted'
    createTitle.value = ''
    createContent.value = ''
    listNotice.value =
      '创建任务已提交。后台可能仍在处理；列表刷新只反映当前数据库状态，若暂未显示新笔记，请稍后再次刷新。'
    await loadNotes(1)
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      authRequired.value = true
      createStatus.value = 'idle'
      return
    }

    createStatus.value = 'error'
    createError.value =
      error instanceof Error ? error.message : '创建请求失败，请稍后重试'
  }
}

function openEdit(note) {
  if (!note || note.id == null) {
    return
  }

  editReturnView.value =
    activeView.value === 'detail' ? 'detail' : 'list'
  editingNoteId.value = note.id
  editTitle.value = note.title || ''
  editContent.value = note.content || ''
  editStatus.value = 'idle'
  editError.value = ''
  operationNotice.value = ''
  operationError.value = ''
  activeView.value = 'edit'
  nextTick(() => editTitleInput.value?.focus())
}

async function submitEdit() {
  const title = editTitle.value.trim()
  const content = editContent.value.trim()

  if (!title || !content) {
    editStatus.value = 'error'
    editError.value = '标题和正文不能为空'
    return
  }

  editStatus.value = 'submitting'
  editError.value = ''
  operationError.value = ''

  try {
    const result = await putJson(`/notes/${editingNoteId.value}`, {
      title,
      content,
    })

    if (result !== '笔记修改任务已提交') {
      editStatus.value = 'error'
      editError.value =
        result || '后端返回了无法识别的修改结果'
      return
    }

    editStatus.value = 'accepted'
    operationNotice.value =
      '修改任务已提交。后台可能仍在处理，当前详情或列表不保证已显示最新内容。'
    listNotice.value =
      '修改任务已提交。后台可能仍在处理；列表刷新只反映当前数据库状态。'
    await loadNotes(currentPage.value)
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      authRequired.value = true
      editStatus.value = 'idle'
      return
    }

    editStatus.value = 'error'
    editError.value =
      error instanceof Error ? error.message : '修改请求失败，请稍后重试'
  }
}

async function cancelEdit() {
  operationError.value = ''
  editError.value = ''
  editStatus.value = 'idle'

  if (editReturnView.value === 'detail' && editingNoteId.value != null) {
    await openNote(editingNoteId.value)
    return
  }

  activeView.value = 'list'
}

async function deleteNote(note) {
  if (!note || note.id == null || deleteSubmitting.value) {
    return
  }

  const title = note.title || '这篇笔记'
  const confirmed = window.confirm(
    `确定删除“${title}”吗？删除会通过 RabbitMQ 异步处理，提交后不一定立即完成。`,
  )

  if (!confirmed) {
    return
  }

  deleteSubmitting.value = true
  operationError.value = ''
  operationNotice.value = ''

  try {
    const result = await deleteJson(`/notes/${note.id}`)

    if (result !== '笔记删除任务已提交') {
      operationError.value =
        result || '后端返回了无法识别的删除结果'
      return
    }

    operationNotice.value =
      '删除任务已提交。后台可能仍在处理，刷新列表可查看当前实际状态。'
    listNotice.value =
      '删除任务已提交。后台软删除可能尚未完成，请刷新列表确认当前状态。'
    await loadNotes(currentPage.value)
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      authRequired.value = true
      return
    }

    operationError.value =
      error instanceof Error ? error.message : '删除请求失败，请稍后重试'
  } finally {
    deleteSubmitting.value = false
  }
}

async function refreshAfterCreate() {
  activeView.value = 'list'
  await loadNotes(1)
}

async function refreshCurrentList() {
  activeView.value = 'list'
  await loadNotes(currentPage.value)
}

function openChat() {
  activeView.value = 'chat'
  chatError.value = ''
  nextTick(() => chatInputElement.value?.focus())
}

function returnToNotes() {
  activeView.value = 'list'
  chatError.value = ''
}

async function scrollChatToEnd() {
  await nextTick()
  const container = chatMessagesContainer.value

  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

function appendChatMessage(role, content) {
  chatMessages.value.push({
    id: ++chatMessageSequence,
    role,
    content,
  })
  return scrollChatToEnd()
}

function startNewChat() {
  if (chatSending.value) {
    return
  }

  chatMessages.value = []
  chatConversationId.value = ''
  chatError.value = ''
  nextTick(() => chatInputElement.value?.focus())
}

async function sendChatMessage() {
  const message = chatInput.value.trim()

  if (!message) {
    chatError.value = '请输入聊天内容'
    return
  }

  if (chatSending.value) {
    return
  }

  chatError.value = ''
  chatInput.value = ''
  chatSending.value = true
  await appendChatMessage('user', message)

  try {
    const payload = { message }

    if (chatConversationId.value) {
      payload.conversationId = chatConversationId.value
    }

    const data = await postJsonObject('/chat', payload)

    if (!data || typeof data.reply !== 'string' || !data.reply.trim()) {
      throw new ApiError('AI 回复内容为空或格式不正确', 200)
    }

    if (data.conversationId != null) {
      if (
        typeof data.conversationId !== 'string' ||
        !data.conversationId.trim()
      ) {
        throw new ApiError('AI 会话标识格式不正确', 200)
      }

      chatConversationId.value = data.conversationId.trim()
    }

    await appendChatMessage('assistant', data.reply.trim())
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      authRequired.value = true
      chatError.value = '登录状态已失效，请重新登录后再使用 AI 对话'
      return
    }

    if (error instanceof ApiError && error.status === 502) {
      chatError.value = 'AI 服务暂时不可用，请稍后再试'
      return
    }

    const detail =
      error instanceof Error ? error.message : '未知请求错误'
    chatError.value = `发送失败：${detail}`
  } finally {
    chatSending.value = false
    await scrollChatToEnd()
  }
}

function returnToLogin() {
  currentUsername.value = ''
  notes.value = []
  noteStatus.value = 'idle'
  noteError.value = ''
  authRequired.value = false
  currentPage.value = 1
  pageSize.value = PAGE_SIZE
  total.value = 0
  totalPages.value = 0
  activeView.value = 'list'
  selectedNote.value = null
  detailStatus.value = 'idle'
  detailError.value = ''
  createTitle.value = ''
  createContent.value = ''
  createStatus.value = 'idle'
  createError.value = ''
  listNotice.value = ''
  editingNoteId.value = null
  editReturnView.value = 'list'
  editTitle.value = ''
  editContent.value = ''
  editStatus.value = 'idle'
  editError.value = ''
  operationNotice.value = ''
  operationError.value = ''
  deleteSubmitting.value = false
  chatMessages.value = []
  chatInput.value = ''
  chatSending.value = false
  chatError.value = ''
  chatConversationId.value = ''
  mode.value = 'login'
  password.value = ''
  notice.value = {
    type: 'error',
    message: '登录状态已失效，请重新登录',
  }
  nextTick(() => passwordInput.value?.focus())
}

function formatDate(value) {
  if (!value) {
    return '时间未知'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return '时间未知'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function summarize(content) {
  const text = (content || '').trim()

  if (!text) {
    return '暂无正文'
  }

  return text.length > 140 ? `${text.slice(0, 140)}…` : text
}

async function submit() {
  const normalizedUsername = username.value.trim()

  if (!normalizedUsername || !password.value.trim()) {
    notice.value = {
      type: 'error',
      message: '用户名和密码不能为空',
    }
    return
  }

  submitting.value = true
  notice.value = null

  try {
    const path = isLogin.value ? '/users/login' : '/users/register'
    const result = await postJson(path, {
      username: normalizedUsername,
      password: password.value,
    })

    if (isLogin.value && result === '登录成功') {
      username.value = normalizedUsername
      password.value = ''
      currentUsername.value = normalizedUsername
      notice.value = null
      activeView.value = 'list'
      listNotice.value = ''
      await loadNotes(1)
      return
    }

    if (!isLogin.value && result === '注册成功') {
      mode.value = 'login'
      username.value = normalizedUsername
      password.value = ''
      notice.value = {
        type: 'success',
        message: '注册成功，请使用新账号登录',
      }
      nextTick(() => passwordInput.value?.focus())
      return
    }

    notice.value = {
      type: 'error',
      message: result || '后端返回了空结果，请检查服务日志',
    }
  } catch (error) {
    notice.value = {
      type: 'error',
      message:
        error instanceof Error ? error.message : '请求失败，请稍后重试',
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page" :class="{ 'notes-page': currentUsername }">
    <section class="intro">
      <p class="brand">JotangNote</p>
      <div>
        <p class="eyebrow">PART 4 · 前后端联调</p>
        <h1>把零散的想法，留成可以继续生长的笔记。</h1>
        <p class="intro-copy">
          登录后查看、阅读、提交笔记，也可以和 AI 助手围绕你的笔记内容对话。
          账号身份由后端 Session 确认。
        </p>
      </div>
      <p class="stage-note">
        当前阶段包含笔记列表、详情、创建、编辑、删除和 AI 对话。
      </p>
    </section>

    <section class="auth-area">
      <div class="auth-card" :class="{ 'notes-card': currentUsername }">
        <template v-if="!currentUsername">
          <header class="card-header">
            <p class="eyebrow">WELCOME BACK</p>
            <h2>{{ isLogin ? '登录 JotangNote' : '创建新账号' }}</h2>
            <p>
              {{
                isLogin
                  ? '使用已有账号继续。登录成功后，浏览器会保存后端 Session。'
                  : '注册成功后不会自动登录，请返回登录页完成验证。'
              }}
            </p>
          </header>

          <div class="mode-switch" role="tablist" aria-label="账号操作">
            <button
              type="button"
              role="tab"
              :aria-selected="isLogin"
              :class="{ active: isLogin }"
              :disabled="submitting"
              @click="switchMode('login')"
            >
              登录
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="!isLogin"
              :class="{ active: !isLogin }"
              :disabled="submitting"
              @click="switchMode('register')"
            >
              注册
            </button>
          </div>

          <p
            v-if="notice"
            class="notice"
            :class="notice.type"
            role="status"
            aria-live="polite"
          >
            {{ notice.message }}
          </p>

          <form class="auth-form" @submit.prevent="submit">
            <label for="username">用户名</label>
            <input
              id="username"
              v-model="username"
              name="username"
              type="text"
              autocomplete="username"
              maxlength="50"
              placeholder="请输入用户名"
              :disabled="submitting"
              required
            />

            <label for="password">密码</label>
            <input
              id="password"
              ref="passwordInput"
              v-model="password"
              name="password"
              type="password"
              :autocomplete="isLogin ? 'current-password' : 'new-password'"
              placeholder="请输入密码"
              :disabled="submitting"
              required
            />

            <button class="submit-button" type="submit" :disabled="submitting">
              {{ submitting ? '请求处理中…' : submitLabel }}
            </button>
          </form>

          <p class="form-help">
            {{ isLogin ? '还没有账号？' : '已经有账号？' }}
            <button
              type="button"
              class="text-button"
              :disabled="submitting"
              @click="switchMode(isLogin ? 'register' : 'login')"
            >
              {{ isLogin ? '立即注册' : '返回登录' }}
            </button>
          </p>
        </template>

        <template v-else-if="authRequired">
          <p class="eyebrow">LOGIN REQUIRED</p>
          <h2>需要登录</h2>
          <p class="success-copy">
            当前 Session 已失效，或你尚未登录。笔记列表不能作为空列表处理。
          </p>
          <div class="notice error" role="alert">
            请重新登录后再查看你的笔记。
          </div>
          <button class="submit-button" type="button" @click="returnToLogin">
            返回登录页面
          </button>
        </template>

        <template v-else-if="activeView === 'detail'">
          <button class="back-button" type="button" @click="backToList">
            ← 返回笔记列表
          </button>

          <div
            v-if="detailStatus === 'loading'"
            class="state-panel loading-state"
            role="status"
            aria-live="polite"
          >
            <span class="loading-dot"></span>
            <p>正在加载笔记详情…</p>
          </div>

          <div
            v-else-if="detailStatus === 'error'"
            class="state-panel error-state"
            role="alert"
          >
            <strong>笔记详情加载失败</strong>
            <p>{{ detailError }}</p>
            <button
              class="secondary-button"
              type="button"
              @click="backToList"
            >
              返回列表
            </button>
          </div>

          <article v-else-if="selectedNote" class="note-detail">
            <p class="eyebrow">NOTE DETAIL</p>
            <h2>{{ selectedNote.title || '无标题笔记' }}</h2>
            <div class="note-detail-meta">
              <span>创建于 {{ formatDate(selectedNote.createdAt) }}</span>
              <span v-if="selectedNote.updatedAt">
                更新于 {{ formatDate(selectedNote.updatedAt) }}
              </span>
            </div>

            <div class="detail-actions">
              <button
                class="secondary-button"
                type="button"
                @click="openEdit(selectedNote)"
              >
                编辑
              </button>
              <button
                class="danger-button"
                type="button"
                :disabled="deleteSubmitting"
                @click="deleteNote(selectedNote)"
              >
                {{ deleteSubmitting ? '正在提交…' : '删除' }}
              </button>
              <button
                v-if="operationNotice"
                class="secondary-button"
                type="button"
                @click="refreshCurrentList"
              >
                返回列表并刷新
              </button>
            </div>

            <div
              v-if="operationNotice"
              class="notice accepted"
              role="status"
            >
              {{ operationNotice }}
            </div>
            <div
              v-if="operationError"
              class="notice error"
              role="alert"
            >
              {{ operationError }}
            </div>

            <div class="note-detail-content">
              {{ selectedNote.content || '暂无正文' }}
            </div>
          </article>
        </template>

        <template v-else-if="activeView === 'create'">
          <button class="back-button" type="button" @click="backToList">
            ← 返回笔记列表
          </button>
          <p class="eyebrow">NEW NOTE</p>
          <h2>创建笔记</h2>
          <p class="create-intro">
            提交后后端只会确认创建任务已进入异步处理，不会立即确认数据库写入或
            返回新笔记 ID。
          </p>

          <div
            v-if="createStatus === 'accepted'"
            class="notice accepted"
            role="status"
            aria-live="polite"
          >
            后端已返回“笔记创建任务已提交”。后台消费和数据库写入可能稍后完成。
          </div>

          <div
            v-if="createStatus === 'error'"
            class="notice error"
            role="alert"
          >
            {{ createError }}
          </div>

          <form class="create-form" @submit.prevent="submitCreate">
            <label for="note-title">标题</label>
            <input
              id="note-title"
              ref="titleInput"
              v-model="createTitle"
              name="title"
              type="text"
              maxlength="255"
              placeholder="输入笔记标题"
              :disabled="createStatus === 'submitting'"
              required
            />

            <label for="note-content">正文</label>
            <textarea
              id="note-content"
              v-model="createContent"
              name="content"
              rows="9"
              placeholder="输入笔记正文"
              :disabled="createStatus === 'submitting'"
              required
            ></textarea>

            <div class="create-actions">
              <button
                class="secondary-button"
                type="button"
                :disabled="createStatus === 'submitting'"
                @click="backToList"
              >
                取消
              </button>
              <button
                class="submit-button compact-button"
                type="submit"
                :disabled="createStatus === 'submitting'"
              >
                {{
                  createStatus === 'submitting'
                    ? '正在提交…'
                    : '提交创建任务'
                }}
              </button>
            </div>
          </form>

          <div v-if="createStatus === 'accepted'" class="create-actions">
            <button
              class="secondary-button"
              type="button"
              @click="backToList"
            >
              返回列表
            </button>
            <button
              class="submit-button compact-button"
              type="button"
              @click="refreshAfterCreate"
            >
              刷新列表
            </button>
          </div>
        </template>

        <template v-else-if="activeView === 'edit'">
          <button class="back-button" type="button" @click="cancelEdit">
            ← 取消编辑
          </button>
          <p class="eyebrow">EDIT NOTE</p>
          <h2>编辑笔记</h2>
          <p class="create-intro">
            修改请求会通过 RabbitMQ 异步处理。后端只确认任务已提交，不确认数据库
            最终更新结果。
          </p>

          <div
            v-if="editStatus === 'accepted'"
            class="notice accepted"
            role="status"
            aria-live="polite"
          >
            后端已返回“笔记修改任务已提交”。后台处理可能尚未完成。
          </div>

          <div
            v-if="editStatus === 'error'"
            class="notice error"
            role="alert"
          >
            {{ editError }}
          </div>

          <form class="create-form" @submit.prevent="submitEdit">
            <label for="edit-note-title">标题</label>
            <input
              id="edit-note-title"
              ref="editTitleInput"
              v-model="editTitle"
              name="title"
              type="text"
              maxlength="255"
              placeholder="输入笔记标题"
              :disabled="editStatus === 'submitting'"
              required
            />

            <label for="edit-note-content">正文</label>
            <textarea
              id="edit-note-content"
              v-model="editContent"
              name="content"
              rows="9"
              placeholder="输入笔记正文"
              :disabled="editStatus === 'submitting'"
              required
            ></textarea>

            <div class="create-actions">
              <button
                class="secondary-button"
                type="button"
                :disabled="editStatus === 'submitting'"
                @click="cancelEdit"
              >
                取消编辑
              </button>
              <button
                class="submit-button compact-button"
                type="submit"
                :disabled="editStatus === 'submitting'"
              >
                {{
                  editStatus === 'submitting'
                    ? '正在提交…'
                    : '提交修改任务'
                }}
              </button>
            </div>
          </form>

          <div v-if="editStatus === 'accepted'" class="create-actions">
            <button
              class="secondary-button"
              type="button"
              @click="cancelEdit"
            >
              {{
                editReturnView === 'detail'
                  ? '返回详情'
                  : '返回列表'
              }}
            </button>
            <button
              class="submit-button compact-button"
              type="button"
              @click="refreshCurrentList"
            >
              刷新列表
            </button>
          </div>
        </template>

        <template v-else-if="activeView === 'chat'">
          <button class="back-button" type="button" @click="returnToNotes">
            ← 返回笔记列表
          </button>
          <header class="chat-header">
            <div>
              <p class="eyebrow">AI CHAT</p>
              <h2>AI 笔记助手</h2>
              <p class="chat-intro">
                回复由后端 AI 服务生成。同一段对话会沿用后端返回的
                conversationId 维持上下文。
              </p>
            </div>
            <button
              class="secondary-button compact-button"
              type="button"
              :disabled="chatSending"
              @click="startNewChat"
            >
              新对话
            </button>
          </header>

          <div
            ref="chatMessagesContainer"
            class="chat-messages"
            role="log"
            aria-live="polite"
            aria-label="AI 聊天消息"
          >
            <div
              v-if="chatMessages.length === 0 && !chatSending"
              class="chat-empty"
            >
              <strong>开始一段对话</strong>
              <p>输入问题后点击发送。AI 回复出现前不会显示模拟消息。</p>
            </div>

            <div
              v-for="message in chatMessages"
              :key="message.id"
              class="chat-message"
              :class="message.role"
            >
              <span class="chat-role">
                {{ message.role === 'user' ? '你' : 'AI' }}
              </span>
              <p>{{ message.content }}</p>
            </div>

            <div
              v-if="chatSending"
              class="chat-message assistant chat-loading"
              role="status"
            >
              <span class="loading-dot"></span>
              <span>AI 正在回复…</span>
            </div>
          </div>

          <p
            v-if="chatError"
            class="notice error chat-error"
            role="alert"
          >
            {{ chatError }}
          </p>

          <form class="chat-composer" @submit.prevent="sendChatMessage">
            <textarea
              id="chat-input"
              ref="chatInputElement"
              v-model="chatInput"
              rows="3"
              aria-label="发送给 AI 的消息"
              placeholder="输入消息，例如：帮我总结最近一篇笔记"
              :disabled="chatSending"
            ></textarea>
            <button
              class="submit-button compact-button"
              type="submit"
              :disabled="chatSending"
            >
              {{ chatSending ? '等待回复…' : '发送' }}
            </button>
          </form>

          <p class="chat-footnote">
            {{
              chatConversationId
                ? '当前对话已建立，后续消息会发送同一 conversationId。'
                : '首次发送成功后，后端会返回本段对话的 conversationId。'
            }}
          </p>
        </template>

        <template v-else>
          <header class="notes-header">
            <div>
              <p class="eyebrow">YOUR NOTES</p>
              <h2>{{ currentUsername }} 的笔记</h2>
              <p class="notes-summary">
                共 {{ total }} 篇笔记
                <span v-if="totalPages > 0">
                  · 第 {{ currentPage }} / {{ totalPages }} 页
                </span>
              </p>
            </div>
            <div class="notes-header-actions">
              <span class="session-badge">Session 已登录</span>
              <button
                class="secondary-button compact-button"
                type="button"
                @click="openChat"
              >
                AI 对话
              </button>
              <button
                class="secondary-button compact-button"
                type="button"
                :disabled="noteStatus === 'loading'"
                @click="loadNotes(currentPage)"
              >
                刷新
              </button>
              <button
                class="submit-button compact-button"
                type="button"
                @click="openCreate"
              >
                新建笔记
              </button>
            </div>
          </header>

          <p
            v-if="listNotice"
            class="notice accepted list-notice"
            role="status"
          >
            {{ listNotice }}
          </p>
          <p
            v-if="operationError"
            class="notice error list-notice"
            role="alert"
          >
            {{ operationError }}
          </p>

          <div
            v-if="noteStatus === 'loading'"
            class="state-panel loading-state"
            role="status"
            aria-live="polite"
          >
            <span class="loading-dot"></span>
            <p>正在加载笔记…</p>
          </div>

          <div
            v-else-if="noteStatus === 'error'"
            class="state-panel error-state"
            role="alert"
          >
            <strong>笔记加载失败</strong>
            <p>{{ noteError }}</p>
            <button
              class="secondary-button"
              type="button"
              @click="loadNotes(currentPage)"
            >
              重新加载
            </button>
          </div>

          <div
            v-else-if="notes.length === 0"
            class="state-panel empty-state"
          >
            <strong>当前没有笔记</strong>
            <p>这一页没有可展示的笔记，可以提交一个新的创建任务。</p>
            <button
              class="secondary-button"
              type="button"
              @click="openCreate"
            >
              新建笔记
            </button>
          </div>

          <template v-else>
            <ul class="notes-list">
              <li v-for="note in notes" :key="note.id" class="note-row">
                <button
                  class="note-row-button"
                  type="button"
                  @click="openNote(note.id)"
                >
                  <article>
                    <div class="note-meta">
                      <time :datetime="note.createdAt">
                        创建于 {{ formatDate(note.createdAt) }}
                      </time>
                      <span v-if="note.updatedAt">
                        更新于 {{ formatDate(note.updatedAt) }}
                      </span>
                    </div>
                    <h3>{{ note.title || '无标题笔记' }}</h3>
                    <p>{{ summarize(note.content) }}</p>
                  </article>
                </button>
                <div class="note-row-actions">
                  <button
                    class="secondary-button compact-button"
                    type="button"
                    @click="openEdit(note)"
                  >
                    编辑
                  </button>
                  <button
                    class="danger-button"
                    type="button"
                    :disabled="deleteSubmitting"
                    @click="deleteNote(note)"
                  >
                    {{ deleteSubmitting ? '正在提交…' : '删除' }}
                  </button>
                </div>
              </li>
            </ul>

            <nav
              v-if="totalPages > 1"
              class="pagination"
              aria-label="笔记分页"
            >
              <button
                class="secondary-button"
                type="button"
                :disabled="!canGoPrevious"
                @click="loadNotes(currentPage - 1)"
              >
                上一页
              </button>
              <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
              <button
                class="secondary-button"
                type="button"
                :disabled="!canGoNext"
                @click="loadNotes(currentPage + 1)"
              >
                下一页
              </button>
              <button
                class="secondary-button"
                type="button"
                :disabled="noteStatus === 'loading'"
                @click="loadNotes(currentPage)"
              >
                刷新
              </button>
            </nav>
          </template>
        </template>
      </div>
    </section>
  </main>
</template>

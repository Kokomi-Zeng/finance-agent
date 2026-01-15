<template>
  <div class="financial-agent-container">
    <!-- 左侧历史栏 -->
    <div class="sidebar-left" :class="{ collapsed: sidebarCollapsed }">
      <div class="brand-row">
        <button class="menu-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '打开菜单' : '关闭菜单'">
          <span>☰</span>
        </button>
        <div v-if="!sidebarCollapsed" class="brand">
          <span class="brand-icon">💰</span>
          <span class="brand-text">AI理财智能体</span>
        </div>
      </div>
      <div v-if="!sidebarCollapsed" class="history-list">
        <div class="new-chat-item" @click="startNewConversation">
          <span class="new-chat-text">✎ 创建新对话</span>
        </div>
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="history-item"
          :class="{ active: conv.id === chatId }"
          @click="switchConversation(conv.id)"
        >
          <div class="conv-content">
            <template v-if="editingConvId === conv.id">
              <input
                v-model="editingTitle"
                class="edit-title-input"
                @click.stop
                @keydown.enter="saveConvTitle(conv.id)"
                @keydown.escape="cancelEdit"
                ref="editInput"
              />
            </template>
            <template v-else>
              <div class="conv-title">{{ conv.title }}</div>
              <div class="conv-time">{{ formatTime(conv.createdAt) }}</div>
            </template>
          </div>
          <div v-if="editingConvId !== conv.id" class="action-btns">
            <button
              class="edit-btn"
              @click.stop="startEdit(conv)"
              title="重命名"
            >✎</button>
            <button
              class="delete-btn"
              @click.stop="deleteConversation(conv.id)"
              title="删除"
            >✕</button>
          </div>
        </div>
        <div v-if="conversations.length === 0" class="no-history">
          暂无对话历史
        </div>
      </div>
    </div>

    <!-- 手机端遮罩层 -->
    <div
      v-if="!sidebarCollapsed"
      class="sidebar-overlay"
      @click="toggleSidebar"
    ></div>

    <!-- 右侧主内容区 -->
    <div class="main-content">
      <div class="header">
        <button class="mobile-menu-btn" @click="toggleSidebar">
          <span>☰</span>
        </button>
        <div class="mobile-title">AI理财智能体</div>
        <div class="back-button" @click="goBack">返回</div>
      </div>

      <div class="content-wrapper">
        <div class="chat-area">
          <ChatRoom
            :messages="messages"
            :connection-status="connectionStatus"
            ai-type="agent"
            placeholder="向AI理财智能体提问..."
            @send-message="sendMessage"
            @stop-generation="stopGeneration"
          />
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import { chatWithFinancialAgent } from '../api'

useHead({
  title: 'AI理财智能体 - AI理财管理助手',
  meta: [
    {
      name: 'description',
      content: 'AI理财智能体可以自主规划任务，搜索市场信息，生成专业理财报告'
    },
    {
      name: 'keywords',
      content: 'AI理财智能体,智能投资,自动分析,理财报告,市场分析'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const connectionStatus = ref('disconnected')
let eventSource = null

const sidebarCollapsed = ref(false)
const conversations = ref([])
const chatId = ref('')
const editingConvId = ref(null)
const editingTitle = ref('')
const editInput = ref(null)

const STORAGE_KEY = 'financial-agent-conversations'
const MESSAGES_KEY = 'financial-agent-messages'

const generateChatId = () => {
  return `agent-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

const loadConversations = () => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    conversations.value = JSON.parse(saved)
  }
}

const saveConversations = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(conversations.value))
}

const saveMessages = (id, msgs) => {
  const allMessages = JSON.parse(localStorage.getItem(MESSAGES_KEY) || '{}')
  allMessages[id] = msgs
  localStorage.setItem(MESSAGES_KEY, JSON.stringify(allMessages))
}

const loadMessages = (id) => {
  const allMessages = JSON.parse(localStorage.getItem(MESSAGES_KEY) || '{}')
  return allMessages[id] || []
}

const createNewConversation = () => {
  const newId = generateChatId()
  const newConv = {
    id: newId,
    title: '新对话',
    createdAt: Date.now()
  }
  conversations.value.unshift(newConv)
  saveConversations()
  return newId
}

const updateConversationTitle = (id, title) => {
  const conv = conversations.value.find(c => c.id === id)
  if (conv && conv.title === '新对话') {
    conv.title = title.length > 20 ? title.substring(0, 20) + '...' : title
    saveConversations()
  }
}

const startNewConversation = () => {
  if (chatId.value && messages.value.length > 0) {
    saveMessages(chatId.value, messages.value)
  }
  chatId.value = createNewConversation()
  messages.value = []
  addWelcomeMessage()
  if (window.innerWidth <= 768) {
    sidebarCollapsed.value = true
  }
}

const switchConversation = (id) => {
  if (chatId.value && messages.value.length > 0) {
    saveMessages(chatId.value, messages.value)
  }
  chatId.value = id
  const savedMessages = loadMessages(id)
  if (savedMessages.length > 0) {
    messages.value = savedMessages
  } else {
    messages.value = []
    addWelcomeMessage()
  }
  if (window.innerWidth <= 768) {
    sidebarCollapsed.value = true
  }
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const startEdit = async (conv) => {
  editingConvId.value = conv.id
  editingTitle.value = conv.title
  await nextTick()
  const input = Array.isArray(editInput.value) ? editInput.value[0] : editInput.value
  if (input) {
    input.focus()
    input.select()
  }
}

const saveConvTitle = (id) => {
  if (editingTitle.value.trim()) {
    const conv = conversations.value.find(c => c.id === id)
    if (conv) {
      conv.title = editingTitle.value.trim()
      saveConversations()
    }
  }
  editingConvId.value = null
  editingTitle.value = ''
}

const cancelEdit = () => {
  editingConvId.value = null
  editingTitle.value = ''
}

const handleGlobalClick = (e) => {
  if (editingConvId.value && !e.target.closest('.edit-title-input')) {
    saveConvTitle(editingConvId.value)
  }
}

const deleteConversation = (id) => {
  const index = conversations.value.findIndex(c => c.id === id)
  if (index === -1) return

  // 删除对话
  conversations.value.splice(index, 1)
  saveConversations()

  // 删除对应的消息
  const allMessages = JSON.parse(localStorage.getItem(MESSAGES_KEY) || '{}')
  delete allMessages[id]
  localStorage.setItem(MESSAGES_KEY, JSON.stringify(allMessages))

  // 如果删除的是当前对话，切换到其他对话
  if (chatId.value === id) {
    if (conversations.value.length > 0) {
      switchConversation(conversations.value[0].id)
    } else {
      chatId.value = createNewConversation()
      messages.value = []
      addWelcomeMessage()
    }
  }
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return date.toLocaleDateString()
}

const addWelcomeMessage = () => {
  addMessage('你好！我是AI理财智能体。\n\n我整合了专业理财知识库，可以为你提供：\n• 专业理财知识问答\n• 搜索最新市场资讯\n• 分析投资机会\n• 生成理财报告\n• 下载研究资料\n\n请告诉我你的理财需求，我会自动规划并执行任务。\n\n⚠️ 温馨提示：投资有风险，建议仅供参考。', false)
}

const addMessage = (content, isUser, type = '') => {
  messages.value.push({
    content,
    isUser,
    type,
    time: new Date().getTime()
  })
}

const sendMessage = (message) => {
  addMessage(message, true, 'user-question')
  updateConversationTitle(chatId.value, message)

  if (eventSource) {
    eventSource.close()
  }

  connectionStatus.value = 'connecting'

  addMessage('', false, 'thinking')
  const loadingMessageIndex = messages.value.length - 1
  messages.value[loadingMessageIndex].content = '⏳ 正在连接智能体...'
  messages.value[loadingMessageIndex].isThinking = true
  messages.value[loadingMessageIndex].thinkingSteps = []

  eventSource = chatWithFinancialAgent(message, chatId.value)

  const thinkingSteps = []
  let thinkingMessageIndex = loadingMessageIndex
  let resultMessageIndex = -1

  eventSource.onmessage = (event) => {
    const data = event.data

    if (data && data !== '[DONE]') {
      try {
        const jsonData = JSON.parse(data)

        if (jsonData.type === 'thinking') {
          thinkingSteps.push({
            step: jsonData.step,
            content: jsonData.content
          })
          messages.value[thinkingMessageIndex].thinkingSteps = [...thinkingSteps]
          messages.value[thinkingMessageIndex].currentStep = jsonData.content
          messages.value[thinkingMessageIndex].content = `🤔 正在分析... (${thinkingSteps.length} 步)`
          messages.value[thinkingMessageIndex].isThinking = true
          messages.value[thinkingMessageIndex].expanded = false
        } else if (jsonData.type === 'terminate') {
          if (thinkingMessageIndex >= 0) {
            messages.value[thinkingMessageIndex].terminated = true
            messages.value[thinkingMessageIndex].terminateReason = jsonData.reason
            messages.value[thinkingMessageIndex].isThinking = false
            messages.value[thinkingMessageIndex].content = `✓ 分析完成 (${thinkingSteps.length} 步)`
            messages.value[thinkingMessageIndex].expanded = false
          }
        } else if (jsonData.type === 'result') {
          addMessage(jsonData.content, false, 'ai-result')
          if (thinkingMessageIndex >= 0) {
            messages.value[thinkingMessageIndex].isThinking = false
            messages.value[thinkingMessageIndex].content = `✓ 分析完成 (${thinkingSteps.length} 步)`
            messages.value[thinkingMessageIndex].expanded = false
          }
        } else if (jsonData.type === 'result_start') {
          addMessage('', false, 'ai-result')
          resultMessageIndex = messages.value.length - 1
          if (thinkingMessageIndex >= 0) {
            messages.value[thinkingMessageIndex].isThinking = false
            messages.value[thinkingMessageIndex].content = `✓ 分析完成 (${thinkingSteps.length} 步)`
            messages.value[thinkingMessageIndex].expanded = false
          }
        } else if (jsonData.type === 'result_chunk') {
          if (resultMessageIndex >= 0) {
            messages.value[resultMessageIndex].content += jsonData.content
          }
        } else if (jsonData.type === 'result_end') {
          resultMessageIndex = -1
        }
      } catch (e) {
        console.warn('Non-JSON message received:', data)
        addMessage(data, false, 'ai-answer')
      }
    }

    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      eventSource.close()
      saveMessages(chatId.value, messages.value)
    }
  }

  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    connectionStatus.value = 'error'
    eventSource.close()
    saveMessages(chatId.value, messages.value)
  }
}

const stopGeneration = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  connectionStatus.value = 'disconnected'

  // 更新最后一条消息的状态
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && !lastMessage.isUser) {
    if (lastMessage.type === 'thinking' && lastMessage.isThinking) {
      lastMessage.isThinking = false
      lastMessage.content = `⏹ 已停止 (${lastMessage.thinkingSteps?.length || 0} 步)`
    } else if (lastMessage.type === 'ai-result' && !lastMessage.content) {
      lastMessage.content = '(已停止生成)'
    }
  }

  saveMessages(chatId.value, messages.value)
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  if (window.innerWidth <= 768) {
    sidebarCollapsed.value = true
  }
  loadConversations()

  if (conversations.value.length > 0) {
    chatId.value = conversations.value[0].id
    const savedMessages = loadMessages(chatId.value)
    if (savedMessages.length > 0) {
      messages.value = savedMessages
    } else {
      addWelcomeMessage()
    }
  } else {
    chatId.value = createNewConversation()
    addWelcomeMessage()
  }

  document.addEventListener('click', handleGlobalClick)
})

onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
  if (chatId.value && messages.value.length > 0) {
    saveMessages(chatId.value, messages.value)
  }
  document.removeEventListener('click', handleGlobalClick)
})
</script>

<style scoped>
.financial-agent-container {
  display: flex;
  flex-direction: row;
  height: 100vh;
  width: 100%;
  background: #0a1628;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

/* 左侧历史栏 - 轻量化设计 */
.sidebar-left {
  width: 260px;
  height: 100vh;
  background: #0d1c30;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
  z-index: 1000;
}

.sidebar-left.collapsed {
  width: 70px;
}

.brand-row {
  position: relative;
  padding: 16px;
  flex-shrink: 0;
  height: 70px;
  box-sizing: border-box;
}

.menu-btn {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 38px;
  height: 38px;
  padding: 0;
  border: none;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.6);
  border-radius: 10px;
  cursor: pointer;
  font-size: 18px;
  transition: background 0.2s, color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-btn:hover {
  background: rgba(203, 166, 89, 0.12);
  color: rgba(203, 166, 89, 0.9);
}

.brand {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.brand-icon {
  font-size: 18px;
}

.brand-text {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  letter-spacing: 0.5px;
}


.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  scrollbar-width: none;
  -ms-overflow-style: none;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  touch-action: pan-y;
}

.history-list::-webkit-scrollbar {
  display: none;
}

.new-chat-item {
  padding: 12px 14px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: all 0.2s;
  background: #132640;
  text-align: center;
}

.new-chat-item:hover {
  background: #182d4a;
}

.new-chat-text {
  font-size: 13px;
  color: rgba(203, 166, 89, 0.9);
  font-weight: 500;
}

.history-item {
  padding: 12px 14px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;
  background: transparent;
  position: relative;
}

.history-item:hover {
  background: rgba(255, 255, 255, 0.03);
}

.history-item:hover .action-btns {
  opacity: 1;
}

.conv-content {
  text-align: center;
}

.action-btns {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.edit-btn,
.delete-btn {
  width: 26px;
  height: 26px;
  border: none;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.5);
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.edit-btn:hover {
  background: rgba(203, 166, 89, 0.15);
  color: rgba(203, 166, 89, 0.9);
}

.delete-btn:hover {
  background: rgba(255, 100, 100, 0.15);
  color: rgba(255, 100, 100, 0.9);
}

.edit-title-input {
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  font-weight: 400;
  font-family: inherit;
  outline: none;
  margin-bottom: 4px;
  text-align: center;
  box-sizing: border-box;
}

.history-item.active {
  background: linear-gradient(135deg,
    rgba(203, 166, 89, 0.1) 0%,
    rgba(203, 166, 89, 0.05) 100%
  );
}

.conv-title {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 400;
}

.conv-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
}

.no-history {
  text-align: center;
  color: rgba(255, 255, 255, 0.25);
  padding: 24px;
  font-size: 13px;
}

/* 右侧主内容区 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(180deg,
    #0f1f35 0%,
    #132640 50%,
    #0f1f35 100%
  );
}

.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 24px;
  background: transparent;
  z-index: 50;
  height: 50px;
  flex-shrink: 0;
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
}

.back-button {
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  justify-self: end;
  color: rgba(203, 166, 89, 0.8);
  border: 1px solid rgba(203, 166, 89, 0.2);
  border-radius: 8px;
  padding: 8px 16px;
  background: rgba(203, 166, 89, 0.05);
  font-weight: 500;
}

.back-button:hover {
  background: rgba(203, 166, 89, 0.1);
  border-color: rgba(203, 166, 89, 0.3);
}

.mobile-title {
  display: none;
}

.title {
  font-size: 17px;
  font-weight: 600;
  margin: 0;
  text-align: center;
  justify-self: center;
  color: rgba(255, 255, 255, 0.9);
  letter-spacing: 0.5px;
}

.header-spacer {
  justify-self: start;
}

/* 移动端菜单按钮 - 桌面端隐藏但占位 */
.mobile-menu-btn {
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  background: transparent;
  color: transparent;
  border-radius: 8px;
  cursor: default;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  justify-self: start;
  visibility: hidden;
  pointer-events: none;
}

.content-wrapper {
  flex: 1;
  width: 100%;
  overflow: hidden;
  position: relative;
}

.chat-area {
  width: 100%;
  height: 100%;
  padding: 0;
  overflow: hidden;
}

/* 遮罩层 */
.sidebar-overlay {
  display: none;
}

/* 响应式 - 平板 */
@media (max-width: 768px) {
  .sidebar-left {
    position: fixed;
    left: 0;
    top: 0;
    z-index: 1000;
    width: 280px;
    height: 100vh;
    transform: translateX(0);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.3);
  }

  .sidebar-left.collapsed {
    transform: translateX(-100%);
    width: 280px;
    box-shadow: none;
  }

  .sidebar-overlay {
    display: block;
    position: fixed;
    left: 0;
    top: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(4px);
    z-index: 999;
  }

  .edit-btn {
    display: none;
  }

  .mobile-menu-btn {
    visibility: visible;
    pointer-events: auto;
    cursor: pointer;
    background: rgba(255, 255, 255, 0.03);
    color: rgba(255, 255, 255, 0.5);
    position: absolute;
    left: 16px;
    top: 50%;
    transform: translateY(-50%);
    transition: all 0.2s;
  }

  .mobile-menu-btn:hover {
    background: rgba(203, 166, 89, 0.08);
    color: rgba(203, 166, 89, 0.9);
  }

  .header {
    padding: 0 16px;
    background: #0d1c30;
    position: relative;
    justify-content: center;
  }

  .mobile-title {
    display: block;
    font-size: 15px;
    font-weight: 600;
    color: rgba(255, 255, 255, 0.9);
    letter-spacing: 0.5px;
  }

  .back-button {
    position: absolute;
    right: 16px;
    top: 50%;
    transform: translateY(-50%);
  }

  .title {
    font-size: 16px;
  }
}

/* 响应式 - 手机 */
@media (max-width: 480px) {
  .sidebar-left {
    width: 85vw;
    max-width: 300px;
  }

  .mobile-menu-btn {
    left: 12px;
    width: 32px;
    height: 32px;
    font-size: 16px;
  }

  .header {
    padding: 0 12px;
  }

  .back-button {
    font-size: 12px;
    padding: 6px 12px;
  }

  .title {
    font-size: 15px;
    text-align: center;
  }
}
</style>

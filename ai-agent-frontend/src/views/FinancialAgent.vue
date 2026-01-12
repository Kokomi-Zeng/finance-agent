<template>
  <div class="financial-agent-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">AI理财智能体</h1>
      <div class="header-actions">
        <button class="new-chat-btn" @click="startNewConversation">+ 新对话</button>
        <button class="history-btn" @click="toggleHistory">历史</button>
      </div>
    </div>

    <!-- 会话历史侧边栏 -->
    <div class="history-sidebar" :class="{ open: showHistory }">
      <div class="history-header">
        <h3>对话历史</h3>
        <button class="close-btn" @click="toggleHistory">×</button>
      </div>
      <div class="history-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="history-item"
          :class="{ active: conv.id === chatId }"
          @click="switchConversation(conv.id)"
        >
          <div class="conv-title">{{ conv.title }}</div>
          <div class="conv-time">{{ formatTime(conv.createdAt) }}</div>
        </div>
        <div v-if="conversations.length === 0" class="no-history">
          暂无对话历史
        </div>
      </div>
    </div>

    <div class="content-wrapper">
      <div class="chat-area">
        <ChatRoom
          :messages="messages"
          :connection-status="connectionStatus"
          ai-type="agent"
          @send-message="sendMessage"
        />
      </div>
    </div>

    <div class="footer-container">
      <AppFooter />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithFinancialAgent } from '../api'

// 设置页面标题和元数据
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

// 会话管理
const showHistory = ref(false)
const conversations = ref([])
const chatId = ref('')

// localStorage key（每个浏览器独立）
const STORAGE_KEY = 'financial-agent-conversations'
const MESSAGES_KEY = 'financial-agent-messages'

// 生成唯一的会话ID
const generateChatId = () => {
  return `agent-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

// 从 localStorage 加载会话列表
const loadConversations = () => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    conversations.value = JSON.parse(saved)
  }
}

// 保存会话列表到 localStorage
const saveConversations = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(conversations.value))
}

// 保存消息到 localStorage
const saveMessages = (id, msgs) => {
  const allMessages = JSON.parse(localStorage.getItem(MESSAGES_KEY) || '{}')
  allMessages[id] = msgs
  localStorage.setItem(MESSAGES_KEY, JSON.stringify(allMessages))
}

// 加载消息从 localStorage
const loadMessages = (id) => {
  const allMessages = JSON.parse(localStorage.getItem(MESSAGES_KEY) || '{}')
  return allMessages[id] || []
}

// 创建新会话
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

// 更新会话标题（使用第一条消息）
const updateConversationTitle = (id, title) => {
  const conv = conversations.value.find(c => c.id === id)
  if (conv && conv.title === '新对话') {
    conv.title = title.length > 20 ? title.substring(0, 20) + '...' : title
    saveConversations()
  }
}

// 开始新对话
const startNewConversation = () => {
  // 保存当前会话的消息
  if (chatId.value && messages.value.length > 0) {
    saveMessages(chatId.value, messages.value)
  }
  chatId.value = createNewConversation()
  messages.value = []
  addWelcomeMessage()
}

// 切换会话
const switchConversation = (id) => {
  // 保存当前会话的消息
  if (chatId.value && messages.value.length > 0) {
    saveMessages(chatId.value, messages.value)
  }
  chatId.value = id
  // 加载历史消息
  const savedMessages = loadMessages(id)
  if (savedMessages.length > 0) {
    messages.value = savedMessages
  } else {
    messages.value = []
    addWelcomeMessage()
  }
  showHistory.value = false
}

// 切换历史侧边栏
const toggleHistory = () => {
  showHistory.value = !showHistory.value
}

// 格式化时间
const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return date.toLocaleDateString()
}

// 添加欢迎消息
const addWelcomeMessage = () => {
  addMessage('你好！我是AI理财智能体。\n\n我整合了专业理财知识库，可以为你提供：\n• 专业理财知识问答\n• 搜索最新市场资讯\n• 分析投资机会\n• 生成理财报告\n• 下载研究资料\n\n请告诉我你的理财需求，我会自动规划并执行任务。\n\n⚠️ 温馨提示：投资有风险，建议仅供参考。', false)
}

// 添加消息到列表
const addMessage = (content, isUser, type = '') => {
  messages.value.push({
    content,
    isUser,
    type,
    time: new Date().getTime()
  })
}

// 发送消息
const sendMessage = (message) => {
  addMessage(message, true, 'user-question')

  // 更新会话标题
  updateConversationTitle(chatId.value, message)

  // 连接SSE
  if (eventSource) {
    eventSource.close()
  }

  // 设置连接状态
  connectionStatus.value = 'connecting'

  // 立即显示加载状态，让用户知道正在处理
  addMessage('', false, 'thinking')
  const loadingMessageIndex = messages.value.length - 1
  messages.value[loadingMessageIndex].content = '⏳ 正在连接智能体...'
  messages.value[loadingMessageIndex].isThinking = true
  messages.value[loadingMessageIndex].thinkingSteps = []

  eventSource = chatWithFinancialAgent(message, chatId.value)

  // 存储思考步骤
  const thinkingSteps = []
  let thinkingMessageIndex = loadingMessageIndex
  let resultMessageIndex = -1

  // 监听SSE消息
  eventSource.onmessage = (event) => {
    const data = event.data

    if (data && data !== '[DONE]') {
      try {
        // 解析JSON消息
        const jsonData = JSON.parse(data)

        if (jsonData.type === 'thinking') {
          // 思考步骤
          thinkingSteps.push({
            step: jsonData.step,
            content: jsonData.content
          })

          // 更新思考消息内容（消息已在发送时创建）
          messages.value[thinkingMessageIndex].thinkingSteps = [...thinkingSteps]
          messages.value[thinkingMessageIndex].currentStep = jsonData.content
          messages.value[thinkingMessageIndex].content = `🤔 正在分析... (${thinkingSteps.length} 步)`
          messages.value[thinkingMessageIndex].isThinking = true
          messages.value[thinkingMessageIndex].expanded = false

        } else if (jsonData.type === 'terminate') {
          // 终止消息
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
      // 保存消息到 localStorage
      saveMessages(chatId.value, messages.value)
    }
  }

  // 监听SSE错误
  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    connectionStatus.value = 'error'
    eventSource.close()
    // 保存消息到 localStorage
    saveMessages(chatId.value, messages.value)
  }
}

// 返回主页
const goBack = () => {
  router.push('/')
}

// 页面加载时初始化
onMounted(() => {
  // 加载会话历史
  loadConversations()

  // 如果有历史会话，恢复最近的一个；否则创建新会话
  if (conversations.value.length > 0) {
    chatId.value = conversations.value[0].id
    // 加载历史消息
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
})

// 组件销毁前关闭SSE连接并保存消息
onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
  // 保存当前会话的消息
  if (chatId.value && messages.value.length > 0) {
    saveMessages(chatId.value, messages.value)
  }
})
</script>

<style scoped>
.financial-agent-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background-color: #0d1b2a;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

.header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 0 24px;
  background: linear-gradient(135deg, #1b263b 0%, #0d1b2a 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  z-index: 10;
  border-bottom: 1px solid rgba(203, 166, 89, 0.3);
  height: 60px;
  flex-shrink: 0;
}

.back-button {
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: all 0.2s;
  justify-self: start;
  color: #94a3b8;
}

.back-button:hover {
  color: #cba659;
}

.back-button:before {
  content: '←';
  margin-right: 8px;
}

.title {
  font-size: 20px;
  font-weight: bold;
  margin: 0;
  text-align: center;
  justify-self: center;
  color: #cba659;
}

.header-actions {
  display: flex;
  gap: 8px;
  justify-self: end;
}

.new-chat-btn,
.history-btn {
  padding: 6px 12px;
  border: 1px solid rgba(203, 166, 89, 0.5);
  background: transparent;
  color: #cba659;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.new-chat-btn:hover,
.history-btn:hover {
  background: rgba(203, 166, 89, 0.2);
  border-color: #cba659;
}

/* 历史侧边栏 */
.history-sidebar {
  position: fixed;
  top: 0;
  right: -300px;
  width: 300px;
  height: 100vh;
  background: linear-gradient(135deg, #1b263b 0%, #0d1b2a 100%);
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.3);
  z-index: 200;
  transition: right 0.3s ease;
  display: flex;
  flex-direction: column;
  border-left: 1px solid rgba(203, 166, 89, 0.3);
}

.history-sidebar.open {
  right: 0;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid rgba(203, 166, 89, 0.2);
  background: rgba(203, 166, 89, 0.1);
}

.history-header h3 {
  margin: 0;
  font-size: 16px;
  color: #cba659;
}

.close-btn {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #cba659;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.history-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
  border: 1px solid transparent;
}

.history-item:hover {
  background: rgba(203, 166, 89, 0.1);
}

.history-item.active {
  background: rgba(203, 166, 89, 0.15);
  border: 1px solid rgba(203, 166, 89, 0.3);
}

.conv-title {
  font-size: 14px;
  color: #e2e8f0;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 12px;
  color: #64748b;
}

.no-history {
  text-align: center;
  color: #64748b;
  padding: 20px;
  font-size: 14px;
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

.footer-container {
  flex-shrink: 0;
  width: 100%;
  height: 80px;
  overflow: hidden;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .header {
    padding: 0 16px;
  }

  .title {
    font-size: 18px;
  }

  .history-sidebar {
    width: 280px;
    right: -280px;
  }

  .header-actions {
    gap: 4px;
  }

  .new-chat-btn,
  .history-btn {
    padding: 5px 8px;
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 0 12px;
    grid-template-columns: auto 1fr auto;
    gap: 8px;
  }

  .back-button {
    font-size: 14px;
  }

  .title {
    font-size: 14px;
  }

  .history-sidebar {
    width: 260px;
    right: -260px;
  }

  .new-chat-btn,
  .history-btn {
    padding: 4px 6px;
    font-size: 10px;
  }
}
</style>

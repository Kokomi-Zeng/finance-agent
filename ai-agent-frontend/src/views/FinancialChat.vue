<template>
  <div class="financial-chat-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">理财咨询</h1>
      <div class="placeholder"></div>
    </div>

    <div class="content-wrapper">
      <div class="chat-area">
        <ChatRoom
          :messages="messages"
          :connection-status="connectionStatus"
          ai-type="financial"
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
import { chatWithFinancialApp } from '../api'

// 设置页面标题和元数据
useHead({
  title: '理财咨询 - AI理财管理助手',
  meta: [
    {
      name: 'description',
      content: '与AI理财顾问进行对话，获取个性化的理财建议和投资分析'
    },
    {
      name: 'keywords',
      content: 'AI理财,投资咨询,理财建议,智能顾问,财务规划'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const connectionStatus = ref('disconnected')
let eventSource = null

// 生成唯一的会话ID
const chatId = ref('chat_' + Date.now())

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

  // 连接SSE
  if (eventSource) {
    eventSource.close()
  }

  // 设置连接状态
  connectionStatus.value = 'connecting'

  // 添加一个空的AI回复消息
  addMessage('', false, 'ai-answer')
  const aiMessageIndex = messages.value.length - 1

  eventSource = chatWithFinancialApp(message, chatId.value)

  // 监听SSE消息
  eventSource.onmessage = (event) => {
    const data = event.data

    if (data && data !== '[DONE]') {
      // 累加到AI回复
      messages.value[aiMessageIndex].content += data
      connectionStatus.value = 'connected'
    }

    if (data === '[DONE]') {
      // 完成后关闭连接
      connectionStatus.value = 'disconnected'
      eventSource.close()
    }
  }

  // 监听SSE错误
  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    connectionStatus.value = 'error'
    eventSource.close()
  }
}

// 返回主页
const goBack = () => {
  router.push('/')
}

// 页面加载时添加欢迎消息
onMounted(() => {
  addMessage('你好！我是AI理财顾问。我可以帮助你：\n\n• 分析投资组合\n• 制定理财计划\n• 解答理财问题\n• 提供投资建议\n\n请问有什么可以帮助你的吗？\n\n⚠️ 温馨提示：投资有风险，建议仅供参考。', false)
})

// 组件销毁前关闭SSE连接
onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>

<style scoped>
.financial-chat-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f0f7f4;
}

.header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 16px 24px;
  background-color: #1B5E20;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: opacity 0.2s;
  justify-self: start;
}

.back-button:hover {
  opacity: 0.8;
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
}

.placeholder {
  width: 1px;
  justify-self: end;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.chat-area {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  position: relative;
  min-height: calc(100vh - 56px - 180px);
  margin-bottom: 16px;
}

.footer-container {
  margin-top: auto;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .header {
    padding: 12px 16px;
  }

  .title {
    font-size: 18px;
  }

  .chat-area {
    padding: 12px;
    min-height: calc(100vh - 48px - 160px);
    margin-bottom: 12px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 10px 12px;
  }

  .back-button {
    font-size: 14px;
  }

  .title {
    font-size: 16px;
  }

  .chat-area {
    padding: 8px;
    min-height: calc(100vh - 42px - 150px);
    margin-bottom: 8px;
  }
}
</style>

<template>
  <div class="chat-container">
    <!-- 聊天记录区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
        <!-- AI消息 -->
        <div v-if="!msg.isUser"
             class="message ai-message"
             :class="[msg.type]">
          <div class="avatar ai-avatar">
            <AiAvatarFallback :type="aiType" />
          </div>
          <div class="message-bubble">
            <!-- 思考过程消息 -->
            <div v-if="msg.type === 'thinking'" class="thinking-content">
              <div class="thinking-header"
                   :class="{ 'thinking-active': msg.isThinking }"
                   @click="msg.expanded = !msg.expanded">
                <span class="thinking-icon">{{ msg.expanded ? '▼' : '▶' }}</span>
                <span class="thinking-text">{{ msg.content }}</span>
              </div>
              <!-- 折叠状态下显示当前步骤 -->
              <div v-if="!msg.expanded && msg.currentStep" class="current-step-preview">
                <span class="current-step-label">当前：</span>
                <span class="current-step-content">{{ msg.currentStep }}</span>
              </div>
              <!-- 展开状态显示所有步骤 -->
              <div v-if="msg.expanded" class="thinking-steps">
                <div v-for="step in msg.thinkingSteps" :key="step.step" class="thinking-step">
                  <span class="step-number">Step {{ step.step }}:</span>
                  <span class="step-content">{{ step.content }}</span>
                </div>
                <div v-if="msg.terminated" class="terminate-message">
                  {{ msg.terminateReason }}
                </div>
              </div>
            </div>
            <!-- 普通消息 -->
            <div v-else class="message-content" v-html="formatContent(msg.content)">
            </div>
            <span v-if="connectionStatus === 'connecting' && index === messages.length - 1" class="typing-indicator">▋</span>
            <div class="message-time">{{ formatTime(msg.time) }}</div>
          </div>
        </div>

        <!-- 用户消息 -->
        <div v-else class="message user-message" :class="[msg.type]">
          <div class="message-bubble">
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.time) }}</div>
          </div>
          <div class="avatar user-avatar">
            <div class="avatar-placeholder">我</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-container">
      <div class="chat-input">
        <textarea
          v-model="inputMessage"
          @keydown.enter.prevent="sendMessage"
          placeholder="请输入消息..."
          class="input-box"
          :disabled="connectionStatus === 'connecting'"
        ></textarea>
        <button
          @click="sendMessage"
          class="send-button"
          :disabled="connectionStatus === 'connecting' || !inputMessage.trim()"
        >发送</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch, computed } from 'vue'
import AiAvatarFallback from './AiAvatarFallback.vue'
import { marked } from 'marked'

// 配置 marked 选项
marked.setOptions({
  breaks: true, // 支持换行
  gfm: true     // 支持 GitHub 风格的 Markdown
})

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  connectionStatus: {
    type: String,
    default: 'disconnected'
  },
  aiType: {
    type: String,
    default: 'default'  // 'love' 或 'super'
  }
})

const emit = defineEmits(['send-message'])

const inputMessage = ref('')
const messagesContainer = ref(null)

// 根据AI类型选择不同头像
const aiAvatar = computed(() => {
  return props.aiType === 'love'
    ? '/ai-love-avatar.png'  // 恋爱大师头像
    : '/ai-super-avatar.png' // 超级智能体头像
})

// 发送消息
const sendMessage = () => {
  if (!inputMessage.value.trim()) return

  emit('send-message', inputMessage.value)
  inputMessage.value = ''
}

// 格式化时间
const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 格式化消息内容，使用 marked 渲染 Markdown
const formatContent = (content) => {
  if (!content) return ''

  // 使用 marked 解析 Markdown
  let html = marked.parse(content)

  // 为所有链接添加 target="_blank" 和样式类
  html = html.replace(
    /<a href="([^"]+)">/g,
    '<a href="$1" target="_blank" rel="noopener noreferrer" class="message-link">'
  )

  // 将纯URL转换为可点击链接（marked 可能没有自动处理的）
  // 匹配 /api/... 开头的相对路径（排除已经在a标签中的）
  html = html.replace(
    /(?<!href="|">)(\/api\/file\/(?:download|preview)\?[^\s<]+)/g,
    '<a href="$1" target="_blank" class="message-link">$1</a>'
  )

  return html
}

// 自动滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 监听消息变化与内容变化，自动滚动
watch(() => props.messages.length, () => {
  scrollToBottom()
})

watch(() => props.messages.map(m => m.content).join(''), () => {
  scrollToBottom()
})

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  background-color: #1b263b;
  border-radius: 0;
  overflow: hidden;
  position: relative;
  border: none;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE & Edge */
}

.message-wrapper {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.message {
  display: flex;
  align-items: flex-start;
  max-width: 85%;
  margin-bottom: 8px;
}

.user-message {
  margin-left: auto; /* 用户消息靠右 */
  flex-direction: row; /* 正常顺序，先气泡后头像 */
}

.ai-message {
  margin-right: auto; /* AI消息靠左 */
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar {
  margin-left: 8px; /* 用户头像在右侧，左边距 */
}

.ai-avatar {
  margin-right: 8px; /* AI头像在左侧，右边距 */
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #415a77, #778da9);
  color: white;
  font-weight: bold;
}

.message-bubble {
  padding: 12px;
  border-radius: 18px;
  position: relative;
  word-wrap: break-word;
  min-width: 100px; /* 最小宽度 */
}

.user-message .message-bubble {
  background: linear-gradient(135deg, #415a77, #778da9);
  color: white;
  border-bottom-right-radius: 4px;
  text-align: left;
}

.ai-message .message-bubble {
  background-color: rgba(255, 255, 255, 0.1);
  color: #e2e8f0;
  border-bottom-left-radius: 4px;
  text-align: left;
  border: 1px solid rgba(203, 166, 89, 0.15);
}

.message-content {
  font-size: 16px;
  line-height: 1.6;
}

/* Markdown 渲染样式 */
.message-content :deep(p) {
  margin: 0 0 8px 0;
}

.message-content :deep(p:last-child) {
  margin-bottom: 0;
}

.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4) {
  margin: 12px 0 8px 0;
  font-weight: 600;
  color: #cba659;
}

.message-content :deep(h1) {
  font-size: 1.4em;
}

.message-content :deep(h2) {
  font-size: 1.2em;
}

.message-content :deep(h3) {
  font-size: 1.1em;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.message-content :deep(li) {
  margin: 4px 0;
}

.message-content :deep(code) {
  background-color: rgba(203, 166, 89, 0.2);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.9em;
  color: #cba659;
}

.message-content :deep(pre) {
  background-color: rgba(0, 0, 0, 0.3);
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
  border: 1px solid rgba(203, 166, 89, 0.2);
}

.message-content :deep(pre code) {
  background-color: transparent;
  padding: 0;
  color: #e2e8f0;
}

.message-content :deep(blockquote) {
  border-left: 3px solid #cba659;
  margin: 8px 0;
  padding-left: 12px;
  color: #94a3b8;
}

.message-content :deep(strong) {
  font-weight: 600;
  color: #cba659;
}

.message-content :deep(em) {
  font-style: italic;
}

.message-content :deep(hr) {
  border: none;
  border-top: 1px solid rgba(203, 166, 89, 0.3);
  margin: 12px 0;
}

/* AI结果消息中的 Markdown 样式调整 */
.ai-result .message-content :deep(code) {
  background-color: rgba(255, 255, 255, 0.2);
}

.ai-result .message-content :deep(pre) {
  background-color: rgba(0, 0, 0, 0.2);
}

.ai-result .message-content :deep(blockquote) {
  border-left-color: rgba(255, 255, 255, 0.5);
  color: rgba(255, 255, 255, 0.9);
}

/* 消息中的链接样式 */
.message-content :deep(.message-link) {
  color: #cba659;
  text-decoration: underline;
  word-break: break-all;
}

.message-content :deep(.message-link:hover) {
  color: #e8d5a3;
}

/* AI结果消息中的链接 */
.ai-result .message-content :deep(.message-link) {
  color: #ffffff;
  text-decoration: underline;
}

.ai-result .message-content :deep(.message-link:hover) {
  color: #e0e0e0;
}

.message-time {
  font-size: 12px;
  opacity: 0.7;
  margin-top: 4px;
  text-align: right;
  color: #64748b;
}

.user-message .message-time {
  color: rgba(255, 255, 255, 0.7);
}

.chat-input-container {
  flex-shrink: 0;
  background: linear-gradient(135deg, #0d1b2a 0%, #1b263b 100%);
  border-top: 1px solid rgba(203, 166, 89, 0.3);
  z-index: 100;
  height: 72px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.2);
}

.chat-input {
  display: flex;
  padding: 16px;
  height: 100%;
  box-sizing: border-box;
  align-items: center;
}

.input-box {
  flex-grow: 1;
  border: 1px solid rgba(203, 166, 89, 0.3);
  border-radius: 20px;
  padding: 10px 16px;
  font-size: 16px;
  resize: none;
  min-height: 20px;
  max-height: 40px; /* 限制高度 */
  outline: none;
  transition: all 0.3s;
  overflow-y: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE & Edge */
  background-color: rgba(255, 255, 255, 0.05);
  color: #e2e8f0;
}

/* 隐藏Webkit浏览器的滚动条 */
.chat-messages::-webkit-scrollbar {
  display: none;
}

.input-box::-webkit-scrollbar {
  display: none;
}

.input-box::placeholder {
  color: #64748b;
}

.input-box:focus {
  border-color: #cba659;
  background-color: rgba(255, 255, 255, 0.08);
}

.send-button {
  margin-left: 12px;
  background: linear-gradient(135deg, #415a77, #778da9);
  color: #cba659;
  border: 1px solid rgba(203, 166, 89, 0.3);
  border-radius: 20px;
  padding: 0 20px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
  height: 40px;
  align-self: center;
  font-weight: 500;
}

.send-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #778da9, #94a3b8);
  border-color: #cba659;
  box-shadow: 0 0 10px rgba(203, 166, 89, 0.3);
}

.typing-indicator {
  display: inline-block;
  animation: blink 0.7s infinite;
  margin-left: 2px;
  color: #cba659;
}

@keyframes blink {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

.input-box:disabled, .send-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .message {
    max-width: 95%;
  }

  .message-content {
    font-size: 15px;
  }

  .chat-input {
    padding: 12px;
  }

  .input-box {
    padding: 8px 12px;
  }

  .send-button {
    padding: 0 15px;
    font-size: 14px;
  }

  .chat-messages {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .avatar {
    width: 32px;
    height: 32px;
  }

  .message-bubble {
    padding: 10px;
  }

  .message-content {
    font-size: 14px;
  }

  .chat-input-container {
    height: 64px;
  }

  .send-button {
    padding: 0 12px;
    font-size: 13px;
  }

  .chat-messages {
    padding: 12px;
  }
}

/* 新增：不同类型消息的样式 */
.ai-answer {
  animation: fadeIn 0.3s ease-in-out;
}

.ai-final {
  /* 最终回答，可以有不同的样式，例如边框高亮等 */
}

.ai-error {
  opacity: 0.7;
}

.user-question {
  /* 用户提问的特殊样式 */
}

/* 连续消息气泡样式 */
.ai-message + .ai-message {
  margin-top: 4px;
}

.ai-message + .ai-message .avatar {
  visibility: hidden;
}

.ai-message + .ai-message .message-bubble {
  border-top-left-radius: 10px;
}

/* 思考过程样式 */
.thinking-content {
  width: 100%;
}

.thinking-header {
  display: flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.thinking-header:hover {
  background-color: rgba(203, 166, 89, 0.1);
}

/* 正在思考的动画效果 */
.thinking-active {
  background: linear-gradient(90deg, rgba(203, 166, 89, 0.1) 0%, rgba(65, 90, 119, 0.1) 100%);
  animation: thinking-pulse 2s ease-in-out infinite;
}

@keyframes thinking-pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.9;
    transform: scale(1.01);
  }
}

.thinking-active .thinking-text {
  font-weight: 600;
  color: #cba659;
}

.thinking-icon {
  margin-right: 8px;
  font-size: 12px;
  transition: transform 0.2s;
  color: #cba659;
}

.thinking-text {
  flex: 1;
  font-weight: 500;
  color: #94a3b8;
}

/* 折叠状态下的当前步骤预览 */
.current-step-preview {
  margin-top: 8px;
  padding: 8px 12px;
  background-color: rgba(203, 166, 89, 0.1);
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.4;
  border-left: 2px solid #cba659;
}

.current-step-label {
  font-weight: 600;
  color: #cba659;
  margin-right: 4px;
}

.current-step-content {
  color: #94a3b8;
}

.thinking-steps {
  margin-top: 12px;
  padding: 12px;
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  border-left: 3px solid #cba659;
}

.thinking-step {
  margin-bottom: 8px;
  padding: 6px;
  font-size: 14px;
  line-height: 1.5;
}

.step-number {
  font-weight: 600;
  color: #cba659;
  margin-right: 8px;
}

.step-content {
  color: #94a3b8;
}

.terminate-message {
  margin-top: 12px;
  padding: 8px;
  background-color: rgba(203, 166, 89, 0.15);
  border-radius: 6px;
  color: #cba659;
  font-style: italic;
}

/* 最终结果样式 */
.ai-result {
  animation: fadeIn 0.5s ease-in-out;
}

.ai-result .message-bubble {
  background: linear-gradient(135deg, #415a77 0%, #1b263b 100%);
  color: white;
  border: 1px solid rgba(203, 166, 89, 0.3);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

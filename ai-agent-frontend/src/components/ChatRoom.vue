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
                   :class="{ 'thinking-active': msg.isThinking, 'no-steps': !msg.thinkingSteps || msg.thinkingSteps.length === 0 }"
                   @click="msg.thinkingSteps && msg.thinkingSteps.length > 0 ? msg.expanded = !msg.expanded : null">
                <span v-if="msg.thinkingSteps && msg.thinkingSteps.length > 0" class="thinking-icon">{{ msg.expanded ? '▼' : '▶' }}</span>
                <span class="thinking-text">{{ msg.content }}</span>
              </div>
              <!-- 折叠状态下显示当前步骤 -->
              <div v-if="!msg.expanded && msg.currentStep" class="current-step-preview">
                <span class="current-step-label">当前：</span>
                <span class="current-step-content">{{ msg.currentStep }}</span>
              </div>
              <!-- 展开状态显示所有步骤 -->
              <div v-if="msg.expanded && msg.thinkingSteps && msg.thinkingSteps.length > 0" class="thinking-steps">
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
          :placeholder="placeholder"
          class="input-box"
          :disabled="connectionStatus === 'connecting'"
        ></textarea>
        <button
          @click="sendMessage"
          class="send-button"
          :disabled="connectionStatus === 'connecting' || !inputMessage.trim()"
        >
          <span class="send-icon">↑</span>
        </button>
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
  },
  placeholder: {
    type: String,
    default: '请输入消息...'
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
  background: transparent;
  overflow: hidden;
  position: relative;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  padding-bottom: 100px;
  display: flex;
  flex-direction: column;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.chat-messages::-webkit-scrollbar {
  display: none;
}

.message-wrapper {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.message {
  display: flex;
  align-items: flex-start;
  max-width: 80%;
  margin-bottom: 8px;
}

.user-message {
  margin-left: auto;
  flex-direction: row;
}

.ai-message {
  margin-right: auto;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar {
  margin-left: 10px;
}

.ai-avatar {
  margin-right: 10px;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg,
    rgba(203, 166, 89, 0.3) 0%,
    rgba(203, 166, 89, 0.15) 100%
  );
  color: rgba(203, 166, 89, 0.9);
  font-weight: 500;
  font-size: 12px;
}

.message-bubble {
  padding: 14px 18px;
  border-radius: 16px;
  position: relative;
  word-wrap: break-word;
  min-width: 60px;
}

.user-message .message-bubble {
  background: linear-gradient(135deg,
    rgba(203, 166, 89, 0.2) 0%,
    rgba(203, 166, 89, 0.1) 100%
  );
  color: rgba(255, 255, 255, 0.9);
  border-bottom-right-radius: 4px;
  text-align: left;
}

.ai-message .message-bubble {
  background: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.85);
  border-bottom-left-radius: 4px;
  text-align: left;
}

.message-content {
  font-size: 14px;
  line-height: 1.7;
  font-weight: 400;
}

/* Markdown 渲染样式 */
.message-content :deep(p) {
  margin: 0 0 10px 0;
}

.message-content :deep(p:last-child) {
  margin-bottom: 0;
}

.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4) {
  margin: 16px 0 10px 0;
  font-weight: 600;
  color: rgba(203, 166, 89, 0.9);
}

.message-content :deep(h1) {
  font-size: 1.3em;
}

.message-content :deep(h2) {
  font-size: 1.15em;
}

.message-content :deep(h3) {
  font-size: 1.05em;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  margin: 10px 0;
  padding-left: 20px;
}

.message-content :deep(li) {
  margin: 6px 0;
  color: rgba(255, 255, 255, 0.75);
}

.message-content :deep(code) {
  background: rgba(203, 166, 89, 0.1);
  padding: 2px 8px;
  border-radius: 6px;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 0.85em;
  color: rgba(203, 166, 89, 0.9);
}

.message-content :deep(pre) {
  background: rgba(0, 0, 0, 0.25);
  padding: 14px;
  border-radius: 10px;
  overflow-x: auto;
  margin: 12px 0;
}

.message-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: rgba(255, 255, 255, 0.8);
}

.message-content :deep(blockquote) {
  border-left: 2px solid rgba(203, 166, 89, 0.4);
  margin: 12px 0;
  padding-left: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.message-content :deep(strong) {
  font-weight: 600;
  color: rgba(203, 166, 89, 0.9);
}

.message-content :deep(em) {
  font-style: italic;
  color: rgba(255, 255, 255, 0.7);
}

.message-content :deep(hr) {
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  margin: 16px 0;
}

/* 消息中的链接样式 */
.message-content :deep(.message-link) {
  color: rgba(203, 166, 89, 0.9);
  text-decoration: none;
  border-bottom: 1px solid rgba(203, 166, 89, 0.3);
  transition: all 0.2s;
  word-break: break-all;
}

.message-content :deep(.message-link:hover) {
  color: rgba(203, 166, 89, 1);
  border-bottom-color: rgba(203, 166, 89, 0.6);
}

.message-time {
  font-size: 11px;
  margin-top: 8px;
  text-align: right;
  color: rgba(255, 255, 255, 0.25);
}

.user-message .message-time {
  color: rgba(255, 255, 255, 0.35);
}

.chat-input-container {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: transparent;
  z-index: 100;
  padding: 16px 24px;
}

.chat-input {
  display: flex;
  align-items: center;
  max-width: 900px;
  margin: 0 auto;
  background: rgba(15, 25, 45, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  padding: 6px 6px 6px 20px;
  position: relative;
  min-height: 48px;
  box-sizing: border-box;
}

.input-box {
  flex: 1;
  border: none;
  padding: 0;
  padding-right: 12px;
  font-size: 14px;
  resize: none;
  height: 36px;
  min-height: 36px;
  max-height: 120px;
  outline: none;
  transition: all 0.2s;
  overflow-y: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  line-height: 36px;
  box-sizing: border-box;
}

.input-box::-webkit-scrollbar {
  display: none;
}

.input-box::placeholder {
  color: rgba(255, 255, 255, 0.25);
}

.input-box:focus {
  outline: none;
}

.chat-input:focus-within {
  border-color: rgba(203, 166, 89, 0.3);
}

.send-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(203, 166, 89, 0.9);
  color: #0a1628;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.send-icon {
  font-size: 18px;
  font-weight: bold;
}

.send-button:hover:not(:disabled) {
  background: rgba(203, 166, 89, 1);
  transform: scale(1.05);
}

.send-button:disabled {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.3);
  cursor: not-allowed;
}

.typing-indicator {
  display: inline-block;
  animation: blink 0.8s ease-in-out infinite;
  margin-left: 2px;
  color: rgba(203, 166, 89, 0.8);
}

@keyframes blink {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.input-box:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .message {
    max-width: 90%;
  }

  .chat-input-container {
    padding: 14px 16px;
  }

  .chat-messages {
    padding: 20px 16px;
    padding-bottom: 90px;
  }
}

@media (max-width: 480px) {
  .avatar {
    width: 28px;
    height: 28px;
  }

  .message-bubble {
    padding: 12px 14px;
  }

  .message-content {
    font-size: 13px;
  }

  .send-button {
    width: 32px;
    height: 32px;
  }

  .send-icon {
    font-size: 16px;
  }

  .chat-messages {
    padding: 16px 12px;
    padding-bottom: 85px;
  }
}

/* 消息类型样式 */
.ai-answer {
  animation: fadeIn 0.3s ease-out;
}

.ai-error {
  opacity: 0.6;
}

/* 连续消息 */
.ai-message + .ai-message {
  margin-top: 4px;
}

.ai-message + .ai-message .avatar {
  visibility: hidden;
}

.ai-message + .ai-message .message-bubble {
  border-top-left-radius: 12px;
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
  padding: 10px 12px;
  border-radius: 10px;
  transition: all 0.2s;
  margin: -4px;
}

.thinking-header:hover {
  background: rgba(255, 255, 255, 0.03);
}

.thinking-header.no-steps {
  cursor: default;
}

.thinking-header.no-steps:hover {
  background: transparent;
}

.thinking-active {
  background: linear-gradient(90deg,
    rgba(203, 166, 89, 0.06) 0%,
    rgba(203, 166, 89, 0.02) 100%
  );
  animation: thinking-pulse 2.5s ease-in-out infinite;
}

@keyframes thinking-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.thinking-active .thinking-text {
  font-weight: 500;
  color: rgba(203, 166, 89, 0.9);
}

.thinking-icon {
  margin-right: 10px;
  font-size: 10px;
  transition: transform 0.2s;
  color: rgba(203, 166, 89, 0.6);
}

.thinking-text {
  flex: 1;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
}

.current-step-preview {
  margin-top: 12px;
  padding: 10px 14px;
  background: rgba(203, 166, 89, 0.04);
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.5;
  border-left: 2px solid rgba(203, 166, 89, 0.3);
}

.current-step-label {
  font-weight: 500;
  color: rgba(203, 166, 89, 0.8);
  margin-right: 6px;
}

.current-step-content {
  color: rgba(255, 255, 255, 0.5);
}

.thinking-steps {
  margin-top: 14px;
  padding: 14px;
  background: rgba(0, 0, 0, 0.15);
  border-radius: 10px;
  border-left: 2px solid rgba(203, 166, 89, 0.25);
}

.thinking-step {
  margin-bottom: 10px;
  padding: 6px 0;
  font-size: 13px;
  line-height: 1.6;
}

.thinking-step:last-child {
  margin-bottom: 0;
}

.step-number {
  font-weight: 500;
  color: rgba(203, 166, 89, 0.7);
  margin-right: 10px;
}

.step-content {
  color: rgba(255, 255, 255, 0.55);
}

.terminate-message {
  margin-top: 14px;
  padding: 10px 14px;
  background: rgba(203, 166, 89, 0.06);
  border-radius: 8px;
  color: rgba(203, 166, 89, 0.7);
  font-style: italic;
  font-size: 12px;
}

/* AI结果样式 */
.ai-result {
  animation: fadeIn 0.4s ease-out;
}

.ai-result .message-bubble {
  background: linear-gradient(135deg,
    rgba(203, 166, 89, 0.08) 0%,
    rgba(203, 166, 89, 0.03) 100%
  );
  color: rgba(255, 255, 255, 0.9);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

<template>
  <div class="home-container">
    <!-- 动态背景粒子 -->
    <div class="particles">
      <div v-for="i in 20" :key="i" class="particle" :style="getParticleStyle(i)"></div>
    </div>

    <!-- 顶部导航栏 -->
    <nav class="top-nav">
      <div class="logo">
        <span class="logo-icon">◈</span>
        <span class="logo-text">FinanceAI</span>
      </div>
      <div class="nav-status">
        <span class="status-dot"></span>
        <span class="status-text">系统在线</span>
      </div>
    </nav>

    <!-- 主内容区域 -->
    <main class="main-content">
      <!-- 左侧面板 - 数据展示 -->
      <aside class="left-panel">
        <div class="data-card">
          <div class="data-label">智能分析引擎</div>
          <div class="data-value">
            <span class="value-num">v3.0</span>
            <span class="value-badge">ACTIVE</span>
          </div>
        </div>
        <div class="data-card">
          <div class="data-label">知识库规模</div>
          <div class="data-value">
            <span class="value-num">5+</span>
            <span class="value-unit">条目</span>
          </div>
        </div>
        <div class="data-card">
          <div class="data-label">模型响应速度</div>
          <div class="data-value">
            <span class="value-num">&lt;20s</span>
            <span class="value-unit">平均</span>
          </div>
        </div>
        <div class="wave-container">
          <svg class="wave-svg" viewBox="0 0 100 30" preserveAspectRatio="none">
            <path class="wave-path" d="M0,15 Q25,5 50,15 T100,15" fill="none" stroke="currentColor" stroke-width="1"/>
            <path class="wave-path delay-1" d="M0,15 Q25,25 50,15 T100,15" fill="none" stroke="currentColor" stroke-width="0.5"/>
          </svg>
        </div>
      </aside>

      <!-- 中央主区域 -->
      <section class="center-area">
        <div class="hero-section">
          <h1 class="main-title">
            <span class="title-line">AI 理财</span>
            <span class="title-line highlight">智能体</span>
          </h1>
          <p class="tagline">深度分析 · 智能决策 · 专业报告</p>
        </div>

        <div class="action-zone">
          <button class="primary-action" @click="navigateTo('/agent')">
            <span class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 6v6l4 2"/>
              </svg>
            </span>
            <span class="action-content">
              <span class="action-title">开始对话</span>
              <span class="action-desc">与AI智能体交互</span>
            </span>
            <span class="action-arrow">→</span>
          </button>
        </div>

        <div class="features-grid">
          <div class="feature-item">
            <div class="feature-icon">◉</div>
            <div class="feature-text">市场分析</div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">◎</div>
            <div class="feature-text">资讯搜索</div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">◈</div>
            <div class="feature-text">报告生成</div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">◇</div>
            <div class="feature-text">知识问答</div>
          </div>
        </div>
      </section>

      <!-- 右侧面板 - 动态信息 -->
      <aside class="right-panel">
        <div class="info-block">
          <div class="info-header">
            <span class="info-icon">●</span>
            <span class="info-title">核心能力</span>
          </div>
          <ul class="info-list">
            <li>自主任务规划</li>
            <li>多源信息整合</li>
            <li>专业知识库</li>
            <li>实时市场追踪</li>
          </ul>
        </div>
        <div class="terminal-block">
          <div class="terminal-header">
            <span class="terminal-dot"></span>
            <span class="terminal-dot"></span>
            <span class="terminal-dot"></span>
          </div>
          <div class="terminal-content">
            <p class="terminal-line">> 初始化完成</p>
            <p class="terminal-line">> 知识库已加载</p>
            <p class="terminal-line">> 等待指令...</p>
            <span class="terminal-cursor">_</span>
          </div>
        </div>
      </aside>
    </main>

    <!-- 底部信息 -->
    <footer class="bottom-bar">
      <div class="footer-left">
        <span class="footer-item">投资有风险，建议仅供参考</span>
      </div>
      <div class="footer-right">
        <span class="footer-item">Powered by AI</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'

useHead({
  title: 'AI理财管理助手 - 首页',
  meta: [
    {
      name: 'description',
      content: 'AI理财管理助手提供专业的投资分析、市场资讯搜索和理财报告生成服务'
    },
    {
      name: 'keywords',
      content: 'AI理财,投资分析,智能投资,财务管理,资产配置,理财助手,市场分析'
    }
  ]
})

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}

// 生成随机粒子样式
const getParticleStyle = (index) => {
  const size = Math.random() * 4 + 2
  const left = Math.random() * 100
  const top = Math.random() * 100
  const duration = Math.random() * 20 + 10
  const delay = Math.random() * 5

  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`
  }
}
</script>

<style scoped>
/* 配色变量 */
:root {
  --finance-primary: #0d1b2a;
  --finance-secondary: #1b263b;
  --finance-gold: #cba659;
  --finance-gold-light: #e8d5a3;
  --finance-accent: #415a77;
  --finance-light: #e2e8f0;
  --finance-muted: #94a3b8;
}

.home-container {
  min-height: 100vh;
  width: 100%;
  background: linear-gradient(135deg, #0d1b2a 0%, #1b263b 50%, #0d1b2a 100%);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

/* 粒子背景 */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.particle {
  position: absolute;
  background: radial-gradient(circle, rgba(203, 166, 89, 0.6) 0%, transparent 70%);
  border-radius: 50%;
  animation: float-particle linear infinite;
}

@keyframes float-particle {
  0%, 100% {
    transform: translateY(0) translateX(0);
    opacity: 0.3;
  }
  50% {
    transform: translateY(-30px) translateX(20px);
    opacity: 0.8;
  }
}

/* 顶部导航 */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  z-index: 10;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 28px;
  color: #cba659;
  text-shadow: 0 0 20px rgba(203, 166, 89, 0.5);
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #e2e8f0;
  letter-spacing: 2px;
}

.nav-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(27, 38, 59, 0.6);
  border-radius: 20px;
  border: 1px solid rgba(203, 166, 89, 0.2);
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #4ade80;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-size: 13px;
  color: #94a3b8;
}

/* 主内容区 */
.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 240px 1fr 240px;
  gap: 30px;
  padding: 20px 40px;
  z-index: 10;
}

/* 左侧面板 */
.left-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.data-card {
  background: rgba(27, 38, 59, 0.5);
  border: 1px solid rgba(203, 166, 89, 0.15);
  border-radius: 12px;
  padding: 16px;
  backdrop-filter: blur(10px);
}

.data-label {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.data-value {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.value-num {
  font-size: 24px;
  font-weight: 700;
  color: #cba659;
}

.value-unit {
  font-size: 12px;
  color: #94a3b8;
}

.value-badge {
  font-size: 10px;
  padding: 2px 8px;
  background: rgba(74, 222, 128, 0.2);
  color: #4ade80;
  border-radius: 10px;
}

.wave-container {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 20px 0;
}

.wave-svg {
  width: 100%;
  height: 30px;
  color: rgba(203, 166, 89, 0.4);
}

.wave-path {
  animation: wave 3s ease-in-out infinite;
}

.wave-path.delay-1 {
  animation-delay: 0.5s;
  opacity: 0.5;
}

@keyframes wave {
  0%, 100% { d: path('M0,15 Q25,5 50,15 T100,15'); }
  50% { d: path('M0,15 Q25,25 50,15 T100,15'); }
}

/* 中央区域 */
.center-area {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  gap: 40px;
}

.hero-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.main-title {
  font-size: 56px;
  font-weight: 800;
  line-height: 1.1;
  margin: 0;
}

.title-line {
  display: block;
  color: #e2e8f0;
}

.title-line.highlight {
  color: #cba659;
  text-shadow: 0 0 40px rgba(203, 166, 89, 0.4);
}

.tagline {
  font-size: 16px;
  color: #94a3b8;
  letter-spacing: 4px;
  margin: 0;
}

/* 主按钮 */
.action-zone {
  width: 100%;
  max-width: 400px;
}

.primary-action {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 28px;
  background: linear-gradient(135deg, rgba(65, 90, 119, 0.4) 0%, rgba(27, 38, 59, 0.6) 100%);
  border: 1px solid rgba(203, 166, 89, 0.3);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
}

.primary-action:hover {
  background: linear-gradient(135deg, rgba(65, 90, 119, 0.6) 0%, rgba(27, 38, 59, 0.8) 100%);
  border-color: #cba659;
  transform: translateY(-2px);
  box-shadow: 0 10px 40px rgba(203, 166, 89, 0.2);
}

.action-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(203, 166, 89, 0.15);
  border-radius: 12px;
  flex-shrink: 0;
}

.action-icon svg {
  width: 24px;
  height: 24px;
  color: #cba659;
}

.action-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.action-title {
  font-size: 18px;
  font-weight: 600;
  color: #e2e8f0;
}

.action-desc {
  font-size: 13px;
  color: #94a3b8;
}

.action-arrow {
  font-size: 20px;
  color: #cba659;
  transition: transform 0.3s;
}

.primary-action:hover .action-arrow {
  transform: translateX(4px);
}

/* 特性网格 */
.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  width: 100%;
  max-width: 500px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  background: rgba(27, 38, 59, 0.3);
  border-radius: 12px;
  border: 1px solid transparent;
  transition: all 0.3s;
}

.feature-item:hover {
  border-color: rgba(203, 166, 89, 0.3);
  background: rgba(27, 38, 59, 0.5);
}

.feature-icon {
  font-size: 20px;
  color: #cba659;
}

.feature-text {
  font-size: 12px;
  color: #94a3b8;
}

/* 右侧面板 */
.right-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-block {
  background: rgba(27, 38, 59, 0.5);
  border: 1px solid rgba(203, 166, 89, 0.15);
  border-radius: 12px;
  padding: 16px;
  backdrop-filter: blur(10px);
}

.info-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.info-icon {
  color: #cba659;
  font-size: 10px;
}

.info-title {
  font-size: 13px;
  font-weight: 600;
  color: #e2e8f0;
}

.info-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-list li {
  font-size: 12px;
  color: #94a3b8;
  padding-left: 12px;
  position: relative;
}

.info-list li::before {
  content: '—';
  position: absolute;
  left: 0;
  color: rgba(203, 166, 89, 0.5);
}

/* 终端块 */
.terminal-block {
  flex: 1;
  background: rgba(13, 27, 42, 0.8);
  border: 1px solid rgba(203, 166, 89, 0.15);
  border-radius: 12px;
  overflow: hidden;
}

.terminal-header {
  display: flex;
  gap: 6px;
  padding: 10px 12px;
  background: rgba(27, 38, 59, 0.5);
  border-bottom: 1px solid rgba(203, 166, 89, 0.1);
}

.terminal-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(203, 166, 89, 0.3);
}

.terminal-content {
  padding: 12px;
  font-family: 'Consolas', 'Monaco', monospace;
}

.terminal-line {
  font-size: 11px;
  color: #94a3b8;
  margin: 0 0 6px 0;
  line-height: 1.4;
}

.terminal-cursor {
  color: #cba659;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 底部栏 */
.bottom-bar {
  display: flex;
  justify-content: space-between;
  padding: 16px 40px;
  border-top: 1px solid rgba(203, 166, 89, 0.1);
  z-index: 10;
}

.footer-item {
  font-size: 12px;
  color: #64748b;
}

/* 响应式 - 平板 */
@media (max-width: 1024px) {
  .main-content {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .left-panel, .right-panel {
    display: none;
  }

  .main-title {
    font-size: 44px;
  }
}

/* 响应式 - 手机 */
@media (max-width: 640px) {
  .top-nav {
    padding: 16px 20px;
  }

  .logo-text {
    display: none;
  }

  .main-content {
    padding: 20px;
  }

  .main-title {
    font-size: 36px;
  }

  .tagline {
    font-size: 12px;
    letter-spacing: 2px;
  }

  .primary-action {
    padding: 16px 20px;
  }

  .action-icon {
    width: 40px;
    height: 40px;
  }

  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .bottom-bar {
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 16px 20px;
  }
}
</style>

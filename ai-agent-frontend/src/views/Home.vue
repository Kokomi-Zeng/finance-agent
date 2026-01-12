<template>
  <div class="home-container">
    <div class="header">
      <div class="glitch-wrapper">
        <h1 class="glitch-title">AI理财管理助手</h1>
      </div>
      <p class="subtitle">/ 智能理财 稳健增值 /</p>
      <div class="cyber-line"></div>
    </div>

    <div class="apps-container">
      <div class="app-card" @click="navigateTo('/agent')">
        <div class="card-glow"></div>
        <div class="app-icon agent-icon">📊</div>
        <div class="app-info">
          <div class="app-title">AI理财智能体</div>
          <div class="app-desc">整合专业知识库，自主分析市场，搜索最新资讯，生成专业投资报告</div>
        </div>
        <div class="app-button">
          <span class="btn-text">立即体验</span>
          <span class="btn-icon">→</span>
        </div>
      </div>
    </div>

    <div class="cyber-circles">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import AppFooter from '../components/AppFooter.vue'

// 设置页面标题和元数据
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
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;500;700&display=swap');

/* 金融主题样式变量 */
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
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background-color: var(--finance-primary);
  background-image:
    linear-gradient(0deg, rgba(27, 38, 59, 0.9), rgba(13, 27, 42, 0.95)),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><rect x="0" y="0" width="100" height="1" fill="%23415a77" opacity="0.2"/><rect x="0" y="0" width="1" height="100" fill="%23415a77" opacity="0.2"/></svg>');
  background-size: auto, 40px 40px;
  position: relative;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

/* 标题 */
.header {
  flex-shrink: 0;
  height: 18vh;
  padding: 2vh 0;
  text-align: center;
  background-color: transparent;
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.glitch-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 1vh;
}

.glitch-title {
  font-family: 'Orbitron', sans-serif;
  font-size: 3.2rem;
  font-weight: 700;
  color: var(--finance-light);
  text-shadow:
    0 0 5px rgba(203, 166, 89, 0.7),
    0 0 10px rgba(203, 166, 89, 0.5),
    0 0 20px rgba(203, 166, 89, 0.3);
  letter-spacing: 2px;
  position: relative;
  animation: glow 3s infinite;
}

.subtitle {
  font-family: 'Orbitron', sans-serif;
  font-size: 1.2rem;
  color: rgba(255, 255, 255, 0.7);
  max-width: 600px;
  margin: 0 auto 20px;
  letter-spacing: 3px;
  text-transform: uppercase;
}

.cyber-line {
  height: 2px;
  width: 80%;
  max-width: 600px;
  margin: 0 auto;
  background: linear-gradient(90deg, transparent, var(--finance-gold), transparent);
  position: relative;
}

.cyber-line::before,
.cyber-line::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 10px;
  height: 10px;
  background-color: var(--finance-gold);
  border-radius: 50%;
  transform: translateY(-50%);
  box-shadow: 0 0 10px 2px var(--finance-gold);
}

.cyber-line::before {
  left: 20%;
}

.cyber-line::after {
  right: 20%;
}

.apps-container {
  height: calc(100vh - 18vh - 80px);
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 0;
  width: 100%;
  padding: 0;
  position: relative;
  z-index: 2;
  overflow: hidden;
}

.app-card {
  width: 80%;
  height: 90%;
  max-width: 600px;
  margin: 0 auto;
  flex-shrink: 0;
  background-color: rgba(27, 38, 59, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow:
    0 8px 32px rgba(203, 166, 89, 0.15),
    inset 0 0 0 1px rgba(203, 166, 89, 0.2);
  padding: 5%;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle at center,
    rgba(203, 166, 89, 0.1) 0%,
    transparent 70%
  );
  opacity: 0;
  transition: opacity 0.5s;
  pointer-events: none;
}

.app-card:hover {
  transform: translateY(-15px) scale(1.03);
  box-shadow:
    0 15px 50px rgba(203, 166, 89, 0.25),
    inset 0 0 0 1px rgba(203, 166, 89, 0.5);
}

.app-card:hover .card-glow {
  opacity: 1;
}

.app-icon {
  font-size: 8vh;
  margin-bottom: 3%;
  width: 15vh;
  height: 15vh;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  position: relative;
  z-index: 1;
  flex-shrink: 0;
}

.agent-icon {
  background: linear-gradient(135deg, #1b263b, #415a77);
  box-shadow: 0 0 20px rgba(203, 166, 89, 0.4);
  border: 2px solid rgba(203, 166, 89, 0.3);
}

.app-info {
  text-align: center;
  flex: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.app-title {
  font-family: 'Orbitron', sans-serif;
  font-size: 3.5vh;
  font-weight: bold;
  color: white;
  margin-bottom: 2%;
  text-shadow: 0 0 10px rgba(203, 166, 89, 0.5);
}

.app-desc {
  font-size: 2vh;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

.app-button {
  background: linear-gradient(90deg, #1b263b, #415a77);
  color: var(--finance-gold);
  padding: 2vh 5%;
  border-radius: 30px;
  font-weight: 500;
  font-size: 2.5vh;
  transition: all 0.3s;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(203, 166, 89, 0.3);
}

.app-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(203, 166, 89, 0.2), transparent);
  transition: left 0.7s;
}

.app-button:hover {
  box-shadow: 0 0 15px rgba(203, 166, 89, 0.5);
  transform: scale(1.05);
  background: linear-gradient(90deg, #415a77, #778da9);
}

.app-button:hover::before {
  left: 100%;
}

.btn-text {
  margin-right: 8px;
  letter-spacing: 1px;
}

.btn-icon {
  font-size: 1.2rem;
  transition: transform 0.3s;
}

.app-button:hover .btn-icon {
  transform: translateX(4px);
}

/* 背景圆圈动画 */
.cyber-circles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 1;
}

.circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.15;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -100px;
  background: linear-gradient(135deg, var(--finance-accent), var(--finance-secondary));
  animation: float 15s infinite alternate;
}

.circle-2 {
  width: 500px;
  height: 500px;
  bottom: -200px;
  left: -200px;
  background: linear-gradient(135deg, var(--finance-secondary), var(--finance-gold));
  animation: float 20s infinite alternate-reverse;
}

.circle-3 {
  width: 200px;
  height: 200px;
  top: 40%;
  right: 15%;
  background: linear-gradient(135deg, var(--finance-gold), var(--finance-accent));
  animation: float 12s infinite alternate;
}

/* 动画效果 */
@keyframes float {
  0% {
    transform: translate(0, 0) rotate(0deg);
  }
  100% {
    transform: translate(50px, 50px) rotate(10deg);
  }
}

@keyframes glow {
  0% {
    text-shadow:
      0 0 5px rgba(203, 166, 89, 0.7),
      0 0 10px rgba(203, 166, 89, 0.5);
  }
  50% {
    text-shadow:
      0 0 5px rgba(203, 166, 89, 0.7),
      0 0 10px rgba(203, 166, 89, 0.5),
      0 0 20px rgba(203, 166, 89, 0.3);
  }
  100% {
    text-shadow:
      0 0 5px rgba(203, 166, 89, 0.7),
      0 0 10px rgba(203, 166, 89, 0.5);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .glitch-title {
    font-size: 2.5rem;
  }

  .subtitle {
    font-size: 1rem;
  }

  .apps-container {
    gap: 0;
    padding: 2vh 0;
  }

  .app-card {
    width: 80%;
    max-width: 500px;
    min-height: 450px;
    height: auto;
    padding: 6% 5%;
  }

  .app-icon {
    font-size: 3.5rem;
    width: 80px;
    height: 80px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 30px 15px 20px;
  }

  .glitch-title {
    font-size: 1.8rem;
  }

  .subtitle {
    font-size: 0.9rem;
    letter-spacing: 2px;
  }

  .apps-container {
    padding: 1vh 0;
  }

  .app-card {
    width: 80%;
    max-width: 400px;
    min-height: 400px;
    height: auto;
    padding: 5% 4%;
  }

  .app-icon {
    font-size: 3rem;
    margin-bottom: 20px;
    width: 70px;
    height: 70px;
  }

  .app-title {
    font-size: 1.4rem;
  }

  .app-desc {
    font-size: 0.9rem;
    line-height: 1.5;
  }

  .circle-1, .circle-2, .circle-3 {
    opacity: 0.1;
  }
}
</style>

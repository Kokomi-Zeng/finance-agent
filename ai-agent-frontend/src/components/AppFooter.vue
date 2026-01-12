<template>
  <footer class="app-footer">
    <div class="footer-content">
      <div class="footer-left">
        <span class="copyright">© {{ currentYear }} AI理财管理助手</span>
        <span class="divider">|</span>
        <span class="risk-tip">投资有风险，入市需谨慎</span>
      </div>
      <div class="footer-right">
        <a href="#" @click.prevent="showUserAgreement = true" class="footer-link">用户协议</a>
        <span class="divider">|</span>
        <a href="#" @click.prevent="showPrivacyPolicy = true" class="footer-link">隐私政策</a>
      </div>
    </div>

    <!-- 用户协议弹窗 - 使用 Teleport 传送到 body -->
    <Teleport to="body">
      <div v-if="showUserAgreement" class="modal-overlay" @click.self="showUserAgreement = false">
        <div class="modal-content">
          <div class="modal-header">
            <h2>用户协议</h2>
            <button class="close-btn" @click="showUserAgreement = false">×</button>
          </div>
          <div class="modal-body">
            <h3>一、服务条款</h3>
            <p>欢迎使用AI理财管理助手（以下简称"本平台"）。在使用本平台服务之前，请您仔细阅读并充分理解本协议各条款。</p>

            <h3>二、服务内容</h3>
            <p>本平台提供基于人工智能技术的理财信息分析服务，包括但不限于：</p>
            <ul>
              <li>市场信息搜索与分析</li>
              <li>投资组合分析建议</li>
              <li>理财知识问答</li>
              <li>研究报告生成</li>
            </ul>

            <h3>三、免责声明</h3>
            <p>本平台提供的所有信息和建议仅供参考，不构成任何投资建议或承诺。用户应根据自身情况独立判断，自主决策，自担风险。本平台不对用户的投资损益承担任何责任。</p>

            <h3>四、用户责任</h3>
            <p>用户应当：</p>
            <ul>
              <li>如实提供个人信息</li>
              <li>遵守国家法律法规</li>
              <li>不得利用本平台从事违法活动</li>
              <li>妥善保管账户信息</li>
            </ul>

            <h3>五、知识产权</h3>
            <p>本平台的所有内容，包括但不限于文字、图片、代码、界面设计等，均受知识产权法律保护。未经授权，禁止复制、传播或用于商业用途。</p>

            <h3>六、协议修改</h3>
            <p>本平台有权根据需要修改本协议，修改后的协议将在平台上公布。如您继续使用本平台服务，视为您已接受修改后的协议。</p>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 隐私政策弹窗 - 使用 Teleport 传送到 body -->
    <Teleport to="body">
      <div v-if="showPrivacyPolicy" class="modal-overlay" @click.self="showPrivacyPolicy = false">
        <div class="modal-content">
          <div class="modal-header">
            <h2>隐私政策</h2>
            <button class="close-btn" @click="showPrivacyPolicy = false">×</button>
          </div>
          <div class="modal-body">
            <h3>一、信息收集</h3>
            <p>为提供更好的服务，我们可能收集以下信息：</p>
            <ul>
              <li>设备信息：浏览器类型、操作系统等</li>
              <li>使用数据：功能使用情况、访问时间等</li>
              <li>对话内容：您与AI助手的对话记录</li>
            </ul>

            <h3>二、信息使用</h3>
            <p>我们收集的信息将用于：</p>
            <ul>
              <li>提供和改进服务质量</li>
              <li>个性化用户体验</li>
              <li>安全防护和故障排查</li>
              <li>数据分析和研究</li>
            </ul>

            <h3>三、信息保护</h3>
            <p>我们采取严格的安全措施保护您的信息：</p>
            <ul>
              <li>数据加密传输和存储</li>
              <li>访问权限控制</li>
              <li>定期安全审计</li>
              <li>员工保密培训</li>
            </ul>

            <h3>四、信息共享</h3>
            <p>我们不会向第三方出售、出租或交换您的个人信息。除非：</p>
            <ul>
              <li>获得您的明确同意</li>
              <li>法律法规要求</li>
              <li>保护本平台和用户的合法权益</li>
            </ul>

            <h3>五、Cookie使用</h3>
            <p>我们使用Cookie和类似技术来提升用户体验、分析流量和提供个性化服务。您可以通过浏览器设置管理Cookie。</p>

            <h3>六、用户权利</h3>
            <p>您有权：</p>
            <ul>
              <li>查询、更正您的个人信息</li>
              <li>删除您的对话记录</li>
              <li>撤回授权同意</li>
              <li>注销账户</li>
            </ul>

            <h3>七、未成年人保护</h3>
            <p>本平台主要面向成年用户。如果您是未成年人，请在监护人指导下使用本服务。</p>

            <h3>八、政策更新</h3>
            <p>我们可能会更新本隐私政策。重大变更将在平台上通知。继续使用服务即表示您同意更新后的政策。</p>
          </div>
        </div>
      </div>
    </Teleport>
  </footer>
</template>

<script setup>
import { ref, computed } from 'vue'

// 计算当前年份
const currentYear = computed(() => new Date().getFullYear())

// 弹窗控制
const showUserAgreement = ref(false)
const showPrivacyPolicy = ref(false)
</script>

<style scoped>
.app-footer {
  background: linear-gradient(135deg, #0d1b2a 0%, #1b263b 100%);
  height: 80px;
  color: #94a3b8;
  border-top: 1px solid rgba(203, 166, 89, 0.3);
  width: 100%;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 2;
}

.footer-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 0.85rem;
}

.copyright {
  color: #94a3b8;
}

.risk-tip {
  color: #64748b;
  font-style: italic;
}

.divider {
  color: #64748b;
}

.footer-link {
  color: #94a3b8;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-link:hover {
  color: #cba659;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(13, 27, 42, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 20px;
  box-sizing: border-box;
}

.modal-content {
  background: linear-gradient(135deg, #1b263b 0%, #0d1b2a 100%);
  border-radius: 16px;
  max-width: 700px;
  width: 100%;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(203, 166, 89, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(203, 166, 89, 0.2);
  background: rgba(203, 166, 89, 0.1);
}

.modal-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #cba659;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #94a3b8;
  padding: 0;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #cba659;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  max-height: calc(80vh - 80px);
  color: #e2e8f0;
}

.modal-body h3 {
  color: #cba659;
  font-size: 1.1rem;
  margin: 20px 0 12px;
  font-weight: 500;
}

.modal-body h3:first-child {
  margin-top: 0;
}

.modal-body p {
  line-height: 1.7;
  margin-bottom: 12px;
  color: #94a3b8;
}

.modal-body ul {
  margin: 0 0 12px 20px;
  padding: 0;
}

.modal-body li {
  line-height: 1.7;
  margin-bottom: 6px;
  color: #94a3b8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .footer-content {
    flex-direction: column;
    gap: 8px;
    padding: 0 16px;
  }

  .footer-left,
  .footer-right {
    font-size: 0.75rem;
    gap: 8px;
  }

  .modal-content {
    max-height: 90vh;
  }

  .modal-body {
    max-height: calc(90vh - 80px);
  }
}

@media (max-width: 480px) {
  .footer-content {
    padding: 0 12px;
  }

  .footer-left,
  .footer-right {
    font-size: 0.7rem;
    gap: 6px;
  }

  .risk-tip {
    display: none;
  }

  .modal-header {
    padding: 16px 20px;
  }

  .modal-header h2 {
    font-size: 1.25rem;
  }

  .modal-body {
    padding: 20px;
  }

  .modal-body h3 {
    font-size: 1rem;
  }
}
</style>

<template>
  <div class="home-container">
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">AI 知识图谱平台</h1>
        <p class="hero-subtitle">智能构建、可视化管理、AI辅助的知识图谱解决方案</p>
        <div class="hero-buttons">
          <el-button type="primary" size="large" @click="goToLogin">
            立即开始
          </el-button>
          <el-button size="large" @click="learnMore">
            了解更多
          </el-button>
        </div>
      </div>
      <div class="hero-image">
        <img src="../assets/hero.png" alt="知识图谱" />
      </div>
    </div>

    <div class="features-section">
      <div class="features-container">
        <h2 class="section-title">核心功能</h2>
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon">📊</div>
            <h3 class="feature-title">知识图谱广场</h3>
            <p class="feature-desc">浏览和发现公共知识图谱，获取灵感并学习最佳实践</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🔗</div>
            <h3 class="feature-title">图谱创建</h3>
            <p class="feature-desc">支持手动输入节点和关系，或通过Excel批量导入快速构建</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🤖</div>
            <h3 class="feature-title">AI智能辅助</h3>
            <p class="feature-desc">通过AI对话智能生成知识图谱，支持自然语言描述</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">📈</div>
            <h3 class="feature-title">可视化展示</h3>
            <p class="feature-desc">基于ECharts的交互式图谱可视化，支持缩放和拖拽</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">📤</div>
            <h3 class="feature-title">Excel导入导出</h3>
            <p class="feature-desc">支持将知识图谱导出为Excel表格，或从Excel导入构建图谱</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">👥</div>
            <h3 class="feature-title">个人与公共</h3>
            <p class="feature-desc">管理个人知识图谱，同时可分享到公共广场</p>
          </div>
        </div>
      </div>
    </div>

    <div class="how-it-works">
      <div class="steps-container">
        <h2 class="section-title">使用流程</h2>
        <div class="steps">
          <div class="step">
            <div class="step-number">1</div>
            <h3 class="step-title">登录账号</h3>
            <p class="step-desc">创建账户并登录系统</p>
          </div>
          <div class="step">
            <div class="step-number">2</div>
            <h3 class="step-title">创建图谱</h3>
            <p class="step-desc">手动输入或使用AI辅助创建</p>
          </div>
          <div class="step">
            <div class="step-number">3</div>
            <h3 class="step-title">可视化编辑</h3>
            <p class="step-desc">在可视化界面中完善图谱</p>
          </div>
          <div class="step">
            <div class="step-number">4</div>
            <h3 class="step-title">分享导出</h3>
            <p class="step-desc">分享到广场或导出为Excel</p>
          </div>
        </div>
      </div>
    </div>

    <footer class="footer">
      <p>&copy; 2024 AI 知识图谱平台. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { graphApi } from '@/api/graph'

const router = useRouter()
const hotGraphs = ref([])

onMounted(async () => {
  await loadHotGraphs()
})

const loadHotGraphs = async () => {
  try {
    const response = await graphApi.getTopHotGraphs()
    hotGraphs.value = response.data
  } catch (error) {
    console.error('加载热门图谱失败:', error)
  }
}

const goToLogin = () => {
  router.push('/login')
}

const learnMore = () => {
  document.querySelector('.features-section').scrollIntoView({ behavior: 'smooth' })
}

const viewGraph = (graphId) => {
  router.push(`/graph/${graphId}`)
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  font-family: 'Poppins', 'Noto Sans SC', sans-serif;
  position: relative;
  overflow-x: hidden;
}

.home-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.05) 0%, transparent 70%);
  animation: float 30s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(50px, 50px) rotate(90deg);
  }
  50% {
    transform: translate(0, 100px) rotate(180deg);
  }
  75% {
    transform: translate(-50px, 50px) rotate(270deg);
  }
}

.hero-section {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 85vh;
  padding: 40px 20px;
  color: white;
  position: relative;
  z-index: 1;
}

.hero-content {
  flex: 1;
  max-width: 650px;
  animation: slideInLeft 1s ease-out;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-50px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.hero-title {
  font-size: 4rem;
  font-weight: 800;
  margin-bottom: 25px;
  line-height: 1.1;
  letter-spacing: -1px;
  background: linear-gradient(135deg, #ffffff 0%, #f0f0f0 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: titleGlow 3s ease-in-out infinite;
}

@keyframes titleGlow {
  0%, 100% {
    filter: drop-shadow(0 0 20px rgba(255, 255, 255, 0.3));
  }
  50% {
    filter: drop-shadow(0 0 30px rgba(255, 255, 255, 0.5));
  }
}

.hero-subtitle {
  font-size: 1.4rem;
  font-weight: 300;
  margin-bottom: 45px;
  opacity: 0.95;
  line-height: 1.7;
}

.hero-buttons {
  display: flex;
  gap: 20px;
  padding-left: 37px;
}

.hero-buttons :deep(.el-button) {
  font-family: 'Poppins', 'Noto Sans SC', sans-serif;
  font-weight: 600;
  padding: 15px 35px;
  font-size: 1.05rem;
  border-radius: 30px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.hero-buttons :deep(.el-button--primary) {
  background: linear-gradient(135deg, #ffffff 0%, #f0f0f0 100%);
  color: #667eea;
  border: none;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.hero-buttons :deep(.el-button--primary:hover) {
  transform: translateY(-3px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.3);
}

.hero-buttons :deep(.el-button--default) {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
}

.hero-buttons :deep(.el-button--default:hover) {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.5);
  transform: translateY(-3px);
}

.hero-image {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  animation: slideInRight 1s ease-out 0.3s both;
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(50px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.hero-image img {
  max-width: 100%;
  height: auto;
  border-radius: 24px;
  box-shadow: 0 25px 70px rgba(0, 0, 0, 0.4);
  animation: floatImage 6s ease-in-out infinite;
}

@keyframes floatImage {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(2deg);
  }
}

.features-section {
  background: white;
  padding: 100px 20px;
  position: relative;
  z-index: 1;
}

.features-container {
  max-width: 1300px;
  margin: 0 auto;
}

.section-title {
  text-align: center;
  font-size: 2.8rem;
  font-weight: 700;
  margin-bottom: 70px;
  color: #1a1a2e;
  position: relative;
  display: inline-block;
  width: 100%;
}

.section-title::after {
  content: '';
  display: block;
  width: 80px;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  margin: 20px auto 0;
  border-radius: 2px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 35px;
}

.feature-card {
  padding: 45px 35px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f8f9fa 100%);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #e8ecf4;
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.05), transparent);
  transition: left 0.5s;
}

.feature-card:hover::before {
  left: 100%;
}

.feature-card:hover {
  transform: translateY(-12px);
  box-shadow: 0 20px 50px rgba(102, 126, 234, 0.15);
  border-color: #667eea;
}

.feature-icon {
  font-size: 3.5rem;
  margin-bottom: 25px;
  display: inline-block;
  animation: iconBounce 2s ease-in-out infinite;
}

@keyframes iconBounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.feature-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 15px;
  font-family: 'Poppins', 'Noto Sans SC', sans-serif;
}

.feature-desc {
  color: #64748b;
  font-size: 1rem;
  line-height: 1.7;
  font-weight: 300;
}

.feature-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 15px;
  font-family: 'Poppins', 'Noto Sans SC', sans-serif;
}

.feature-desc {
  color: #64748b;
  font-size: 1rem;
  line-height: 1.7;
  font-weight: 300;
}

.how-it-works {
  background: linear-gradient(180deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 100px 20px;
  position: relative;
}

.how-it-works::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 50%, rgba(102, 126, 234, 0.03) 0%, transparent 50%);
}

.steps-container {
  max-width: 1100px;
  margin: 0 auto;
}

.steps {
  display: flex;
  justify-content: space-between;
  gap: 30px;
  flex-wrap: wrap;
}

.step {
  flex: 1;
  min-width: 220px;
  text-align: center;
  padding: 35px 25px;
  background: white;
  border-radius: 20px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #e8ecf4;
  position: relative;
  overflow: hidden;
}

.step::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.03) 0%, transparent 70%);
  animation: rotate 15s linear infinite;
}

.step:hover {
  transform: translateY(-10px);
  box-shadow: 0 15px 40px rgba(102, 126, 234, 0.12);
  border-color: #667eea;
}

.step-number {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 1.8rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
  position: relative;
  z-index: 1;
}

.step-title {
  font-size: 1.3rem;
  font-weight: 700;
  margin-bottom: 12px;
  color: #1a1a2e;
  font-family: 'Poppins', 'Noto Sans SC', sans-serif;
}

.step-desc {
  color: #64748b;
  font-size: 0.95rem;
  line-height: 1.6;
  font-weight: 300;
}

.footer {
  background: linear-gradient(135deg, #1a1a2e 0%, #2d2d44 100%);
  color: white;
  text-align: center;
  padding: 40px 20px;
  position: relative;
  overflow: hidden;
}

.footer::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.1) 0%, transparent 60%);
  animation: rotate 20s linear infinite;
}

.footer p {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 300;
  position: relative;
  z-index: 1;
}

@media (max-width: 768px) {
  .hero-section {
    flex-direction: column;
    text-align: center;
  }

  .hero-title {
    font-size: 2.5rem;
  }

  .hero-buttons {
    justify-content: center;
  }

  .steps {
    flex-direction: column;
  }
  
  .step {
    min-width: 100%;
  }
}
</style>
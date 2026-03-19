<template>
  <div class="ai-chat-container">
    <div class="chat-header">
      <h1 class="page-title">AI 助手</h1>
    </div>

    <div class="chat-content">
      <div class="chat-sidebar">
        <el-button type="primary" @click="startNewChat" style="width: 100%; margin-bottom: 20px">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>

        <div class="chat-history">
          <div
            v-for="(chat, index) in chatHistory"
            :key="index"
            class="history-item"
            :class="{ active: currentChatIndex === index }"
            @click="selectChat(index)"
          >
            <div class="history-title">{{ chat.title }}</div>
            <div class="history-time">{{ formatTime(chat.timestamp) }}</div>
          </div>
        </div>

        <el-button 
          type="danger" 
          plain 
          @click="clearHistory" 
          style="width: 100%; margin-top: 20px"
        >
          <el-icon><Delete /></el-icon>
          清除历史
        </el-button>
      </div>

      <div class="chat-main">
        <div class="messages-container" ref="messagesContainer">
          <div
            v-for="(message, index) in currentMessages"
            :key="`${message.role}-${index}-${updateCounter}`"
            class="message-wrapper"
            :class="message.role"
          >
            <div class="message-avatar">
              <el-avatar :size="40">
                {{ message.role === 'user' ? '👤' : '🤖' }}
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="message-text" v-html="renderMarkdown(message.content)"></div>
              <div v-if="message.graphPreview" class="message-graph-preview">
                <div class="graph-preview-card">
                  <div class="graph-preview-header">
                    <h4>知识图谱预览</h4>
                    <el-tag type="info" size="small">{{ message.graphPreview.nodes.length }} 节点 / {{ message.graphPreview.edges.length }} 关系</el-tag>
                  </div>
                  <div class="graph-preview-visual" ref="graphPreviewChart"></div>
                  <div class="graph-preview-actions">
                    <el-button type="success" @click="confirmCreateGraph(message.graphPreview)">
                      <el-icon><Check /></el-icon>
                      确认创建
                    </el-button>
                    <el-button type="danger" @click="cancelCreateGraph(message)">
                      <el-icon><Close /></el-icon>
                      取消
                    </el-button>
                  </div>
                </div>
              </div>
              <div v-if="message.graph" class="message-graph">
                <div class="graph-preview-mini">
                  <div class="graph-info">
                    <h4>{{ message.graph.name }}</h4>
                    <p>{{ message.graph.description }}</p>
                    <div class="graph-stats">
                      <span>节点: {{ message.graph.nodes.length }}</span>
                      <span>关系: {{ message.graph.edges.length }}</span>
                    </div>
                  </div>
                  <el-button type="primary" size="small" @click="viewGraph(message.graph)">
                    查看图谱
                  </el-button>
                </div>
              </div>
              <div v-if="message.loading" class="message-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                AI正在思考...
              </div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <div class="input-toolbar">
            <el-upload
              :auto-upload="false"
              :on-change="handleFileUpload"
              :show-file-list="false"
              accept=".txt,.doc,.docx"
            >
              <el-button text>
                <el-icon><Document /></el-icon>
                上传文档
              </el-button>
            </el-upload>
            <el-button text @click="insertTemplate">
              <el-icon><MagicStick /></el-icon>
              插入模板
            </el-button>
          </div>
          
          <div class="input-wrapper">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="4"
              placeholder="输入您的知识描述，例如：'创建一个关于编程语言的知识图谱，包括JavaScript、Python、Java等语言，以及它们之间的关系'..."
              @keydown.enter.prevent="handleEnter"
            />
            <el-button
              type="primary"
              :loading="loading"
              :disabled="!inputMessage.trim()"
              @click="sendMessage"
              class="send-button"
            >
              发送
            </el-button>
          </div>

          <div v-if="uploadedFile" class="uploaded-file">
            <el-icon><Document /></el-icon>
            <span>{{ uploadedFile.name }}</span>
            <el-button text @click="removeFile">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, triggerRef } from 'vue'
import { useRouter } from 'vue-router'
import { useGraphStore } from '../stores/graph'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { Plus, Loading, Document, MagicStick, Close, Delete, Check } from '@element-plus/icons-vue'
import { aiApi } from '../api/ai'
import { graphApi } from '../api/graph'
import { marked } from 'marked'
import * as echarts from 'echarts'

const router = useRouter()
const graphStore = useGraphStore()
const userStore = useUserStore()

const messagesContainer = ref(null)
const loading = ref(false)
const inputMessage = ref('')
const uploadedFile = ref(null)
const currentChatIndex = ref(0)
const updateCounter = ref(0)
const chatHistory = reactive([
  {
    title: '新建对话',
    timestamp: new Date(),
    messages: [
      {
        role: 'assistant',
        content: '你好！我是AI知识图谱助手。你可以通过自然语言描述来创建知识图谱，也可以上传文档让我帮你提取知识。'
      }
    ]
  }
])

const renderMarkdown = (content) => {
  if (!content) return ''
  try {
    return marked(content)
  } catch (error) {
    console.error('Markdown渲染失败:', error)
    return content
  }
}

const currentMessages = computed(() => {
  const messages = chatHistory[currentChatIndex.value]?.messages || []
  console.log('computed currentMessages被调用，返回消息数量:', messages.length, 'updateCounter:', updateCounter.value)
  updateCounter.value
  return messages
})

const startNewChat = () => {
  chatHistory.unshift({
    title: '新建对话',
    timestamp: new Date(),
    messages: [
      {
        role: 'assistant',
        content: '你好！我是AI知识图谱助手。你可以通过自然语言描述来创建知识图谱，也可以上传文档让我帮你提取知识。'
      }
    ]
  })
  currentChatIndex.value = 0
  scrollToBottom()
}

const selectChat = (index) => {
  currentChatIndex.value = index
  scrollToBottom()
}

const handleFileUpload = (file) => {
  uploadedFile.value = file
  ElMessage.success('文档上传成功')
}

const removeFile = () => {
  uploadedFile.value = null
}

const insertTemplate = () => {
  inputMessage.value = `创建一个关于${getRandomTopic()}的知识图谱，包括主要的实体和它们之间的关系。`
}

const getRandomTopic = () => {
  const topics = [
    '编程语言',
    '机器学习算法',
    '数据库技术',
    '前端框架',
    '云服务提供商',
    '操作系统'
  ]
  return topics[Math.floor(Math.random() * topics.length)]
}

const handleEnter = (e) => {
  if (!e.shiftKey) {
    sendMessage()
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim()) return

  const userMessage = inputMessage.value.trim()
  const fileContext = uploadedFile.value ? `\n\n[已上传文档: ${uploadedFile.value.name}]` : ''

  chatHistory[currentChatIndex.value].messages.push({
    role: 'user',
    content: userMessage + fileContext
  })

  inputMessage.value = ''
  uploadedFile.value = null
  scrollToBottom()

  loading.value = true
  const aiMessageIndex = currentMessages.value.length
  chatHistory[currentChatIndex.value].messages.push({
    role: 'assistant',
    content: '',
    loading: true
  })

  try {
    await aiApi.chatStream(
      {
        userId: userStore.userId || 1,
        message: userMessage
      },
      (event) => {
        const data = event.data
        if (data && data.trim()) {
          const currentMessages = chatHistory[currentChatIndex.value].messages
          const message = currentMessages[aiMessageIndex]
          message.content += data
          message.loading = false
          currentMessages.splice(aiMessageIndex, 1, { ...message })
          updateCounter.value++
          nextTick(() => {
            scrollToBottom()
          })
        }
      },
      () => {
        const currentMessages = chatHistory[currentChatIndex.value].messages
        const message = currentMessages[aiMessageIndex]
        message.loading = false
        currentMessages.splice(aiMessageIndex, 1, message)
        updateCounter.value++
        loading.value = false
        
        if (currentChatIndex.value === 0) {
          chatHistory[0].title = userMessage.substring(0, 20) + '...'
        }
        
        loadChatHistory()
      },
      (error) => {
        console.error('流式响应错误:', error)
        const currentMessages = chatHistory[currentChatIndex.value].messages
        const message = currentMessages[aiMessageIndex]
        message.loading = false
        message.content = '抱歉，AI响应失败，请稍后重试。'
        currentMessages.splice(aiMessageIndex, 1, message)
        updateCounter.value++
        loading.value = false
        ElMessage.error('AI响应失败，请检查网络连接')
      }
    )
  } catch (error) {
    console.error('AI响应失败:', error)
    const currentMessages = chatHistory[currentChatIndex.value].messages
    const message = currentMessages[aiMessageIndex]
    message.loading = false
    message.content = '抱歉，AI响应失败，请稍后重试。'
    updateCounter.value++
    loading.value = false
    ElMessage.error('AI响应失败，请检查网络连接')
  }
}

const viewGraph = (graph) => {
  router.push(`/layout/graph/${graph.id}`)
}

const parseGraphData = (content) => {
  try {
    const jsonMatch = content.match(/\{[\s\S]*\}/)
    if (jsonMatch) {
      const graphData = JSON.parse(jsonMatch[0])
      if (graphData.nodes && graphData.edges) {
        return {
          nodes: graphData.nodes.map(node => ({
            id: node.id,
            name: node.name,
            category: node.category
          })),
          edges: graphData.edges.map(edge => ({
            source: edge.source,
            label: edge.label,
            target: edge.target
          }))
        }
      }
    }
  } catch (error) {
    console.error('解析图谱数据失败:', error)
  }
  return null
}

const renderGraphPreview = (graphData, messageIndex) => {
  nextTick(() => {
    const chartElements = document.querySelectorAll('.graph-preview-visual')
    if (chartElements.length > messageIndex) {
      const chartElement = chartElements[messageIndex]
      if (chartElement) {
        const chartInstance = echarts.init(chartElement)
        
        const nodes = graphData.nodes.map(node => ({
          id: node.id,
          name: node.name,
          category: node.category,
          symbolSize: 30,
          itemStyle: {
            color: getNodeColor(node.category)
          }
        }))
        
        const edges = graphData.edges.map(edge => ({
          source: edge.source,
          target: edge.target,
          label: {
            show: true,
            formatter: edge.label
          }
        }))
        
        const option = {
          tooltip: {},
          series: [{
            type: 'graph',
            layout: 'force',
            data: nodes,
            links: edges,
            roam: true,
            label: {
              show: true,
              position: 'right',
              formatter: '{b}'
            },
            force: {
              repulsion: 100,
              edgeLength: 100
            }
          }]
        }
        
        chartInstance.setOption(option)
        
        window.addEventListener('resize', () => {
          chartInstance.resize()
        })
      }
    }
  })
}

const getNodeColor = (category) => {
  const colors = {
    '人物': '#FF6B6B',
    '地点': '#4ECDC4',
    '事件': '#45B7D1',
    '概念': '#96CEB4',
    '组织': '#FFEAA7',
    'default': '#DDA0DD'
  }
  return colors[category] || colors['default']
}

const confirmCreateGraph = async (graphData) => {
  if (!userStore.userId) {
    ElMessage.error('请先登录')
    return
  }
  
  try {
    const graphName = `AI生成图谱-${new Date().toLocaleString()}`
    const graphDescription = '通过AI对话自动生成的知识图谱'
    
    const response = await graphApi.createGraph({
      name: graphName,
      description: graphDescription,
      categoryId: 1,
      ownerId: userStore.userId,
      isPublic: 0
    })
    
    const graphId = response.data.id
    
    for (const node of graphData.nodes) {
      await graphApi.createNode({
        graphId: graphId,
        nodeId: node.id,
        name: node.name,
        category: node.category
      })
    }
    
    for (const edge of graphData.edges) {
      await graphApi.createEdge({
        graphId: graphId,
        sourceNodeId: edge.source,
        targetNodeId: edge.target,
        relation: edge.label
      })
    }
    
    ElMessage.success('知识图谱创建成功')
    router.push(`/layout/graph/${graphId}`)
  } catch (error) {
    console.error('创建图谱失败:', error)
    ElMessage.error('创建图谱失败：' + (error.message || '未知错误'))
  }
}

const cancelCreateGraph = (message) => {
  message.graphPreview = null
  message.content = '已取消创建知识图谱。'
  updateCounter.value++
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const formatTime = (date) => {
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  return `${days}天前`
}

const loadChatHistory = async () => {
  if (!userStore.userId) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    const response = await aiApi.getHistory(userStore.userId)
    console.log('加载历史记录响应:', response)
    if (response.code === 200 && response.data && response.data.length > 0) {
      const historyData = response.data
      
      if (historyData.length > 0) {
        chatHistory[0].messages = historyData.map(msg => ({
          role: msg.role,
          content: msg.content
        }))
        
        if (chatHistory[0].messages.length > 1) {
          const firstUserMessage = chatHistory[0].messages.find(m => m.role === 'user')
          if (firstUserMessage) {
            chatHistory[0].title = firstUserMessage.content.substring(0, 20) + '...'
          }
        }
        
        updateCounter.value++
        console.log('历史记录加载完成，消息数量:', chatHistory[0].messages.length)
      }
    }
  } catch (error) {
    console.error('加载聊天历史失败:', error)
  }
}

const clearHistory = async () => {
  if (!userStore.userId) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    await aiApi.clearHistory(userStore.userId)
    
    chatHistory[0].messages = [
      {
        role: 'assistant',
        content: '你好！我是AI知识图谱助手。你可以通过自然语言描述来创建知识图谱，也可以上传文档让我帮你提取知识。'
      }
    ]
    chatHistory[0].title = '新建对话'
    
    currentMessages.value = [...chatHistory[0].messages]
    
    ElMessage.success('聊天历史已清除')
  } catch (error) {
    console.error('清除聊天历史失败:', error)
    ElMessage.error('清除聊天历史失败')
  }
}

onMounted(() => {
  loadChatHistory()
  scrollToBottom()
})
</script>

<style scoped>
.ai-chat-container {
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}

.chat-header {
  margin-bottom: 10px;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.chat-content {
  display: flex;
  flex: 1;
  gap: 20px;
  overflow: hidden;
}

.chat-sidebar {
  width: 280px;
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow-y: auto;
}

.chat-history {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item {
  padding: 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.history-item:hover {
  background: #f5f7fa;
}

.history-item.active {
  background: #f0f7ff;
  border-color: #667eea;
}

.history-title {
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-time {
  font-size: 0.8rem;
  color: #999;
}

.chat-main {
  flex: 1;
  background: white;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-wrapper {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.message-wrapper.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.message-text {
  padding: 15px 20px;
  border-radius: 12px;
  line-height: 1.6;
  word-wrap: break-word;
}

.message-wrapper.user .message-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.message-wrapper.assistant .message-text {
  background: #f5f7fa;
  color: #333;
}

.message-text {
  font-size: 14px;
}

.message-text h1,
.message-text h2,
.message-text h3,
.message-text h4,
.message-text h5,
.message-text h6 {
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
  color: #333;
}

.message-text h1 {
  font-size: 1.8em;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 0.3em;
}

.message-text h2 {
  font-size: 1.5em;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 0.3em;
}

.message-text h3 {
  font-size: 1.3em;
}

.message-text p {
  margin: 0.5em 0;
  line-height: 1.6;
}

.message-text ul,
.message-text ol {
  margin: 0.5em 0;
  padding-left: 2em;
}

.message-text li {
  margin: 0.3em 0;
}

.message-text code {
  background: rgba(0, 0, 0, 0.05);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.message-text pre {
  background: #f5f5f5;
  padding: 1em;
  border-radius: 6px;
  overflow-x: auto;
  margin: 1em 0;
}

.message-text pre code {
  background: none;
  padding: 0;
  border-radius: 0;
}

.message-text blockquote {
  border-left: 4px solid #667eea;
  padding-left: 1em;
  margin: 1em 0;
  color: #666;
  font-style: italic;
}

.message-text a {
  color: #667eea;
  text-decoration: none;
}

.message-text a:hover {
  text-decoration: underline;
}

.message-text table {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
}

.message-text th,
.message-text td {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.message-text th {
  background: #f5f7fa;
  font-weight: 600;
}

.message-text tr:nth-child(even) {
  background: #f9f9f9;
}

.message-text img {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  margin: 1em 0;
}

.message-text hr {
  border: none;
  border-top: 2px solid #e0e0e0;
  margin: 2em 0;
}

.message-graph {
  margin-top: 15px;
}

.graph-preview-mini {
  background: white;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.graph-info h4 {
  margin: 0 0 10px 0;
  color: #333;
}

.graph-info p {
  margin: 0 0 10px 0;
  color: #666;
  font-size: 0.9rem;
}

.graph-stats {
  display: flex;
  gap: 15px;
  font-size: 0.85rem;
  color: #999;
}

.message-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px 20px;
  background: #f5f7fa;
  border-radius: 12px;
  color: #666;
}

.input-area {
  border-top: 1px solid #e4e7ed;
  padding: 20px;
}

.input-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.input-wrapper {
  position: relative;
}

:deep(.el-textarea__inner) {
  resize: none;
  padding-right: 80px;
}

.send-button {
  position: absolute;
  right: 10px;
  bottom: 10px;
}

.message-graph-preview {
  margin-top: 15px;
}

.graph-preview-card {
  background: white;
  border: 2px solid #667eea;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.graph-preview-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.graph-preview-header h4 {
  margin: 0;
  color: white;
  font-size: 1.1rem;
}

.graph-preview-visual {
  height: 400px;
  background: #f8f9fa;
  border-top: 1px solid #e4e7ed;
  border-bottom: 1px solid #e4e7ed;
}

.graph-preview-actions {
  padding: 15px 20px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  background: #f5f7fa;
}

.uploaded-file {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 15px;
  background: #f0f7ff;
  border-radius: 8px;
  margin-top: 10px;
  color: #667eea;
  font-size: 0.9rem;
}

:deep(.el-button--text) {
  color: #667eea;
}

:deep(.el-button--text:hover) {
  color: #764ba2;
}
</style>
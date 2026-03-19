<template>
  <div class="graph-detail-container">
    <div class="graph-header">
      <div class="header-left">
        <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
        <div class="title-section">
          <h1 class="graph-title">{{ graph?.name }}</h1>
          <p class="graph-description">{{ graph?.description }}</p>
        </div>
      </div>
      <div class="header-actions">
        <el-button @click="exportToExcel" :icon="Download">
          导出Excel
        </el-button>
        <el-button v-if="isOwner && !isEditMode" type="primary" @click="showEditGraphDialog" :icon="Edit">
          编辑图谱
        </el-button>
        <el-button v-if="isOwner && isEditMode" type="success" @click="toggleEditMode" :icon="Check">
          完成编辑
        </el-button>
        <el-button v-if="isOwner && !isEditMode" type="primary" @click="toggleEditMode" :icon="Edit">
          编辑模式
        </el-button>
      </div>
    </div>

    <div class="graph-content">
        <div v-loading="loading" class="graph-visual">
          <el-card class="graph-card">
            <template #header>
              <div class="card-header">
                <span>知识图谱可视化</span>
                <div class="graph-controls">
                  <el-button-group>
                    <el-button size="small" @click="zoomIn">
                      <el-icon><ZoomIn /></el-icon>
                    </el-button>
                    <el-button size="small" @click="zoomOut">
                      <el-icon><ZoomOut /></el-icon>
                    </el-button>
                    <el-button size="small" @click="resetZoom">
                      <el-icon><RefreshRight /></el-icon>
                    </el-button>
                  </el-button-group>
                </div>
              </div>
            </template>
            <div ref="graphChart" class="graph-chart"></div>
            
            <div class="comment-section">
              <div class="comment-header">
                <h3>评论 ({{ comments.length }})</h3>
              </div>
              
              <div class="comment-input-area">
                <el-input
                  v-model="newComment"
                  type="textarea"
                  :rows="3"
                  placeholder="发表你的评论..."
                />
                <el-button 
                  type="primary" 
                  @click="handleAddComment" 
                  :loading="commentLoading"
                  style="margin-top: 10px"
                >
                  发表评论
                </el-button>
              </div>
              
              <div class="comment-list">
                <div v-if="comments.length === 0" class="no-comment">
                  暂无评论，快来发表第一条评论吧！
                </div>
                <div v-for="comment in comments" :key="comment.id" class="comment-item">
                  <div class="comment-header">
                    <span class="comment-user">用户{{ comment.userId }}</span>
                    <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                    <el-button 
                      v-if="comment.userId === userStore.user?.id"
                      type="danger" 
                      size="small" 
                      link
                      @click="handleDeleteComment(comment.id)"
                    >
                      删除
                    </el-button>
                  </div>
                  <div class="comment-content">{{ comment.content }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </div>

        <div class="graph-info">
          <el-card class="info-card">
            <template #header>
              <span>图谱信息</span>
            </template>
            <div class="info-content">
              <div class="info-item">
                <span class="info-label">创建时间：</span>
                <span class="info-value">{{ formatDate(graph?.createdAt) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">浏览量：</span>
                <el-tag type="info">{{ graph?.viewCount || 0 }}</el-tag>
              </div>
              <div class="info-item">
                <span class="info-label">点赞数：</span>
                <el-tag type="danger">{{ graph?.likeCount || 0 }}</el-tag>
              </div>
              <div class="info-item">
                <span class="info-label">评论数：</span>
                <el-tag type="warning">{{ graph?.commentCount || 0 }}</el-tag>
              </div>
              <div class="info-item">
                <span class="info-label">下载数：</span>
                <el-tag type="success">{{ graph?.downloadCount || 0 }}</el-tag>
              </div>
              <div class="info-item">
                <span class="info-label">节点数量：</span>
                <el-tag type="primary">{{ nodes?.length || 0 }}</el-tag>
              </div>
              <div class="info-item">
                <span class="info-label">关系数量：</span>
                <el-tag type="success">{{ edges?.length || 0 }}</el-tag>
              </div>
              <div class="info-item">
                <span class="info-label">可见性：</span>
                <el-tag :type="graph?.isPublic ? 'success' : 'info'">
                  {{ graph?.isPublic ? '公开' : '私有' }}
                </el-tag>
              </div>
            </div>
          </el-card>

          <el-card class="nodes-card">
            <template #header>
              <div class="card-header">
                <span>节点列表</span>
                <el-button v-if="isOwner && isEditMode" type="primary" size="small" @click="showAddNodeDialog">
                  添加节点
                </el-button>
              </div>
            </template>
            <el-table :data="nodes || []" style="width: 100%" max-height="300">
              <el-table-column prop="name" label="节点名称" />
              <el-table-column prop="category" label="类别" />
              <el-table-column v-if="isOwner && isEditMode" label="操作" width="150">
                <template #default="scope">
                  <el-button
                    type="primary"
                    size="small"
                    @click="showEditNodeDialog(scope.row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    @click="deleteNode(scope.$index)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card class="edges-card">
            <template #header>
              <div class="card-header">
                <span>关系列表</span>
                <el-button v-if="isOwner && isEditMode" type="primary" size="small" @click="showAddEdgeDialog">
                  添加关系
                </el-button>
              </div>
            </template>
            <el-table :data="edgeList" style="width: 100%" max-height="300">
              <el-table-column prop="sourceName" label="起点" />
              <el-table-column prop="label" label="关系" />
              <el-table-column prop="targetName" label="终点" />
              <el-table-column v-if="isOwner && isEditMode" label="操作" width="100">
                <template #default="scope">
                  <el-button
                    type="danger"
                    size="small"
                    @click="deleteEdge(scope.$index)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </div>

    <el-dialog v-model="addNodeDialogVisible" title="添加节点" width="400px">
      <el-form :model="newNode" label-width="80px">
        <el-form-item label="节点名称">
          <el-input v-model="newNode.name" placeholder="请输入节点名称" />
        </el-form-item>
        <el-form-item label="类别">
          <el-input v-model="newNode.category" placeholder="请输入类别" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addNodeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addNode">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addEdgeDialogVisible" title="添加关系" width="500px">
      <el-form :model="newEdge" label-width="80px">
        <el-form-item label="起点">
          <el-select v-model="newEdge.source" placeholder="请选择起点" style="width: 100%">
            <el-option
              v-for="node in nodes"
              :key="node.nodeId"
              :label="node.name"
              :value="node.nodeId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关系">
          <el-input v-model="newEdge.label" placeholder="请输入关系" />
        </el-form-item>
        <el-form-item label="终点">
          <el-select v-model="newEdge.target" placeholder="请选择终点" style="width: 100%">
            <el-option
              v-for="node in nodes"
              :key="node.nodeId"
              :label="node.name"
              :value="node.nodeId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addEdgeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addEdge">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editNodeDialogVisible" title="编辑节点" width="400px">
      <el-form :model="editNode" label-width="80px">
        <el-form-item label="节点名称">
          <el-input v-model="editNode.name" placeholder="请输入节点名称" />
        </el-form-item>
        <el-form-item label="类别">
          <el-input v-model="editNode.category" placeholder="请输入类别" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editNodeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updateNode">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editGraphDialogVisible" title="编辑图谱" width="500px">
      <el-form :model="editGraph" label-width="80px">
        <el-form-item label="图谱名称">
          <el-input v-model="editGraph.name" placeholder="请输入图谱名称" />
        </el-form-item>
        <el-form-item label="主要内容">
          <el-input
            v-model="editGraph.description"
            type="textarea"
            :rows="4"
            placeholder="请输入主要内容"
          />
        </el-form-item>
        <el-form-item label="可见性">
          <el-switch v-model="editGraph.isPublic" />
          <span style="margin-left: 10px">{{ editGraph.isPublic ? '公开' : '私有' }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editGraphDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updateGraph">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import * as XLSX from 'xlsx'
import {
  ArrowLeft,
  Download,
  Edit,
  ZoomIn,
  ZoomOut,
  RefreshRight,
  Check
} from '@element-plus/icons-vue'
import { graphApi } from '@/api/graph'
import { excelApi } from '@/api/excel'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const graphChart = ref(null)
const chartInstance = ref(null)
const isEditMode = ref(false)
const addNodeDialogVisible = ref(false)
const addEdgeDialogVisible = ref(false)
const editNodeDialogVisible = ref(false)
const editGraphDialogVisible = ref(false)
const newNode = ref({ name: '', category: '' })
const editNode = ref({ id: '', name: '', category: '' })
const newEdge = ref({ source: '', label: '', target: '' })
const editGraph = ref({ name: '', description: '', isPublic: false })
const loading = ref(false)
const isLiked = ref(false)
const comments = ref([])
const newComment = ref('')
const commentLoading = ref(false)

const graphId = computed(() => route.params.id)
const graph = ref(null)
const nodes = ref([])
const edges = ref([])

const isOwner = computed(() => {
  return graph.value?.ownerId === userStore.user?.id
})

const edgeList = computed(() => {
  if (!edges.value || !nodes.value) return []
  return edges.value.map(edge => {
    const sourceNode = nodes.value.find(n => n.nodeId === edge.sourceNodeId)
    const targetNode = nodes.value.find(n => n.nodeId === edge.targetNodeId)
    return {
      ...edge,
      sourceName: sourceNode?.name || edge.sourceNodeId,
      targetName: targetNode?.name || edge.targetNodeId,
      label: edge.relation
    }
  })
})

onMounted(async () => {
  await loadGraph()
  initChart()
})

onUnmounted(() => {
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
})

watch(() => graph.value, () => {
  if (chartInstance.value) {
    renderChart()
  }
}, { deep: true })

const loadGraph = async () => {
  loading.value = true
  try {
    const response = await graphApi.getGraphById(graphId.value)
    graph.value = response.data
    
    await loadNodes()
    await loadEdges()
    await checkLikeStatus()
    await loadComments()
  } catch (error) {
    console.error('加载图谱失败:', error)
    ElMessage.error('加载图谱失败')
  } finally {
    loading.value = false
  }
}

const loadNodes = async () => {
  try {
    const response = await graphApi.getNodes(graphId.value)
    nodes.value = response.data
  } catch (error) {
    console.error('加载节点失败:', error)
  }
}

const loadEdges = async () => {
  try {
    const response = await graphApi.getEdges(graphId.value)
    edges.value = response.data
  } catch (error) {
    console.error('加载关系失败:', error)
  }
}

const initChart = () => {
  if (!graphChart.value) return
  
  chartInstance.value = echarts.init(graphChart.value)
  renderChart()
  
  window.addEventListener('resize', handleResize)
}

const renderChart = () => {
  if (!chartInstance.value || !graph.value) return
  
  const chartNodes = nodes.value.map(node => ({
    id: node.nodeId,
    name: node.name,
    category: node.category,
    symbolSize: 50,
    itemStyle: {
      color: getNodeColor(node.category)
    }
  }))
  
  const chartEdges = edges.value.map(edge => ({
    source: edge.sourceNodeId,
    target: edge.targetNodeId,
    label: {
      show: true,
      formatter: edge.relation,
      fontSize: 12
    },
    lineStyle: {
      width: 2,
      curveness: 0.2
    }
  }))
  
  const categories = [...new Set(nodes.value.map(n => n.category))]
  
  const option = {
    title: {
      text: graph.value.name,
      top: 10,
      left: 10,
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold'
      }
    },
    tooltip: {
      formatter: (params) => {
        if (params.dataType === 'node') {
          return `节点: ${params.name}<br>类别: ${params.data.category}`
        } else {
          return `关系: ${params.data.label}`
        }
      }
    },
    legend: {
      data: categories,
      top: 50,
      right: 10
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: chartNodes,
        links: chartEdges,
        categories: categories.map(c => ({ name: c })),
        roam: true,
        label: {
          show: true,
          position: 'bottom',
          formatter: '{b}'
        },
        lineStyle: {
          color: 'source',
          curveness: 0.3
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 4
          }
        },
        force: {
          repulsion: 300,
          edgeLength: 100
        }
      }
    ]
  }
  
  chartInstance.value.setOption(option)
}

const getNodeColor = (category) => {
  const colors = [
    '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
    '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'
  ]
  const index = categories.value?.indexOf(category) % colors.length || 0
  return colors[index]
}

const categories = computed(() => {
  if (!graph.value?.nodes) return []
  return [...new Set(graph.value.nodes.map(n => n.category))]
})

const handleResize = () => {
  chartInstance.value?.resize()
}

const zoomIn = () => {
  const option = chartInstance.value.getOption()
  const zoom = option.series[0].zoom || 1
  chartInstance.value.setOption({
    series: [{ zoom: zoom + 0.2 }]
  })
}

const zoomOut = () => {
  const option = chartInstance.value.getOption()
  const zoom = option.series[0].zoom || 1
  chartInstance.value.setOption({
    series: [{ zoom: Math.max(0.2, zoom - 0.2) }]
  })
}

const resetZoom = () => {
  chartInstance.value.setOption({
    series: [{ zoom: 1 }]
  })
}

const toggleEditMode = () => {
  isEditMode.value = !isEditMode.value
}

const showAddNodeDialog = () => {
  newNode.value = { name: '', category: '' }
  addNodeDialogVisible.value = true
}

const showEditNodeDialog = (node) => {
  editNode.value = { id: node.id, name: node.name, category: node.category }
  editNodeDialogVisible.value = true
}

const updateNode = async () => {
  if (!editNode.value.name.trim()) {
    ElMessage.warning('请输入节点名称')
    return
  }
  
  try {
    await graphApi.updateNode({
      id: editNode.value.id,
      name: editNode.value.name,
      category: editNode.value.category || '未分类'
    })
    
    await loadNodes()
    renderChart()
    editNodeDialogVisible.value = false
    ElMessage.success('节点更新成功')
  } catch (error) {
    console.error('更新节点失败:', error)
    ElMessage.error('更新节点失败')
  }
}

const showEditGraphDialog = () => {
  editGraph.value = { 
    name: graph.value?.name || '', 
    description: graph.value?.description || '', 
    isPublic: graph.value?.isPublic || false 
  }
  editGraphDialogVisible.value = true
}

const updateGraph = async () => {
  if (!editGraph.value.name.trim()) {
    ElMessage.warning('请输入图谱名称')
    return
  }
  
  try {
    await graphApi.updateGraph({
      id: graphId.value,
      name: editGraph.value.name,
      description: editGraph.value.description,
      isPublic: editGraph.value.isPublic ? 1 : 0
    })
    
    await loadGraph()
    editGraphDialogVisible.value = false
    ElMessage.success('图谱更新成功')
  } catch (error) {
    console.error('更新图谱失败:', error)
    ElMessage.error('更新图谱失败')
  }
}

const addNode = async () => {
  if (!newNode.value.name.trim()) {
    ElMessage.warning('请输入节点名称')
    return
  }
  
  try {
    const newId = String(nodes.value.length + 1)
    await graphApi.createNode({
      graphId: graphId.value,
      nodeId: newId,
      name: newNode.value.name,
      category: newNode.value.category || '未分类'
    })
    
    await loadNodes()
    renderChart()
    addNodeDialogVisible.value = false
    ElMessage.success('节点添加成功')
  } catch (error) {
    console.error('添加节点失败:', error)
    ElMessage.error('添加节点失败')
  }
}

const deleteNode = async (index) => {
  const nodeId = nodes.value[index].id
  try {
    await graphApi.deleteNode(nodeId)
    await loadNodes()
    await loadEdges()
    renderChart()
    ElMessage.success('节点删除成功')
  } catch (error) {
    console.error('删除节点失败:', error)
    ElMessage.error('删除节点失败')
  }
}

const showAddEdgeDialog = () => {
  newEdge.value = { source: '', label: '', target: '' }
  addEdgeDialogVisible.value = true
}

const addEdge = async () => {
  if (!newEdge.value.source || !newEdge.value.target || !newEdge.value.label) {
    ElMessage.warning('请填写完整的关系信息')
    return
  }
  
  try {
    await graphApi.createEdge({
      graphId: graphId.value,
      sourceNodeId: newEdge.value.source,
      targetNodeId: newEdge.value.target,
      relation: newEdge.value.label
    })
    
    await loadEdges()
    renderChart()
    addEdgeDialogVisible.value = false
    ElMessage.success('关系添加成功')
  } catch (error) {
    console.error('添加关系失败:', error)
    ElMessage.error('添加关系失败')
  }
}

const deleteEdge = async (index) => {
  const edgeId = edges.value[index].id
  try {
    await graphApi.deleteEdge(edgeId)
    await loadEdges()
    renderChart()
    ElMessage.success('关系删除成功')
  } catch (error) {
    console.error('删除关系失败:', error)
    ElMessage.error('删除关系失败')
  }
}

const exportToExcel = async () => {
  if (!graphId.value) return
  
  try {
    const response = await excelApi.exportGraph(graphId.value)
    
    const blob = new Blob([response], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${graph.value?.name || '知识图谱'}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

const goBack = () => {
  router.back()
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

const checkLikeStatus = async () => {
  try {
    const response = await graphApi.isLiked(graphId.value, userStore.user?.id)
    isLiked.value = response.data
  } catch (error) {
    console.error('检查点赞状态失败:', error)
  }
}

const handleLike = async () => {
  try {
    if (isLiked.value) {
      await graphApi.unlikeGraph(graphId.value)
      isLiked.value = false
      if (graph.value && graph.value.likeCount > 0) {
        graph.value.likeCount--
      }
      ElMessage.success('取消点赞成功')
    } else {
      await graphApi.likeGraph(graphId.value)
      isLiked.value = true
      if (graph.value) {
        graph.value.likeCount++
      }
      ElMessage.success('点赞成功')
    }
  } catch (error) {
    console.error('点赞操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const loadComments = async () => {
  try {
    const response = await graphApi.getComments(graphId.value)
    comments.value = response.data
  } catch (error) {
    console.error('加载评论失败:', error)
  }
}

const handleAddComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  
  if (!userStore.user?.id) {
    ElMessage.warning('请先登录')
    return
  }
  
  commentLoading.value = true
  try {
    await graphApi.addComment({
      graphId: graphId.value,
      userId: userStore.user.id,
      content: newComment.value
    })
    
    newComment.value = ''
    await loadComments()
    
    if (graph.value) {
      graph.value.commentCount++
    }
    
    ElMessage.success('评论发表成功')
  } catch (error) {
    console.error('发表评论失败:', error)
    ElMessage.error('发表评论失败')
  } finally {
    commentLoading.value = false
  }
}

const handleDeleteComment = async (commentId) => {
  try {
    await graphApi.deleteComment(commentId, userStore.user.id)
    await loadComments()
    
    if (graph.value && graph.value.commentCount > 0) {
      graph.value.commentCount--
    }
    
    ElMessage.success('评论删除成功')
  } catch (error) {
    console.error('删除评论失败:', error)
    ElMessage.error('删除评论失败')
  }
}
</script>

<style scoped>
.graph-detail-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.graph-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.title-section {
  display: flex;
  flex-direction: column;
}

.graph-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.graph-description {
  color: #666;
  font-size: 0.9rem;
  margin: 5px 0 0 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.graph-content {
  display: flex;
  gap: 20px;
  flex: 1;
  overflow: hidden;
}

.graph-visual {
  flex: 2;
  display: flex;
  flex-direction: column;
}

.graph-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.graph-card :deep(.el-card__body) {
  flex: 1;
  padding: 0;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.graph-chart {
  width: 100%;
  height: 100%;
  min-height: 500px;
}

.graph-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
}

.info-card,
.nodes-card,
.edges-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  color: #666;
  font-weight: 500;
}

.info-value {
  color: #333;
  font-weight: 600;
}

.like-section {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.comment-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.comment-section .comment-header {
  margin-bottom: 15px;
}

.comment-section .comment-header h3 {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.comment-input-area {
  margin-bottom: 20px;
}

.comment-input {
  margin-bottom: 20px;
}

.comment-list {
  max-height: 400px;
  overflow-y: auto;
}

.no-comment {
  text-align: center;
  color: #999;
  padding: 20px;
  font-size: 0.9rem;
}

.comment-item {
  padding: 15px 0;
  border-bottom: 1px solid #eee;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.comment-user {
  font-weight: 600;
  color: #333;
}

.comment-time {
  color: #999;
  font-size: 0.8rem;
}

.comment-content {
  color: #666;
  line-height: 1.6;
  word-wrap: break-word;
}

:deep(.el-table) {
  font-size: 0.9rem;
}

:deep(.el-card__header) {
  padding: 15px 20px;
  font-weight: 600;
  color: #333;
}

:deep(.el-card__body) {
  padding: 20px;
}

@media (max-width: 1200px) {
  .graph-content {
    flex-direction: column;
    overflow-y: auto;
  }
  
  .graph-visual {
    min-height: 500px;
  }
}
</style>
<template>
  <div class="square-container">
    <div class="square-header">
      <h1 class="page-title">图谱广场</h1>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索知识图谱..."
        size="large"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      
      <el-select
        v-model="selectedCategory"
        placeholder="选择分类"
        size="large"
        clearable
        @change="handleCategoryChange"
        style="width: 200px; margin-left: 10px"
      >
        <el-option
          v-for="category in categories"
          :key="category.id"
          :label="category.name"
          :value="category.id"
        />
      </el-select>
    </div>

    <el-tabs v-model="activeTab" class="square-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="公共图谱" name="public">
        <div v-loading="loading" v-if="publicGraphs.length === 0" class="empty-state">
          <el-empty description="暂无公共知识图谱" />
        </div>
        <div v-else class="graph-grid">
          <div
            v-for="graph in publicGraphs"
            :key="graph.id"
            class="graph-card"
            @click="viewGraph(graph.id)"
          >
            <div class="graph-preview">
              <div class="graph-icon">📊</div>
            </div>
            <div class="graph-info">
              <h3 class="graph-title">{{ graph.name }}</h3>
              <p class="graph-desc">{{ graph.description }}</p>
              <div class="graph-meta">
                <span class="meta-item">
                  <el-icon><View /></el-icon>
                  {{ graph.viewCount }}
                </span>
                <span class="meta-item">
                  <svg class="like-icon" :class="{ 'liked': graph.isLiked }" @click.stop="handleLike(graph)" viewBox="0 0 1024 1024">
                    <path fill="currentColor" d="m512 747.84 228.16 119.936a6.4 6.4 0 0 0 9.28-6.72l-43.52-254.08 184.512-179.904a6.4 6.4 0 0 0-3.52-10.88l-255.104-37.12L517.76 147.904a6.4 6.4 0 0 0-11.52 0L392.192 379.072l-255.104 37.12a6.4 6.4 0 0 0-3.52 10.88L318.08 606.976l-43.584 254.08a6.4 6.4 0 0 0 9.28 6.72zM313.6 924.48a70.4 70.4 0 0 1-102.144-74.24l37.888-220.928L88.96 472.96A70.4 70.4 0 0 1 128 352.896l221.76-32.256 99.2-200.96a70.4 70.4 0 0 1 126.208 0l99.2 200.96 221.824 32.256a70.4 70.4 0 0 1 39.04 120.064L774.72 629.376l37.888 220.928a70.4 70.4 0 0 1-102.144 74.24L512 820.096l-198.4 104.32z"></path>
                  </svg>
                  <span class="like-count">{{ graph.likeCount }}</span>
                </span>
                <span class="meta-item">
                  <el-icon><Clock /></el-icon>
                  {{ formatDate(graph.createdAt) }}
                </span>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="publicGraphs.length > 0" class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="publicGraphs.length"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的图谱" name="private">
        <div v-loading="loading" v-if="myGraphs.length === 0" class="empty-state">
          <el-empty description="您还没有创建任何知识图谱">
            <el-button type="primary" @click="goToCreate">创建图谱</el-button>
          </el-empty>
        </div>
        <div v-else class="graph-grid">
          <div
            v-for="graph in myGraphs"
            :key="graph.id"
            class="graph-card"
          >
            <div class="graph-preview">
              <div class="graph-icon">📊</div>
              <div class="graph-actions">
                <el-button
                  type="primary"
                  size="small"
                  @click="viewGraph(graph.id)"
                >
                  查看
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="deleteGraph(graph.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
            <div class="graph-info">
              <h3 class="graph-title">{{ graph.name }}</h3>
              <p class="graph-desc">{{ graph.description }}</p>
              <div class="graph-meta">
                <span class="meta-item">
                  <el-icon><Clock /></el-icon>
                  {{ formatDate(graph.createdAt) }}
                </span>
                <el-tag :type="graph.isPublic ? 'success' : 'info'" size="small">
                  {{ graph.isPublic ? '公开' : '私有' }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="myGraphs.length > 0" class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="myGraphs.length"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Clock, Search, Star, View } from '@element-plus/icons-vue'
import { graphApi } from '@/api/graph'
import { categoryApi } from '@/api/category'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('public')
const publicGraphs = ref([])
const myGraphs = ref([])
const categories = ref([])
const selectedCategory = ref(null)
const searchKeyword = ref('')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)

onMounted(() => {
  loadCategories()
  loadPublicGraphs()
  if (userStore.isLoggedIn) {
    loadMyGraphs()
  }
})

const loadCategories = async () => {
  try {
    const response = await categoryApi.getAllCategories()
    categories.value = response.data
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadPublicGraphs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (selectedCategory.value) {
      params.categoryId = selectedCategory.value
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    
    const response = await graphApi.getPublicGraphs(params)
    publicGraphs.value = response.data.records
    await checkLikeStatus(publicGraphs.value)
  } catch (error) {
    console.error('加载公共图谱失败:', error)
    ElMessage.error('加载公共图谱失败')
  } finally {
    loading.value = false
  }
}

const loadMyGraphs = async () => {
  if (!userStore.userId) return
  
  loading.value = true
  try {
    const response = await graphApi.getMyGraphs({
      page: currentPage.value,
      size: pageSize.value,
      userId: userStore.userId
    })
    myGraphs.value = response.data.records
  } catch (error) {
    console.error('加载我的图谱失败:', error)
    ElMessage.error('加载我的图谱失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadPublicGraphs()
}

const handleCategoryChange = () => {
  currentPage.value = 1
  loadPublicGraphs()
}

const handlePageChange = (page) => {
  currentPage.value = page
  if (activeTab.value === 'public') {
    loadPublicGraphs()
  } else {
    loadMyGraphs()
  }
}

const handleTabChange = (tabName) => {
  currentPage.value = 1
  if (tabName === 'public') {
    loadPublicGraphs()
  } else {
    loadMyGraphs()
  }
}

const viewGraph = (id) => {
  router.push(`/layout/graph/${id}`)
}

const deleteGraph = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个知识图谱吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await graphApi.deleteGraph(id)
    ElMessage.success('删除成功')
    loadMyGraphs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleLike = async (graph) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  try {
    if (graph.isLiked) {
      await graphApi.unlikeGraph(graph.id, userStore.userId)
      graph.isLiked = false
      if (graph.likeCount > 0) {
        graph.likeCount--
      }
      ElMessage.success('取消点赞成功')
    } else {
      await graphApi.likeGraph(graph.id, userStore.userId)
      graph.isLiked = true
      graph.likeCount++
      ElMessage.success('点赞成功')
    }
  } catch (error) {
    console.error('点赞操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const checkLikeStatus = async (graphs) => {
  if (!userStore.isLoggedIn) return
  
  for (const graph of graphs) {
    try {
      const response = await graphApi.isLiked(graph.id, userStore.userId)
      graph.isLiked = response.data
    } catch (error) {
      console.error('检查点赞状态失败:', error)
      graph.isLiked = false
    }
  }
}

const goToCreate = () => {
  router.push('/layout/create')
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.square-container {
  max-width: 1400px;
  margin: 0 auto;
}

.square-header {
  margin-bottom: 15px;
}

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  gap: 10px;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.square-tabs {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

:deep(.el-tabs__header) {
  margin-bottom: 30px;
}

:deep(.el-tabs__item) {
  font-size: 1rem;
  font-weight: 500;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.graph-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 28px;
}

.graph-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #e8ecf4;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.graph-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
}

.graph-preview {
  height: 200px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.graph-preview::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 60%);
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.graph-icon {
  font-size: 4.5rem;
  opacity: 0.95;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
  position: relative;
  z-index: 1;
}

.graph-info {
  padding: 24px;
  background: linear-gradient(180deg, #ffffff 0%, #f8f9fa 100%);
}

.graph-title {
  font-size: 1.3rem;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 12px;
  line-height: 1.4;
}

.graph-desc {
  color: #64748b;
  font-size: 0.95rem;
  line-height: 1.6;
  margin-bottom: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.graph-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #e8ecf4;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 0.9rem;
}

.meta-item .el-icon {
  font-size: 1.1rem;
}

.like-icon {
  width: 20px;
  height: 20px;
  cursor: pointer;
  transition: all 0.3s;
  color: #94a3b8;
}

.like-icon:hover {
  transform: scale(1.1);
  color: #f59e0b;
}

.like-icon.liked {
  color: #f59e0b;
  fill: #f59e0b;
}

.like-count {
  font-weight: 600;
  color: #64748b;
}

.meta-item .el-icon {
  font-size: 1.1rem;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e8ecf4;
}

@media (max-width: 768px) {
  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-bar .el-select {
    width: 100% !important;
    margin-left: 0 !important;
  }
  
  .graph-grid {
    grid-template-columns: 1fr;
  }
}
</style>
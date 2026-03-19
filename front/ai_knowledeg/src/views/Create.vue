<template>
  <div class="create-container">
    <div class="create-header">
      <h1 class="page-title">创建图谱</h1>
    </div>

    <el-card class="create-card">
      <el-form :model="graphForm" :rules="rules" ref="graphFormRef" label-width="100px">
        <el-form-item label="图谱名称" prop="name">
          <el-input v-model="graphForm.name" placeholder="请输入知识图谱名称" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="graphForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识图谱描述"
          />
        </el-form-item>

        <el-form-item label="分类">
          <el-select
            v-model="selectedCategory"
            placeholder="请选择分类"
            style="width: 100%"
          >
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="是否公开">
          <el-switch v-model="graphForm.isPublic" />
        </el-form-item>

        <el-divider />

        <div class="input-methods">
          <el-radio-group v-model="inputMethod" @change="handleMethodChange">
            <el-radio-button label="manual">手动输入</el-radio-button>
            <el-radio-button label="excel">Excel导入</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="inputMethod === 'manual'" class="manual-input">
          <el-divider content-position="left">节点管理</el-divider>
          
          <div class="node-list">
            <div
              v-for="(node, index) in graphForm.nodes"
              :key="index"
              class="node-item"
            >
              <el-input
                v-model="node.name"
                placeholder="节点名称"
                style="width: 200px; margin-right: 10px"
              />
              <el-input
                v-model="node.category"
                placeholder="类别"
                style="width: 150px; margin-right: 10px"
              />
              <el-button
                type="danger"
                size="small"
                @click="removeNode(index)"
                :disabled="graphForm.nodes.length <= 1"
              >
                删除
              </el-button>
            </div>
          </div>
          
          <el-button type="primary" @click="addNode" style="margin-top: 15px">
            添加节点
          </el-button>

          <el-divider content-position="left">关系管理</el-divider>
          
          <div class="edge-list">
            <div
              v-for="(edge, index) in graphForm.edges"
              :key="index"
              class="edge-item"
            >
              <el-select
                v-model="edge.source"
                placeholder="起点"
                style="width: 180px; margin-right: 10px"
              >
                <el-option
                  v-for="node in graphForm.nodes"
                  :key="node.id"
                  :label="node.name"
                  :value="node.id"
                />
              </el-select>
              <el-input
                v-model="edge.label"
                placeholder="关系"
                style="width: 120px; margin-right: 10px"
              />
              <el-select
                v-model="edge.target"
                placeholder="终点"
                style="width: 180px; margin-right: 10px"
              >
                <el-option
                  v-for="node in graphForm.nodes"
                  :key="node.id"
                  :label="node.name"
                  :value="node.id"
                />
              </el-select>
              <el-button
                type="danger"
                size="small"
                @click="removeEdge(index)"
              >
                删除
              </el-button>
            </div>
          </div>
          
          <el-button type="primary" @click="addEdge" style="margin-top: 15px">
            添加关系
          </el-button>
        </div>

        <div v-if="inputMethod === 'excel'" class="excel-input">
          <el-divider content-position="left">Excel导入</el-divider>
          
          <div class="upload-area">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :on-change="handleFileChange"
              :limit="1"
              accept=".xlsx,.xls"
              drag
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                将文件拖到此处，或<em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  只能上传 xlsx/xls 文件，文件格式：第一列为节点名称，第二列为节点类别
                </div>
              </template>
            </el-upload>
          </div>

          <el-alert
            title="Excel格式说明"
            type="info"
            :closable="false"
            style="margin-top: 20px"
          >
            <p>节点表：节点名称 | 节点类别</p>
            <p>关系表：起点 | 关系 | 终点</p>
          </el-alert>

          <div v-if="importedData.nodes.length > 0" class="import-preview">
            <h3>导入预览</h3>
            <div class="preview-section">
              <h4>节点 ({{ importedData.nodes.length }})</h4>
              <el-table :data="importedData.nodes.slice(0, 5)" style="width: 100%">
                <el-table-column prop="name" label="节点名称" />
                <el-table-column prop="category" label="类别" />
              </el-table>
              <div v-if="importedData.nodes.length > 5" class="more-hint">
                还有 {{ importedData.nodes.length - 5 }} 个节点...
              </div>
            </div>
            <div v-if="importedData.edges.length > 0" class="preview-section">
              <h4>关系 ({{ importedData.edges.length }})</h4>
              <el-table :data="importedData.edges.slice(0, 5)" style="width: 100%">
                <el-table-column prop="source" label="起点" />
                <el-table-column prop="label" label="关系" />
                <el-table-column prop="target" label="终点" />
              </el-table>
              <div v-if="importedData.edges.length > 5" class="more-hint">
                还有 {{ importedData.edges.length - 5 }} 个关系...
              </div>
            </div>
          </div>
        </div>

        <el-divider />

        <el-form-item>
          <el-button type="primary" size="large" @click="handleCreate" :loading="loading">
            创建图谱
          </el-button>
          <el-button size="large" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import { graphApi } from '@/api/graph'
import { categoryApi } from '@/api/category'

const router = useRouter()
const userStore = useUserStore()

const graphFormRef = ref(null)
const uploadRef = ref(null)
const loading = ref(false)
const inputMethod = ref('manual')
const categories = ref([])
const selectedCategory = ref(null)
const importedData = reactive({
  nodes: [],
  edges: []
})

const graphForm = reactive({
  name: '',
  description: '',
  isPublic: false,
  categoryId: null,
  nodes: [
    { id: '1', name: '', category: '' }
  ],
  edges: []
})

const rules = {
  name: [
    { required: true, message: '请输入图谱名称', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入描述', trigger: 'blur' }
  ]
}

const loadCategories = async () => {
  try {
    const response = await categoryApi.getAllCategories()
    categories.value = response.data
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

loadCategories()

const addNode = () => {
  const newId = String(graphForm.nodes.length + 1)
  graphForm.nodes.push({
    id: newId,
    name: '',
    category: ''
  })
}

const removeNode = (index) => {
  const nodeId = graphForm.nodes[index].id
  graphForm.nodes.splice(index, 1)
  graphForm.edges = graphForm.edges.filter(edge => 
    edge.source !== nodeId && edge.target !== nodeId
  )
}

const addEdge = () => {
  graphForm.edges.push({
    source: '',
    label: '',
    target: ''
  })
}

const removeEdge = (index) => {
  graphForm.edges.splice(index, 1)
}

const handleMethodChange = () => {
  if (inputMethod.value === 'manual') {
    importedData.nodes = []
    importedData.edges = []
  }
}

const handleFileChange = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const data = new Uint8Array(e.target.result)
      const workbook = XLSX.read(data, { type: 'array' })
      
      const nodesSheet = workbook.Sheets['节点'] || workbook.Sheets['Nodes']
      const edgesSheet = workbook.Sheets['关系'] || workbook.Sheets['Edges']
      
      if (nodesSheet) {
        const nodesData = XLSX.utils.sheet_to_json(nodesSheet)
        importedData.nodes = nodesData.map((row, index) => ({
          id: String(index + 1),
          name: row['节点名称'] || row['Name'] || '',
          category: row['节点类别'] || row['Category'] || ''
        }))
      }
      
      if (edgesSheet) {
        const edgesData = XLSX.utils.sheet_to_json(edgesSheet)
        importedData.edges = edgesData.map(row => ({
          source: row['起点'] || row['Source'] || '',
          label: row['关系'] || row['Relation'] || '',
          target: row['终点'] || row['Target'] || ''
        }))
      }
      
      ElMessage.success('文件解析成功')
    } catch (error) {
      ElMessage.error('文件解析失败，请检查文件格式')
      console.error(error)
    }
  }
  reader.readAsArrayBuffer(file.raw)
}

const handleCreate = async () => {
  if (!graphFormRef.value) return
  
  await graphFormRef.value.validate((valid) => {
    if (valid) {
      if (inputMethod.value === 'manual') {
        const validNodes = graphForm.nodes.filter(node => node.name.trim())
        const validEdges = graphForm.edges.filter(edge => 
          edge.source && edge.target && edge.label
        )
        
        if (validNodes.length < 2) {
          ElMessage.warning('请至少添加2个有效节点')
          return
        }
        
        createGraph(validNodes, validEdges)
      } else {
        if (importedData.nodes.length < 2) {
          ElMessage.warning('请确保Excel中至少有2个节点')
          return
        }
        createGraph(importedData.nodes, importedData.edges)
      }
    }
  })
}

const createGraph = async (nodes, edges) => {
  if (!userStore.userId) {
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }
  
  loading.value = true
  
  try {
    const graphData = {
      name: graphForm.name,
      description: graphForm.description,
      categoryId: selectedCategory.value,
      ownerId: userStore.userId,
      isPublic: graphForm.isPublic ? 1 : 0
    }
    
    const response = await graphApi.createGraph(graphData)
    const graphId = response.data.id
    
    if (nodes && nodes.length > 0) {
      for (const node of nodes) {
        await graphApi.createNode({
          graphId: graphId,
          nodeId: node.id,
          name: node.name,
          category: node.category
        })
      }
    }
    
    if (edges && edges.length > 0) {
      for (const edge of edges) {
        await graphApi.createEdge({
          graphId: graphId,
          sourceNodeId: edge.source,
          targetNodeId: edge.target,
          relation: edge.label
        })
      }
    }
    
    ElMessage.success('知识图谱创建成功')
    loading.value = false
    router.push(`/layout/graph/${graphId}`)
  } catch (error) {
    console.error('创建图谱失败:', error)
    ElMessage.error('创建图谱失败：' + (error.message || '未知错误'))
    loading.value = false
  }
}

const handleReset = () => {
  graphFormRef.value?.resetFields()
  graphForm.nodes = [{ id: '1', name: '', category: '' }]
  graphForm.edges = []
  importedData.nodes = []
  importedData.edges = []
  inputMethod.value = 'manual'
}
</script>

<style scoped>
.create-container {
  max-width: 1000px;
  margin: 0 auto;
}

.create-header {
  margin-bottom: 15px;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.create-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.input-methods {
  margin-bottom: 30px;
  text-align: center;
}

.manual-input,
.excel-input {
  padding: 20px 0;
}

.node-list,
.edge-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 8px;
}

.node-item,
.edge-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 10px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.upload-area {
  margin: 20px 0;
}

.import-preview {
  margin-top: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.import-preview h3 {
  margin-bottom: 20px;
  color: #333;
}

.preview-section {
  margin-bottom: 20px;
}

.preview-section h4 {
  margin-bottom: 10px;
  color: #666;
}

.more-hint {
  text-align: center;
  color: #999;
  padding: 10px;
  font-style: italic;
}

:deep(.el-upload-dragger) {
  padding: 40px;
}

:deep(.el-divider__text) {
  font-weight: 600;
  color: #333;
}
</style>
# AI知识图谱平台 - 启动和运行指南

## 项目概述

这是一个完整的AI知识图谱平台，包含前端和后端两部分：

- **前端**: Vue 3 + Element Plus + ECharts
- **后端**: Spring Boot 3.2.0 + MyBatis-Plus + Redis + MySQL
- **AI集成**: 智谱AI大模型API
- **功能**: 用户管理、知识图谱管理、AI对话、Excel导入导出、积分系统、评论系统等

## 环境要求

### 前端环境
- Node.js >= 16.x
- npm >= 8.x
- 现代浏览器（Chrome、Firefox、Edge等）

### 后端环境
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

## 数据库配置

### 1. 创建数据库

```bash
# 登录MySQL
mysql -u root -p

# 执行初始化脚本
source ./ai_knowledge/back_ai_know/init.sql
```

### 2. 配置数据库连接

编辑 `./ai_knowledge/back_ai_know/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_knowledge_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: your_password  # 修改为你的MySQL密码
```

## Redis配置

确保Redis服务正在运行：

```bash
# Windows
redis-server.exe

# Linux/Mac
redis-server
```

配置Redis连接（如果需要）：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password:  # 如果Redis设置了密码
```

## 智谱AI配置

编辑 `e:/ai_knowledge/back_ai_know/src/main/resources/application.yml`:

```yaml
zhipu:
  api:
    key: your-zhipu-api-key  # 替换为你的智谱AI API密钥
    url: https://open.bigmodel.cn/api/paas/v4/chat/completions
    model: glm-4
```

获取智谱AI API密钥：https://open.bigmodel.cn/

## 后端启动

### 1. 安装依赖

```bash
cd e:/ai_knowledge/back_ai_know
mvn clean install
```

### 2. 启动后端服务

```bash
mvn spring-boot:run
```

或者使用IDE（如IntelliJ IDEA）直接运行 `BackAiKnowApplication.java`

### 3. 验证后端启动

访问：http://localhost:8080/api/doc

应该能看到Swagger API文档界面。

## 前端启动

### 1. 安装依赖

```bash
cd e:/ai_knowledge/front/ai_knowledeg
npm install
```

### 2. 启动前端开发服务器

```bash
npm run dev
```

### 3. 访问前端

打开浏览器访问：http://localhost:5175

## 功能说明

### 用户功能
1. **注册/登录**: 支持用户注册和登录，使用Redis存储会话
2. **个人中心**: 查看个人信息、积分等
3. **积分系统**: 
   - 每次获得点赞加1分
   - 下载知识图谱消耗10分

### 知识图谱功能
1. **创建图谱**: 
   - 手动输入节点和关系
   - Excel导入节点和关系数据
   - 设置公开/私有
   - 选择分类
2. **查看图谱**: 可视化展示知识图谱
3. **编辑图谱**: 添加/删除节点和关系
4. **导出Excel**: 导出节点和关系数据为Excel文件

### 知识图谱广场
1. **浏览图谱**: 查看所有公开的知识图谱
2. **分类筛选**: 按分类浏览图谱
3. **搜索功能**: 按关键词搜索图谱
4. **热点展示**: 显示热门知识图谱

### AI对话功能
1. **生成图谱**: 通过AI对话生成知识图谱
2. **历史记录**: Redis存储2天的对话历史
3. **系统提示词**: 自动分析文本提取节点和关系

### 评论和互动
1. **评论功能**: 对知识图谱进行评论
2. **点赞功能**: 点赞知识图谱，增加热度

### 后台管理
1. **用户管理**: 查看、删除用户
2. **图谱管理**: 查看、删除知识图谱
3. **统计信息**: 查看系统统计数据
4. **分类管理**: 管理知识图谱分类

## 项目展示

### 首页展示
![image-20260319213628276](.\photo\屏幕截图 2026-03-19 213623.png)

### 知识图谱广场
### 知识图谱详情

![image-20260319213822933](.\photo\屏幕截图 2026-03-19 213817.png)

![image-20260319214116462](.\photo\屏幕截图 2026-03-19 214111.png)

### AI对话生成

![image-20260319213854191](.\photo\屏幕截图 2026-03-19 213847.png)

### 创建知识图谱

![image-20260319213912765](.\photo\屏幕截图 2026-03-19 213906.png)

## Excel导入格式

### 节点表（Sheet名称：节点）

| 节点名称 | 节点类别 |
|---------|---------|
| JavaScript | 前端 |
| Python | 后端 |

### 关系表（Sheet名称：关系）

| 起点 | 关系 | 终点 |
|-----|------|-----|
| JavaScript | 基于 | Vue |
| Python | 对比 | Java |

## API接口说明

### 用户接口
- POST `/api/user/login` - 用户登录
- POST `/api/user/register` - 用户注册
- GET `/api/user/info/{id}` - 获取用户信息
- POST `/api/user/points/update` - 更新积分
- POST `/api/user/points/deduct` - 扣除积分

### 知识图谱接口
- POST `/api/graph/create` - 创建图谱
- POST `/api/graph/update` - 更新图谱
- DELETE `/api/graph/delete/{id}` - 删除图谱
- GET `/api/graph/{id}` - 获取图谱详情
- GET `/api/graph/public` - 获取公开图谱
- GET `/api/graph/my` - 获取我的图谱
- GET `/api/graph/search` - 搜索图谱
- POST `/api/graph/like` - 点赞图谱
- POST `/api/graph/download` - 下载图谱
- POST `/api/graph/comment` - 添加评论
- GET `/api/graph/hot` - 获取热门图谱

### AI接口
- POST `/api/ai/generate` - 生成知识图谱
- POST `/api/ai/parse` - 解析知识图谱
- POST `/api/ai/chat` - AI对话
- GET `/api/ai/history/{userId}` - 获取对话历史

### 分类接口
- GET `/api/category/list` - 获取所有分类
- GET `/api/category/{id}` - 获取分类详情
- POST `/api/category/create` - 创建分类
- POST `/api/category/update` - 更新分类
- DELETE `/api/category/delete/{id}` - 删除分类

### Excel接口
- POST `/api/excel/export/{graphId}` - 导出Excel
- POST `/api/excel/import/nodes/{graphId}` - 导入节点
- POST `/api/excel/import/edges/{graphId}` - 导入关系

### 后台管理接口
- GET `/api/admin/users` - 获取所有用户
- GET `/api/admin/users/{id}` - 获取用户详情
- DELETE `/api/admin/users/{id}` - 删除用户
- GET `/api/admin/graphs` - 获取所有图谱
- DELETE `/api/admin/graphs/{id}` - 删除图谱
- GET `/api/admin/stats` - 获取系统统计
- GET `/api/admin/hot/graphs` - 获取热门图谱

## 定时任务

系统每天凌晨2点自动清理热点数据：
- 删除超过1天的热点记录
- 保留前3分类和前10知识图谱的热点数据
- 自动创建新的热点记录

## 常见问题

### 1. 后端启动失败
- 检查MySQL是否启动
- 检查数据库连接配置是否正确
- 检查端口8080是否被占用

### 2. 前端启动失败
- 检查Node.js版本是否满足要求
- 删除node_modules重新安装依赖
- 检查端口5175是否被占用

### 3. API调用失败
- 检查后端是否正常启动
- 检查CORS配置
- 查看浏览器控制台错误信息

### 4. AI对话无响应
- 检查智谱AI API密钥是否正确
- 检查网络连接
- 查看后端日志

### 5. Excel导入失败
- 检查Excel文件格式是否正确
- 确保Sheet名称为"节点"和"关系"
- 检查表头是否正

## 许可证

本项目仅供学习和研究使用。

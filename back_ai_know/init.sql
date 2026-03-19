-- AI知识图谱平台数据库初始化脚本
-- 创建时间: 2024-03-18

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_knowledge_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_knowledge_platform;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像URL',
    points INT DEFAULT 0 COMMENT '积分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 知识图谱分类表
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description TEXT COMMENT '分类描述',
    icon VARCHAR(50) COMMENT '分类图标',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱分类表';

-- 知识图谱表
CREATE TABLE IF NOT EXISTS knowledge_graphs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '图谱ID',
    name VARCHAR(100) NOT NULL COMMENT '图谱名称',
    description TEXT COMMENT '图谱描述',
    category_id BIGINT COMMENT '分类ID',
    owner_id BIGINT NOT NULL COMMENT '创建者ID',
    is_public TINYINT(1) DEFAULT 0 COMMENT '是否公开（0-私有，1-公开）',
    cover_image VARCHAR(255) COMMENT '封面图片URL',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    like_count INT DEFAULT 0 COMMENT '点赞次数',
    comment_count INT DEFAULT 0 COMMENT '评论次数',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    is_hot TINYINT(1) DEFAULT 0 COMMENT '是否热门',
    hot_score INT DEFAULT 0 COMMENT '热度分数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_category_id (category_id),
    INDEX idx_owner_id (owner_id),
    INDEX idx_is_public (is_public),
    INDEX idx_is_hot (is_hot),
    INDEX idx_hot_score (hot_score),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱表';

-- 图谱节点表
CREATE TABLE IF NOT EXISTS graph_nodes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '节点ID',
    graph_id BIGINT NOT NULL COMMENT '图谱ID',
    node_id VARCHAR(50) NOT NULL COMMENT '节点ID（图谱内唯一）',
    name VARCHAR(100) NOT NULL COMMENT '节点名称',
    category VARCHAR(50) COMMENT '节点类别',
    properties JSON COMMENT '节点属性（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (graph_id) REFERENCES knowledge_graphs(id) ON DELETE CASCADE,
    UNIQUE KEY uk_graph_node (graph_id, node_id),
    INDEX idx_graph_id (graph_id),
    INDEX idx_node_id (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图谱节点表';

-- 图谱关系表
CREATE TABLE IF NOT EXISTS graph_edges (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
    graph_id BIGINT NOT NULL COMMENT '图谱ID',
    source_node_id VARCHAR(50) NOT NULL COMMENT '起点节点ID',
    target_node_id VARCHAR(50) NOT NULL COMMENT '终点节点ID',
    relation VARCHAR(100) COMMENT '关系名称',
    properties JSON COMMENT '关系属性（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (graph_id) REFERENCES knowledge_graphs(id) ON DELETE CASCADE,
    INDEX idx_graph_id (graph_id),
    INDEX idx_source_node (source_node_id),
    INDEX idx_target_node (target_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图谱关系表';

-- 评论表
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    graph_id BIGINT NOT NULL COMMENT '图谱ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id BIGINT COMMENT '父评论ID（用于回复）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (graph_id) REFERENCES knowledge_graphs(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE,
    INDEX idx_graph_id (graph_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 点赞表
CREATE TABLE IF NOT EXISTS likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    graph_id BIGINT NOT NULL COMMENT '图谱ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (graph_id) REFERENCES knowledge_graphs(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_graph_user (graph_id, user_id),
    INDEX idx_graph_id (graph_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- 积分表
CREATE TABLE IF NOT EXISTS user_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '积分记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    points INT NOT NULL COMMENT '积分变化',
    type ENUM('like', 'download', 'comment', 'create', 'share') NOT NULL COMMENT '积分类型',
    description VARCHAR(255) COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分表';

-- 下载记录表
CREATE TABLE IF NOT EXISTS download_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '下载记录ID',
    graph_id BIGINT NOT NULL COMMENT '图谱ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    points_cost INT DEFAULT 10 COMMENT '消耗积分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (graph_id) REFERENCES knowledge_graphs(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_graph_id (graph_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下载记录表';

-- 热点记录表
CREATE TABLE IF NOT EXISTS hot_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '热点记录ID',
    graph_id BIGINT NOT NULL COMMENT '图谱ID',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    like_count INT DEFAULT 0 COMMENT '点赞次数',
    comment_count INT DEFAULT 0 COMMENT '评论次数',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    record_date DATE NOT NULL COMMENT '记录日期',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (graph_id) REFERENCES knowledge_graphs(id) ON DELETE CASCADE,
    UNIQUE KEY uk_graph_date (graph_id, record_date),
    INDEX idx_record_date (record_date),
    INDEX idx_hot_score (view_count, like_count, comment_count, download_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热点记录表';

-- AI对话历史表（存储在Redis，这里只定义结构）
-- Redis Key: ai:chat:history:{userId}
-- Value: JSON数组，包含对话历史
-- TTL: 2天（172800秒）

-- 初始化分类数据
INSERT INTO categories (name, description, icon, sort_order) VALUES
('编程技术', '编程语言、框架、算法等技术知识', '💻', 1),
('人工智能', 'AI、机器学习、深度学习等', '🤖', 2),
('数据科学', '数据分析、可视化、统计学', '📊', 3),
('其他', '其他领域的知识图谱', '📚', 4);
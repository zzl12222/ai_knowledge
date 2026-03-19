import request from '@/utils/request'

export const graphApi = {
  createGraph(data) {
    return request({
      url: '/graph/create',
      method: 'post',
      data
    })
  },

  createNode(data) {
    return request({
      url: '/graph/node/create',
      method: 'post',
      data
    })
  },

  updateNode(data) {
    return request({
      url: '/graph/node/update',
      method: 'post',
      data
    })
  },

  createEdge(data) {
    return request({
      url: '/graph/edge/create',
      method: 'post',
      data
    })
  },

  updateGraph(data) {
    return request({
      url: '/graph/update',
      method: 'post',
      data
    })
  },

  deleteGraph(id) {
    return request({
      url: `/graph/delete/${id}`,
      method: 'delete'
    })
  },

  getGraphById(id) {
    return request({
      url: `/graph/${id}`,
      method: 'get'
    })
  },

  getNodes(id) {
    return request({
      url: `/graph/${id}/nodes`,
      method: 'get'
    })
  },

  getEdges(id) {
    return request({
      url: `/graph/${id}/edges`,
      method: 'get'
    })
  },

  getPublicGraphs(params) {
    return request({
      url: '/graph/public',
      method: 'get',
      params
    })
  },

  getMyGraphs(params) {
    return request({
      url: '/graph/my',
      method: 'get',
      params
    })
  },

  searchGraphs(params) {
    return request({
      url: '/graph/search',
      method: 'get',
      params
    })
  },

  likeGraph(graphId, userId) {
    return request({
      url: '/graph/like',
      method: 'post',
      data: { graphId, userId }
    })
  },

  unlikeGraph(graphId, userId) {
    return request({
      url: '/graph/unlike',
      method: 'post',
      data: { graphId, userId }
    })
  },

  isLiked(graphId, userId) {
    return request({
      url: '/graph/like/check',
      method: 'get',
      params: { graphId, userId }
    })
  },

  downloadGraph(graphId) {
    return request({
      url: '/graph/download',
      method: 'post',
      data: { graphId }
    })
  },

  addComment(data) {
    return request({
      url: '/graph/comment',
      method: 'post',
      data
    })
  },

  getComments(graphId) {
    return request({
      url: `/graph/comments/${graphId}`,
      method: 'get'
    })
  },

  deleteComment(commentId, userId) {
    return request({
      url: `/graph/comment/${commentId}`,
      method: 'delete',
      params: { userId }
    })
  },

  getHotGraphs(params) {
    return request({
      url: '/graph/hot',
      method: 'get',
      params
    })
  },

  getTopHotGraphs() {
    return request({
      url: '/graph/hot/top',
      method: 'get'
    })
  }
}
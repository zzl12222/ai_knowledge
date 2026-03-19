import request from '@/utils/request'

export const categoryApi = {
  getAllCategories() {
    return request({
      url: '/category/list',
      method: 'get'
    })
  },

  getCategoryById(id) {
    return request({
      url: `/category/${id}`,
      method: 'get'
    })
  },

  createCategory(data) {
    return request({
      url: '/category/create',
      method: 'post',
      data
    })
  },

  updateCategory(data) {
    return request({
      url: '/category/update',
      method: 'post',
      data
    })
  },

  deleteCategory(id) {
    return request({
      url: `/category/delete/${id}`,
      method: 'delete'
    })
  }
}
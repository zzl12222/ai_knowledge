import request from '@/utils/request'

export const userApi = {
  login(data) {
    return request({
      url: '/user/login',
      method: 'post',
      data
    })
  },

  register(data) {
    return request({
      url: '/user/register',
      method: 'post',
      data
    })
  },

  getUserInfo(id) {
    return request({
      url: `/user/info/${id}`,
      method: 'get'
    })
  },

  updatePoints(data) {
    return request({
      url: '/user/points/update',
      method: 'post',
      data
    })
  },

  deductPoints(data) {
    return request({
      url: '/user/points/deduct',
      method: 'post',
      data
    })
  }
}
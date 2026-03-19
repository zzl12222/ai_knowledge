import { defineStore } from 'pinia'
import { ref } from 'vue'
import { userApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const isLoggedIn = ref(false)
  const username = ref('')
  const token = ref('')
  const userId = ref(null)
  const email = ref('')
  const points = ref(0)

  const login = async (loginData) => {
    try {
      const response = await userApi.login(loginData)
      const userData = response.data
      isLoggedIn.value = true
      username.value = userData.username
      token.value = userData.id
      userId.value = userData.id
      email.value = userData.email
      points.value = userData.points
      localStorage.setItem('user', JSON.stringify(userData))
      localStorage.setItem('token', userData.id)
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  const register = async (registerData) => {
    try {
      const response = await userApi.register(registerData)
      const userData = response.data
      isLoggedIn.value = true
      username.value = userData.username
      token.value = userData.id
      userId.value = userData.id
      email.value = userData.email
      points.value = userData.points
      localStorage.setItem('user', JSON.stringify(userData))
      localStorage.setItem('token', userData.id)
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  const logout = () => {
    isLoggedIn.value = false
    username.value = ''
    token.value = ''
    userId.value = null
    email.value = ''
    points.value = 0
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }

  const loadUser = () => {
    const user = localStorage.getItem('user')
    if (user) {
      const userData = JSON.parse(user)
      isLoggedIn.value = true
      username.value = userData.username
      token.value = userData.id
      userId.value = userData.id
      email.value = userData.email
      points.value = userData.points || 0
    }
  }

  const updateUserInfo = async () => {
    if (!userId.value) return
    
    try {
      const response = await userApi.getUserInfo(userId.value)
      const userData = response.data
      username.value = userData.username
      email.value = userData.email
      points.value = userData.points
      localStorage.setItem('user', JSON.stringify(userData))
    } catch (error) {
      console.error('更新用户信息失败:', error)
    }
  }

  return {
    isLoggedIn,
    username,
    token,
    userId,
    email,
    points,
    login,
    register,
    logout,
    loadUser,
    updateUserInfo
  }
})
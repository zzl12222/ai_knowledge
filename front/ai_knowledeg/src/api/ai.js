import request from '@/utils/request'

export const aiApi = {
  generateGraph(data) {
    return request({
      url: '/ai/generate',
      method: 'post',
      data
    })
  },

  parseGraph(data) {
    return request({
      url: '/ai/parse',
      method: 'post',
      data
    })
  },

  chat(data) {
    return request({
      url: '/ai/chat',
      method: 'post',
      data
    })
  },

  chatWithAutoSave(data) {
    return request({
      url: '/ai/chat/auto',
      method: 'post',
      data
    })
  },

  chatNonStream(data) {
    return request({
      url: '/ai/chat/non-stream',
      method: 'post',
      data
    })
  },

  chatStream(data, onMessage, onComplete, onError) {
    const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8987/api'
    const url = `${baseURL}/ai/chat/stream`
    
    console.log('开始流式请求:', url, data)
    
    return fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
      },
      body: JSON.stringify(data)
    }).then(response => {
      console.log('响应状态:', response.status, response.statusText)
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      function read() {
        reader.read().then(({ done, value }) => {
          if (done) {
            console.log('流式响应完成')
            if (onComplete) onComplete()
            return
          }

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            console.log('收到行:', JSON.stringify(line))
            if (line.startsWith('data: ')) {
              const data = line.substring(6)
              console.log('解析数据:', JSON.stringify(data), '数据长度:', data.length)
              try {
                const event = new MessageEvent('message', { data })
                console.log('触发onMessage回调，event.data:', JSON.stringify(event.data))
                if (onMessage) onMessage(event)
              } catch (e) {
                console.error('解析SSE数据失败:', e)
              }
            }
          }

          read()
        }).catch(error => {
          console.error('读取流数据失败:', error)
          if (onError) onError(error)
        })
      }

      read()
    }).catch(error => {
      console.error('流式请求失败:', error)
      if (onError) onError(error)
    })
  },

  getHistory(userId) {
    return request({
      url: `/ai/history/${userId}`,
      method: 'get'
    })
  },

  clearHistory(userId) {
    return request({
      url: `/ai/history/${userId}`,
      method: 'delete'
    })
  }
}
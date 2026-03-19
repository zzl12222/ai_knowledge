import request from '@/utils/request'

export const excelApi = {
  exportGraph(graphId) {
    return request({
      url: `/excel/export/${graphId}`,
      method: 'get',
      responseType: 'blob'
    })
  },

  importNodes(graphId, file) {
    const formData = new FormData()
    formData.append('file', file)
    return request({
      url: `/excel/import/nodes/${graphId}`,
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  importEdges(graphId, file) {
    const formData = new FormData()
    formData.append('file', file)
    return request({
      url: `/excel/import/edges/${graphId}`,
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}
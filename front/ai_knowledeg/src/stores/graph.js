import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useGraphStore = defineStore('graph', () => {
  const graphs = ref([])
  const currentGraph = ref(null)

  const loadGraphs = () => {
    const savedGraphs = localStorage.getItem('graphs')
    if (savedGraphs) {
      graphs.value = JSON.parse(savedGraphs)
    }
  }

  const saveGraphs = () => {
    localStorage.setItem('graphs', JSON.stringify(graphs.value))
  }

  const addGraph = (graph) => {
    graphs.value.push(graph)
    saveGraphs()
  }

  const updateGraph = (id, graphData) => {
    const index = graphs.value.findIndex(g => g.id === id)
    if (index !== -1) {
      graphs.value[index] = { ...graphs.value[index], ...graphData }
      saveGraphs()
    }
  }

  const deleteGraph = (id) => {
    graphs.value = graphs.value.filter(g => g.id !== id)
    saveGraphs()
  }

  const getGraph = (id) => {
    return graphs.value.find(g => g.id === id)
  }

  const setCurrentGraph = (graph) => {
    currentGraph.value = graph
  }

  const getPublicGraphs = () => {
    return graphs.value.filter(g => g.isPublic)
  }

  const getMyGraphs = (username) => {
    return graphs.value.filter(g => g.owner === username)
  }

  return {
    graphs,
    currentGraph,
    loadGraphs,
    saveGraphs,
    addGraph,
    updateGraph,
    deleteGraph,
    getGraph,
    setCurrentGraph,
    getPublicGraphs,
    getMyGraphs
  }
})
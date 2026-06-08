import { defineStore } from 'pinia'
import axios from 'axios'

export const useConfigStore = defineStore('config', {
  state: () => ({
    workspaceRoot: null,
    configured: false,
    loading: false
  }),

  actions: {
    async checkStatus() {
      this.loading = true
      try {
        const res = await axios.get('/api/config/status')
        if (res.data.code === 200) {
          this.configured = res.data.data?.configured || false
          this.workspaceRoot = res.data.data?.workspaceRoot || null
        }
      } catch (e) {
        console.error('配置检查失败', e)
      } finally {
        this.loading = false
      }
    },

    async setWorkspaceRoot(path) {
      await axios.put('/api/config/workspace-root', { workspaceRoot: path })
      await this.checkStatus()
    }
  }
})

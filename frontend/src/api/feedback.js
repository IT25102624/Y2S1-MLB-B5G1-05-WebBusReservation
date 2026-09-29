import client from './client'

/**
 * Feedback & Complaints API Service
 */
export const feedbackApi = {
  // Get all feedback (Admin / Customer Service)
  getAll: async () => {
    const res = await client.get('/feedback')
    return res.data
  },

  // Get feedback by user ID
  getByUserId: async (userId) => {
    const res = await client.get(`/feedback/user/${userId}`)
    return res.data
  },

  // Get feedback statistics
  getStats: async () => {
    const res = await client.get('/feedback/stats')
    return res.data
  },

  // Create feedback or complaint
  create: async (payload) => {
    // payload: { name, type: 'feedback' | 'complain', description, rating, userId }
    const res = await client.post('/feedback', payload)
    return res.data
  },

  // Update existing feedback
  update: async (id, payload) => {
    const res = await client.put(`/feedback/${id}`, payload)
    return res.data
  },

  // Delete feedback
  delete: async (id, userId) => {
    const res = await client.delete(`/feedback/${id}`, {
      params: userId ? { userId } : {}
    })
    return res.data
  },

  // Reply to complaint (Customer Service / Admin)
  reply: async (id, replyText) => {
    const res = await client.put(`/feedback/${id}/reply`, { reply: replyText })
    return res.data
  }
}

export default feedbackApi

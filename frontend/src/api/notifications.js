import client from './client'

/**
 * Real-Time Website Notifications & Alert Service
 */
export const notificationsApi = {
  // Get all notifications across the platform
  getAll: async () => {
    const res = await client.get('/notifications')
    return res.data || []
  },

  // Create a custom notification
  create: async (data) => {
    const res = await client.post('/notifications', data)
    return res.data
  },

  // Get all notifications for user by NIC
  getAllByUser: async (nic) => {
    const res = await client.get(`/notifications/user/${encodeURIComponent(nic)}`)
    return res.data || []
  },

  // Get unread notifications
  getUnreadByUser: async (nic) => {
    const res = await client.get(`/notifications/user/${encodeURIComponent(nic)}/unread`)
    return res.data || []
  },

  // Get unread count
  getUnreadCount: async (nic) => {
    if (nic) {
      const res = await client.get(`/notifications/user/${encodeURIComponent(nic)}/count`)
      return res.data?.unreadCount || 0
    }
    const res = await client.get('/notifications/count')
    return res.data?.unreadCount || 0
  },

  // Mark single notification as read
  markAsRead: async (id) => {
    const res = await client.put(`/notifications/${id}/read`)
    return res.data
  },

  // Mark all notifications as read
  markAllAsRead: async (nic) => {
    if (nic) {
      const res = await client.put(`/notifications/user/${encodeURIComponent(nic)}/read-all`)
      return res.data
    }
    const res = await client.put('/notifications/read-all')
    return res.data
  }
}

export default notificationsApi

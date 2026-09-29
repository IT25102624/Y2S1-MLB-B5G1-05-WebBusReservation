import client from './client'

/**
 * Booking / E-ticket API Service
 * Used to link luggage tags to passenger's confirmed e-tickets
 */
export const bookingsApi = {
  // Get all bookings (for staff)
  getAll: async () => {
    const res = await client.get('/bookings')
    return res.data
  },

  // Get bookings by passenger NIC
  getByUserNic: async (nic) => {
    const res = await client.get(`/bookings/user/${encodeURIComponent(nic)}`)
    return res.data
  },

  // Get booking by ID
  getById: async (id) => {
    const res = await client.get(`/bookings/${id}`)
    return res.data
  },

  // Create booking (if booking directly)
  create: async (payload) => {
    const res = await client.post('/bookings', payload)
    return res.data
  }
}

export default bookingsApi

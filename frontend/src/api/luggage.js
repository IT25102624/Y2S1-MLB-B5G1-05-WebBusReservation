import client from './client'

/**
 * Luggage Management API Service
 */
export const luggageApi = {
  // Get all luggage records (Admin / Conductor)
  getAll: async () => {
    const res = await client.get('/luggage')
    return res.data
  },

  // Get luggage records by Passenger NIC
  getByUserNic: async (nic) => {
    const res = await client.get(`/luggage/user/${encodeURIComponent(nic)}`)
    return res.data
  },

  // Get luggage records linked to a specific booking ID
  getByBooking: async (bookingId) => {
    const res = await client.get(`/luggage/booking/${bookingId}`)
    return res.data
  },

  // Track luggage by unique tag number (Public / Passenger tracker)
  getByTag: async (tagNumber) => {
    const res = await client.get(`/luggage/tag/${encodeURIComponent(tagNumber)}`)
    return res.data
  },

  // Create new luggage tag linked to E-ticket
  create: async ({ bookingId, bagQuantity, description }) => {
    const res = await client.post('/luggage', {
      bookingId: Number(bookingId),
      bagQuantity: Number(bagQuantity),
      description: description || ''
    })
    return res.data
  },

  // Update status (TAGGED, CHECKED_IN, LOADED, COLLECTED)
  updateStatus: async (id, status) => {
    const res = await client.put(`/luggage/${id}/status`, { status })
    return res.data
  },

  // Update luggage record details
  update: async (id, { bagQuantity, description, status }) => {
    const res = await client.put(`/luggage/${id}`, {
      bagQuantity: Number(bagQuantity),
      description,
      status
    })
    return res.data
  },

  // Delete luggage record
  delete: async (id) => {
    const res = await client.delete(`/luggage/${id}`)
    return res.data
  }
}

export default luggageApi

import axios from 'axios'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: { 'Content-Type': 'application/json' }
})

// Attach the JWT (if present) to every request. Guest/public endpoints
// simply ignore the absence of this header, so browsing/booking without an
// account keeps working (login is always optional for passengers).
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('rl_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('rl_token')
      localStorage.removeItem('rl_user')
    }
    return Promise.reject(err)
  }
)

export default client

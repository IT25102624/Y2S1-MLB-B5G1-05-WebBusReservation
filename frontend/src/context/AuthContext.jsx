import React, { createContext, useContext, useEffect, useState, useCallback } from 'react'
import client from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('rl_user')
    return raw ? JSON.parse(raw) : null
  })
  const [sessionDashboard, setSessionDashboard] = useState(() => {
    const raw = localStorage.getItem('rl_dashboard')
    return raw ? JSON.parse(raw) : null
  })

  useEffect(() => {
    if (user) localStorage.setItem('rl_user', JSON.stringify(user))
  }, [user])

  const applySession = useCallback((authResponse) => {
    localStorage.setItem('rl_token', authResponse.accessToken)
    localStorage.setItem('rl_user', JSON.stringify(authResponse.user))
    if (authResponse.dashboard) localStorage.setItem('rl_dashboard', JSON.stringify(authResponse.dashboard))
    setSessionDashboard(authResponse.dashboard || null)
    setUser(authResponse.user)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('rl_token')
    localStorage.removeItem('rl_user')
    localStorage.removeItem('rl_dashboard')
    setSessionDashboard(null)
    setUser(null)
  }, [])

  // --- Passenger (optional) ---
  const registerPassenger = async (payload) => {
    const { data } = await client.post('/auth/passenger/register', payload)
    applySession(data)
    return data
  }
  const loginPassenger = async (payload) => {
    const { data } = await client.post('/auth/passenger/login', payload)
    applySession(data)
    return data
  }

  // --- Staff (Conductor / Driver) ---
  const loginStaff = async (payload) => {
    const { data } = await client.post('/auth/staff/login', payload)
    applySession(data)
    return data
  }

  // --- Admin ---
  const loginAdmin = async (payload) => {
    const { data } = await client.post('/auth/admin/login', payload)
    applySession(data)
    return data
  }

  return (
    <AuthContext.Provider value={{ user, sessionDashboard, registerPassenger, loginPassenger, loginStaff, loginAdmin, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)

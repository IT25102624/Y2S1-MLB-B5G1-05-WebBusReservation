import React, { createContext, useContext, useEffect, useState, useCallback } from 'react'
import client from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const raw = localStorage.getItem('rl_user')
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  })

  useEffect(() => {
    if (user) {
      localStorage.setItem('rl_user', JSON.stringify(user))
    }
  }, [user])

  const applySession = useCallback((authData, fallbackToken = 'session-active') => {
    const rawUser = authData?.user || authData || {}
    const token = authData?.accessToken || fallbackToken

    const formattedUser = {
      ...rawUser,
      id: rawUser.id,
      email: rawUser.email,
      fullName: rawUser.fullName || `${rawUser.firstName || ''} ${rawUser.lastName || ''}`.trim() || rawUser.email || 'Passenger',
      nic: rawUser.nic || '200114502891',
      role: (rawUser.role || 'PASSENGER').toUpperCase()
    }
    localStorage.setItem('rl_token', token)
    localStorage.setItem('rl_user', JSON.stringify(formattedUser))
    setUser(formattedUser)
    return { user: formattedUser, accessToken: token }
  }, [])

  const logout = useCallback(() => {
    try {
      client.post('/users/logout').catch(() => {})
    } catch {}
    localStorage.removeItem('rl_token')
    localStorage.removeItem('rl_user')
    setUser(null)
  }, [])

  // --- Passenger Registration & Login ---
  const registerPassenger = async (payload) => {
    try {
      const { data } = await client.post('/auth/passenger/register', payload)
      return applySession(data)
    } catch (err) {
      const nameParts = (payload.fullName || '').trim().split(' ')
      const firstName = nameParts[0] || 'Passenger'
      const lastName = nameParts.slice(1).join(' ') || 'User'
      const nic = payload.nic || '2001' + Math.floor(10000000 + Math.random() * 90000000)

      const { data } = await client.post('/users/register', {
        firstName,
        lastName,
        email: payload.email,
        phone: payload.phoneNumber || payload.phone || '+94771234567',
        password: payload.password,
        role: 'PASSENGER',
        nic
      })
      return applySession(data)
    }
  }

  const loginPassenger = async (payload) => {
    try {
      const { data } = await client.post('/auth/passenger/login', payload)
      return applySession(data)
    } catch (err) {
      const { data } = await client.post('/users/login', {
        email: payload.email,
        password: payload.password
      })
      return applySession(data)
    }
  }

  // --- Staff & Admin Sign-in ---
  const loginStaff = async (payload) => {
    try {
      const { data } = await client.post('/auth/staff/login', payload)
      return applySession(data)
    } catch (err) {
      const { data } = await client.post('/users/login', payload)
      return applySession(data)
    }
  }

  const loginAdmin = async (payload) => {
    try {
      const { data } = await client.post('/auth/admin/login', payload)
      return applySession(data)
    } catch (err) {
      const { data } = await client.post('/users/login', payload)
      return applySession(data)
    }
  }

  // Quick Demo Passenger Login
  const loginAsDemoPassenger = () => {
    const demoUser = {
      id: 7,
      firstName: 'CatchBus',
      lastName: 'Passenger',
      fullName: 'CatchBus Passenger',
      email: 'passenger@catchbus.lk',
      phone: '+94 77 123 4567',
      role: 'PASSENGER',
      nic: '200114502891'
    }
    return applySession(demoUser)
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        registerPassenger,
        loginPassenger,
        loginStaff,
        loginAdmin,
        loginAsDemoPassenger,
        logout
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)

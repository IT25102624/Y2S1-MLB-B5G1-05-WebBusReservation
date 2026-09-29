import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

/** Gate a route to signed-in users with one of `roles`. Redirects otherwise. */
export default function ProtectedRoute({ roles, children, redirectTo = '/' }) {
  const { user } = useAuth()
  if (!user) return <Navigate to={redirectTo} replace />
  if (roles && !roles.includes(user.role)) return <Navigate to={redirectTo} replace />
  return children
}

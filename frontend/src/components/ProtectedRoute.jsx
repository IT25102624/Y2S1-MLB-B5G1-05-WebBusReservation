import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

/** Gate a route to signed-in users with one of `roles`. Redirects otherwise. */
export default function ProtectedRoute({ roles, children, redirectTo = '/' }) {
  const { user, logout } = useAuth()
  if (!user) return <Navigate to={redirectTo} replace />
  if (roles && !roles.includes(user.role)) {
    return (
      <div className="grid min-h-[70vh] place-items-center bg-ops-950 px-6">
        <div className="max-w-md rounded-3xl border border-signal-rose/30 bg-ops-800 p-8 text-center">
          <h1 className="font-display text-2xl font-bold text-white">Access restricted</h1>
          <p className="mt-3 text-sm text-slate-400">This interface is not available for your account. Please sign out before signing in with another role.</p>
          <button onClick={logout} className="mt-6 rounded-xl bg-signal-cyan px-5 py-3 font-bold text-ops-950">Sign out</button>
        </div>
      </div>
    )
  }
  return children
}

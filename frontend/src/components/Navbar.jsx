import React from 'react'
import { Link, NavLink } from 'react-router-dom'
import Logo from './Logo.jsx'
import NotificationBell from './NotificationBell.jsx'

export default function Navbar() {
  const linkClass = ({ isActive }) =>
    `text-sm font-bold transition px-3 py-1.5 rounded-lg ${
      isActive
        ? 'text-brand-600 bg-brand-50'
        : 'text-ink-800 hover:text-brand-600 hover:bg-sand-50'
    }`

  return (
    <header className="sticky top-0 z-40 border-b border-ink-900/5 bg-white/95 backdrop-blur-md">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-3.5">
        <Link to="/" className="flex items-center gap-3">
          <Logo className="h-8 w-auto text-brand-500" />
        </Link>

        {/* Primary Navigation */}
        <nav className="flex items-center gap-1 sm:gap-2">
          <NavLink to="/luggage" className={linkClass}>
            🏷️ Luggage Tagging
          </NavLink>
          <NavLink to="/notifications" className={linkClass}>
            🔔 Alerts Hub
          </NavLink>
          <NavLink to="/feedback" className={linkClass}>
            ⭐ Reviews &amp; Feedback
          </NavLink>
          <NavLink to="/admin/feedback" className={linkClass}>
            🛡️ Customer Care
          </NavLink>
        </nav>

        {/* Action / Notification Bell */}
        <div className="flex items-center gap-2">
          <NotificationBell />
          <Link
            to="/luggage"
            className="hidden sm:inline-flex rounded-xl bg-brand-500 px-3.5 py-2 text-xs font-bold text-white shadow-card hover:bg-brand-600 transition"
          >
            + Tag Luggage
          </Link>
        </div>
      </div>
    </header>
  )
}

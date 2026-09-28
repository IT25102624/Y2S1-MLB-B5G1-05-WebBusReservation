import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import Logo from './Logo.jsx'

export default function Navbar() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const [notice, setNotice] = useState('')
  const navigate = useNavigate()
  const isStaff = user?.role === 'CONDUCTOR' || user?.role === 'DRIVER'

  const showUnavailable = () => {
    setNotice('This feature is not available for your staff account.')
    setOpen(false)
    window.setTimeout(() => setNotice(''), 3500)
  }

  const renderNavItem = (label, to) => (
    isStaff
      ? <button type="button" onClick={showUnavailable} className="hover:text-brand-600 transition-colors">{label}</button>
      : <Link to={to} className="hover:text-brand-600 transition-colors">{label}</Link>
  )

  return (
    <header className="sticky top-0 z-40 border-b border-ink-900/8 bg-white/85 backdrop-blur-xl">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <Link to="/"><Logo /></Link>

        <nav className="hidden items-center gap-8 text-sm font-semibold text-ink-800/80 md:flex">
          {renderNavItem('Book a trip', '/')}
          {renderNavItem('Destinations', '/destinations')}
          {renderNavItem('Staff & Admin', '/staff/login')}
        </nav>

        <div className="flex items-center gap-3">
          {user ? (
            <div className="flex items-center gap-3">
              {user.role === 'PASSENGER' && (
                <Link to="/passenger" className="hidden rounded-full border border-brand-500/30 px-4 py-2 text-sm font-semibold text-brand-700 sm:inline-block">
                  My trips
                </Link>
              )}
              <span className="hidden text-right sm:block">
                <span className="block text-[11px] text-ink-700/50">Signed in as</span>
                <span className="block text-sm font-semibold text-ink-900">{user.fullName}</span>
              </span>
              <button
                onClick={() => { logout(); navigate('/') }}
                className="rounded-full border border-ink-900/15 px-4 py-2 text-sm font-semibold text-ink-800 transition hover:border-signal-rose/60 hover:text-signal-rose"
              >
                Sign out
              </button>
            </div>
          ) : (
            <div className="hidden gap-2 sm:flex">
              <Link to="/login" className="rounded-full px-4 py-2 text-sm font-semibold text-ink-800 hover:text-brand-600">
                Log in
              </Link>
              <Link
                to="/register"
                className="rounded-full bg-brand-500 px-4 py-2 text-sm font-bold text-white shadow-card transition hover:bg-brand-600"
              >
                Sign up
              </Link>
            </div>
          )}
          <button className="md:hidden text-ink-800" onClick={() => setOpen((o) => !o)} aria-label="Toggle menu">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M4 6h16M4 12h16M4 18h16" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/></svg>
          </button>
        </div>
      </div>

      {open && (
        <div className="border-t border-ink-900/8 bg-white px-6 py-4 md:hidden">
          <div className="flex flex-col gap-3 text-sm font-semibold text-ink-800">
            {isStaff ? (
              <>
                <button type="button" onClick={showUnavailable} className="text-left">Book a trip</button>
                <button type="button" onClick={showUnavailable} className="text-left">Destinations</button>
              </>
            ) : (
              <>
                <Link to="/" onClick={() => setOpen(false)}>Book a trip</Link>
                <Link to="/destinations" onClick={() => setOpen(false)}>Destinations</Link>
              </>
            )}
            <Link to="/login" onClick={() => setOpen(false)}>Log in</Link>
            <Link to="/register" onClick={() => setOpen(false)}>Sign up</Link>
            {isStaff
              ? <button type="button" onClick={showUnavailable} className="text-left">Staff &amp; Admin</button>
              : <Link to="/staff/login" onClick={() => setOpen(false)}>Staff &amp; Admin</Link>}
          </div>
        </div>
      )}
      {notice && (
        <div role="status" aria-live="polite" className="border-t border-amber-200 bg-amber-50 px-6 py-2 text-center text-sm font-semibold text-amber-800">
          {notice}
        </div>
      )}
    </header>
  )
}

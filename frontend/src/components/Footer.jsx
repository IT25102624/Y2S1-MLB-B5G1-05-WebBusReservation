import React from 'react'
import { Link } from 'react-router-dom'
import Logo from './Logo.jsx'
import { cities } from '../data/destinations.js'

export default function Footer() {
  return (
    <footer className="border-t border-ink-900/10 bg-white">
      <div className="mx-auto max-w-6xl px-6 py-14">
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <Logo />
            <p className="mt-4 max-w-xs text-sm text-ink-700/70 leading-relaxed">
              Sri Lanka's premier online bus ticket and baggage tracking platform — search, book,
              and travel safely across the island.
            </p>
          </div>

          <div>
            <h4 className="font-display text-sm font-bold uppercase tracking-wide text-ink-900">Popular Destinations</h4>
            <ul className="mt-4 space-y-2 text-sm text-ink-700/70">
              {cities.slice(0, 6).map((c) => (
                <li key={c}>
                  <Link to={`/schedule?from=Colombo&to=${encodeURIComponent(c)}`} className="hover:text-brand-600 transition">{c}</Link>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h4 className="font-display text-sm font-bold uppercase tracking-wide text-ink-900">Passenger Services</h4>
            <ul className="mt-4 space-y-2 text-sm text-ink-700/70">
              <li><Link to="/luggage" className="hover:text-brand-600 transition">🏷️ Tag &amp; Track Luggage</Link></li>
              <li><Link to="/feedback" className="hover:text-brand-600 transition">⭐ Passenger Feedback &amp; Reviews</Link></li>
              <li><Link to="/notifications" className="hover:text-brand-600 transition">🔔 Live Booking &amp; Luggage Alerts</Link></li>
              <li><Link to="/admin/feedback" className="hover:text-brand-600 transition">🛡️ Customer Service Resolution</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="font-display text-sm font-bold uppercase tracking-wide text-ink-900">Accounts &amp; Portals</h4>
            <ul className="mt-4 space-y-2 text-sm text-ink-700/70">
              <li><Link to="/staff/login" className="hover:text-brand-600 transition">Staff &amp; Admin Portal</Link></li>
              <li><Link to="/register" className="hover:text-brand-600 transition">Create Passenger Account</Link></li>
              <li><Link to="/login" className="hover:text-brand-600 transition">Passenger Sign-in</Link></li>
            </ul>
          </div>
        </div>

        <div className="mt-12 flex flex-col items-center justify-between gap-4 border-t border-ink-900/10 pt-6 text-xs text-ink-700/60 sm:flex-row">
          <span>© {new Date().getFullYear()} CatchBus LK. Sri Lanka Intercity Highway Transport. All rights reserved.</span>
          <span>Verified Secure Passenger &amp; Cargo Network</span>
        </div>
      </div>
    </footer>
  )
}

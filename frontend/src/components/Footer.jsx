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
            <p className="mt-4 max-w-xs text-sm text-ink-700/70">
              Sri Lanka's online bus ticket reservation platform — search, book,
              and travel without ever standing in a queue.
            </p>
          </div>

          <div>
            <h4 className="font-display text-sm font-bold uppercase tracking-wide text-ink-900">Popular cities</h4>
            <ul className="mt-4 space-y-2 text-sm text-ink-700/70">
              {cities.slice(0, 6).map((c) => (
                <li key={c}>
                  <Link to={`/schedule?from=Colombo&to=${encodeURIComponent(c)}`} className="hover:text-brand-600">{c}</Link>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h4 className="font-display text-sm font-bold uppercase tracking-wide text-ink-900">Company</h4>
            <ul className="mt-4 space-y-2 text-sm text-ink-700/70">
              <li><Link to="/" className="hover:text-brand-600">About CatchBusLK</Link></li>
              <li><Link to="/" className="hover:text-brand-600">Cancellation policy</Link></li>
              <li><Link to="/" className="hover:text-brand-600">Help centre</Link></li>
              <li><Link to="/" className="hover:text-brand-600">Contact us</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="font-display text-sm font-bold uppercase tracking-wide text-ink-900">For operators</h4>
            <ul className="mt-4 space-y-2 text-sm text-ink-700/70">
              <li><Link to="/staff/login" className="hover:text-brand-600">Staff &amp; admin portal</Link></li>
              <li><Link to="/register" className="hover:text-brand-600">Create a passenger account</Link></li>
            </ul>
          </div>
        </div>

        <div className="mt-12 flex flex-col items-center justify-between gap-4 border-t border-ink-900/10 pt-6 text-xs text-ink-700/60 sm:flex-row">
          <span>© {new Date().getFullYear()} CatchBusLK. All rights reserved.</span>
          <span>Made for SLIIT SE2030 — Web-based Bus Ticket Reservation System</span>
        </div>
      </div>
    </footer>
  )
}

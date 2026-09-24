import React from 'react'

/** Reusable ticket-stub shaped card used across passenger surfaces. */
export default function TicketCard({ children, className = '' }) {
  return (
    <div className={`ticket-edge ticket-edge-light rounded-2xl border border-ink-900/8 bg-white shadow-card-lg ${className}`}>
      {children}
    </div>
  )
}

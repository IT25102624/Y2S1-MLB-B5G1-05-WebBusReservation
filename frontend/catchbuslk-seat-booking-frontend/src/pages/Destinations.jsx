import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { allRoutes } from '../data/destinations.js'
import { RouteBusIcon } from '../components/BusArt.jsx'

/**
 * Full destinations listing, in the spirit of FlixBus's "Destinations" page —
 * every known route, filterable by a From/To search, each linking through to
 * /schedule with the from/to pre-filled.
 */
export default function Destinations() {
  const [fromQuery, setFromQuery] = useState('')
  const [toQuery, setToQuery] = useState('')
  const navigate = useNavigate()

  const filtered = useMemo(() => {
    const from = fromQuery.trim().toLowerCase()
    const to = toQuery.trim().toLowerCase()
    return allRoutes.filter((r) => {
      const matchesFrom = !from || r.from.toLowerCase().includes(from)
      const matchesTo = !to || r.to.toLowerCase().includes(to)
      return matchesFrom && matchesTo
    })
  }, [fromQuery, toQuery])

  return (
    <div className="mx-auto max-w-6xl px-6 py-16">
      <span className="text-xs font-bold uppercase tracking-widest text-brand-600">Network</span>
      <h1 className="mt-1 font-display text-4xl font-extrabold text-ink-950">All destinations</h1>
      <p className="mt-2 max-w-xl text-ink-700/70">
        Every route currently on the CatchBusLK network. Search a "from" and "to" to narrow the list,
        or open any route to see today's departure times.
      </p>

      <div className="mt-8 flex flex-col gap-3 sm:flex-row">
        <label className="relative flex-1">
          <span className="sr-only">From</span>
          <svg
            className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-700/40"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-4.35-4.35M11 19a8 8 0 100-16 8 8 0 000 16z" />
          </svg>
          <input
            type="text"
            value={fromQuery}
            onChange={(e) => setFromQuery(e.target.value)}
            placeholder="From (e.g. Colombo)"
            className="w-full rounded-full border border-ink-900/10 bg-white py-2.5 pl-11 pr-4 text-sm font-semibold text-ink-950 shadow-card outline-none transition placeholder:font-normal placeholder:text-ink-700/40 focus:border-brand-500/50"
          />
        </label>
        <label className="relative flex-1">
          <span className="sr-only">To</span>
          <svg
            className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-700/40"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-4.35-4.35M11 19a8 8 0 100-16 8 8 0 000 16z" />
          </svg>
          <input
            type="text"
            value={toQuery}
            onChange={(e) => setToQuery(e.target.value)}
            placeholder="To (e.g. Galle)"
            className="w-full rounded-full border border-ink-900/10 bg-white py-2.5 pl-11 pr-4 text-sm font-semibold text-ink-950 shadow-card outline-none transition placeholder:font-normal placeholder:text-ink-700/40 focus:border-brand-500/50"
          />
        </label>
        {(fromQuery || toQuery) && (
          <button
            type="button"
            onClick={() => {
              setFromQuery('')
              setToQuery('')
            }}
            className="shrink-0 rounded-full border border-ink-900/10 px-5 py-2.5 text-sm font-semibold text-ink-700/70 transition hover:border-brand-500/40 hover:text-brand-600"
          >
            Clear
          </button>
        )}
      </div>

      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {filtered.map((r) => (
          <button
            key={`${r.from}-${r.to}`}
            onClick={() => navigate(`/schedule?from=${encodeURIComponent(r.from)}&to=${encodeURIComponent(r.to)}`)}
            className="group flex items-center gap-4 rounded-2xl border border-ink-900/8 bg-white p-4 text-left shadow-card transition hover:-translate-y-0.5 hover:shadow-card-lg"
          >
            <RouteBusIcon className="h-10 w-14 shrink-0 text-brand-500 group-hover:text-brand-600" />
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2 font-bold text-ink-950">
                <span className="truncate">{r.from}</span>
                <span className="route-dash flex-1 text-ink-300" />
                <span className="truncate">{r.to}</span>
              </div>
            </div>
          </button>
        ))}

        {filtered.length === 0 && (
          <p className="col-span-full py-12 text-center text-ink-700/50">No routes found. Try a different search.</p>
        )}
      </div>
    </div>
  )
}

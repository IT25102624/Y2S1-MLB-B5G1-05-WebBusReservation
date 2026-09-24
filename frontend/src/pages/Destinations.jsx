import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { allRoutes, cities } from '../data/destinations.js'
import { RouteBusIcon } from '../components/BusArt.jsx'

/**
 * Full destinations listing, in the spirit of FlixBus's "Destinations" page —
 * every known route, filterable by city, each linking through to /schedule
 * with the from/to pre-filled.
 */
export default function Destinations() {
  const [cityFilter, setCityFilter] = useState('All')
  const navigate = useNavigate()

  const filtered = useMemo(() => {
    if (cityFilter === 'All') return allRoutes
    return allRoutes.filter((r) => r.from === cityFilter || r.to === cityFilter)
  }, [cityFilter])

  return (
    <div className="mx-auto max-w-6xl px-6 py-16">
      <span className="text-xs font-bold uppercase tracking-widest text-brand-600">Network</span>
      <h1 className="mt-1 font-display text-4xl font-extrabold text-ink-950">All destinations</h1>
      <p className="mt-2 max-w-xl text-ink-700/70">
        Every route currently on the CatchBusLK network. Pick a city to narrow the list, or open any
        route to see today's departure times.
      </p>

      <div className="mt-8 flex flex-wrap gap-2">
        {['All', ...cities].map((c) => (
          <button
            key={c}
            onClick={() => setCityFilter(c)}
            className={`rounded-full border px-4 py-1.5 text-sm font-semibold transition ${
              cityFilter === c
                ? 'border-brand-500 bg-brand-500 text-white'
                : 'border-ink-900/10 text-ink-700/70 hover:border-brand-500/40 hover:text-brand-600'
            }`}
          >
            {c}
          </button>
        ))}
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
              <div className="mt-1 flex items-center justify-between text-xs text-ink-700/60">
                <span>{r.duration}</span>
                <span className="font-bold text-brand-600">{r.price}</span>
              </div>
            </div>
          </button>
        ))}

        {filtered.length === 0 && (
          <p className="col-span-full py-12 text-center text-ink-700/50">No routes found for {cityFilter} yet.</p>
        )}
      </div>
    </div>
  )
}

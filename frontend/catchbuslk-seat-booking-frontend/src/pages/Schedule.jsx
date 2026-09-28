import React, { useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { generateMockSchedule, cities } from '../data/destinations.js'

/**
 * Shows bus departure times for a chosen route (from/to passed as query
 * params, e.g. /schedule?from=Colombo&to=Kandy). Trip data is mocked here —
 * this is the page where the Search/Schedule module's real /api/schedules
 * response would slot in, keeping the same {trips: [...]} shape.
 */
export default function Schedule() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const from = params.get('from') || ''
  const to = params.get('to') || ''
  const [date, setDate] = useState(params.get('date') || '')

  const { route, trips } = useMemo(() => generateMockSchedule(from, to), [from, to])

  const swap = () => setParams({ from: to, to: from, date })

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <Link to="/destinations" className="text-sm font-semibold text-ink-700/60 hover:text-brand-600">← All destinations</Link>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-4">
        <div>
          <span className="text-xs font-bold uppercase tracking-widest text-brand-600">View Times</span>
          <h1 className="mt-1 flex items-center gap-3 font-display text-3xl font-extrabold text-ink-950">
            {route.from} <span className="text-ink-300">→</span> {route.to}
          </h1>
          <p className="mt-1 text-sm text-ink-700/60">{route.duration} average journey · {trips.length} departures today</p>
        </div>
        <button onClick={swap} className="rounded-full border border-ink-900/10 px-4 py-2 text-sm font-semibold text-ink-800 hover:border-brand-500/40 hover:text-brand-600">
          ⇄ Swap direction
        </button>
      </div>

      {/* Quick re-search bar */}
      <div className="mt-6 grid gap-2 rounded-2xl border border-ink-900/8 bg-white p-3 shadow-card sm:grid-cols-[1fr_1fr_1fr_auto]">
        <select
          value={from}
          onChange={(e) => setParams({ from: e.target.value, to, date })}
          className="rounded-xl border border-ink-900/10 bg-sand-50 px-3 py-2.5 font-semibold text-ink-900"
        >
          {cities.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        <select
          value={to}
          onChange={(e) => setParams({ from, to: e.target.value, date })}
          className="rounded-xl border border-ink-900/10 bg-sand-50 px-3 py-2.5 font-semibold text-ink-900"
        >
          {cities.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        <input
          type="date"
          value={date}
          onChange={(e) => { setDate(e.target.value); setParams({ from, to, date: e.target.value }) }}
          className="rounded-xl border border-ink-900/10 bg-sand-50 px-3 py-2.5 text-ink-900"
        />
        <button
          onClick={() => setParams({ from, to, date })}
          className="rounded-xl bg-brand-500 px-5 py-2.5 font-bold text-white hover:bg-brand-600"
        >
          Update
        </button>
      </div>

      {/* Trip list */}
      <div className="mt-8 space-y-3">
        {trips.map((t) => (
          <div key={t.id} className="rounded-2xl border border-ink-900/8 bg-white p-5 shadow-card transition hover:shadow-card-lg">
            {/* Departure time + operator */}
            <div className="flex flex-wrap items-center gap-3">
              <button
                onClick={() =>
                  navigate(
                    `/direct-booking?from=${encodeURIComponent(route.from)}&to=${encodeURIComponent(route.to)}&date=${encodeURIComponent(date || new Date().toISOString().slice(0, 10))}&time=${encodeURIComponent(t.departTime)}&operator=${encodeURIComponent(t.operator)}`
                  )
                }
                title="Skip seat browsing — send your request straight to our WhatsApp desk"
                className="rounded-full bg-signal-rose px-3 py-1 text-xs font-extrabold uppercase tracking-wide text-white shadow-sm transition hover:bg-rose-600"
              >
                Direct Booking
              </button>
              <span className="font-display text-xl font-extrabold text-ink-950">{t.departTime}</span>
              <span className="text-sm font-semibold text-ink-900">{t.operator}</span>
            </div>

            {/* One row per bus type: Non A/C Normal Bus first, A/C Luxury Bus below it */}
            <div className="mt-3 divide-y divide-ink-900/8 border-t border-ink-900/8">
              {t.options.map((o) => (
                <div key={o.key} className="flex flex-wrap items-center justify-between gap-3 py-3">
                  <div className="flex items-center gap-4">
                    <span className="min-w-[9rem] text-sm font-semibold text-ink-800">{o.busType}</span>
                    <span className="text-xs text-ink-700/50">{o.duration}</span>
                  </div>
                  <div className="flex items-center gap-4">
                    <span className={`text-xs font-semibold ${o.seatsLeft <= 8 ? 'text-signal-rose' : 'text-brand-600'}`}>
                      {o.seatsLeft} seats left
                    </span>
                    <span className="font-display text-base font-extrabold text-ink-950">{o.price}</span>
                    <button
                      onClick={() => navigate(`/seat-selection?trip=${encodeURIComponent(t.id)}-${encodeURIComponent(o.key)}&from=${encodeURIComponent(route.from)}&to=${encodeURIComponent(route.to)}&date=${encodeURIComponent(date || new Date().toISOString().slice(0,10))}&time=${encodeURIComponent(t.departTime)}&duration=${encodeURIComponent(o.duration)}&operator=${encodeURIComponent(t.operator)}&busType=${encodeURIComponent(o.busType)}&price=${encodeURIComponent(o.price)}`)}
                      className="rounded-xl bg-brand-500 px-5 py-2 text-sm font-bold text-white hover:bg-brand-600"
                    >
                      View seats
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      <p className="mt-6 text-center text-xs text-ink-700/40">
        Departure times shown here are placeholder data — this page is where the Search &amp; Schedule module's live API response will render.
      </p>
    </div>
  )
}

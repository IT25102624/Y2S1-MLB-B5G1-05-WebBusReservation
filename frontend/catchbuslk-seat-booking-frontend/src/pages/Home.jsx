import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { HeroBus, RouteBusIcon } from '../components/BusArt.jsx'
import { popularRoutes, cities } from '../data/destinations.js'

export default function Home() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ from: '', to: '', date: '' })

  const search = (e) => {
    e.preventDefault()
    const params = new URLSearchParams({ from: form.from, to: form.to, date: form.date })
    navigate(`/schedule?${params.toString()}`)
  }

  return (
    <div>
      {/* ---------------- HERO ---------------- */}
      <section className="relative overflow-hidden bg-gradient-to-b from-brand-50 to-sand-50">
        <div className="mx-auto max-w-6xl px-6 pb-40 pt-14 sm:pt-20">
          <div className="grid items-center gap-10 lg:grid-cols-2">
            <div className="animate-rise">
              <span className="inline-flex items-center gap-2 rounded-full border border-brand-500/30 bg-brand-500/10 px-3 py-1 text-xs font-bold uppercase tracking-wide text-brand-700">
                <span className="h-1.5 w-1.5 rounded-full bg-brand-500" /> No account needed to book
              </span>
              <h1 className="mt-5 font-display text-5xl font-extrabold leading-[1.05] text-ink-950 sm:text-6xl">
                Sri Lanka,
                <span className="text-brand-500"> one ticket</span> at a time.
              </h1>
              <p className="mt-5 max-w-md text-lg text-ink-700/80">
                Compare schedules, pick your seat, and pay online in minutes —
                {user ? ` welcome back, ${user.fullName.split(' ')[0]}.` : ' book as a guest, or sign in to save your trips.'}
              </p>
            </div>

            <div className="animate-rise stagger-1 relative hidden lg:block">
              <HeroBus className="w-full drop-shadow-xl" />
            </div>
          </div>
        </div>

        {/* Search widget overlapping hero/content boundary, FlixBus-style */}
        <div className="absolute inset-x-0 bottom-0 translate-y-1/2 px-6">
          <form onSubmit={search} className="animate-rise stagger-2 mx-auto max-w-4xl rounded-3xl border border-ink-900/5 bg-white p-3 shadow-card-lg">
            <div className="grid gap-2 rounded-2xl bg-sand-100/70 p-4 sm:grid-cols-[1fr_auto_1fr_1fr_auto] sm:items-end sm:p-3">
              <Field label="From">
                <input required value={form.from} onChange={(e) => setForm({ ...form, from: e.target.value })}
                  placeholder="Colombo" className="field" list="city-list" />
              </Field>
              <div className="hidden pb-3 text-ink-400 sm:block">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M5 12h14m0 0-5-5m5 5-5 5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/></svg>
              </div>
              <Field label="To">
                <input required value={form.to} onChange={(e) => setForm({ ...form, to: e.target.value })}
                  placeholder="Kandy" className="field" list="city-list" />
              </Field>
              <Field label="Date">
                <input type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} className="field" />
              </Field>
              <button type="submit" className="mt-2 rounded-xl bg-brand-500 px-6 py-3 font-bold text-white shadow-card transition hover:bg-brand-600 sm:mt-0">
                Book a Trip
              </button>
            </div>
            <datalist id="city-list">
              {cities.map((c) => <option key={c} value={c} />)}
            </datalist>
          </form>
        </div>
      </section>

      {!user && (
        <p className="mx-auto mt-28 max-w-md px-6 text-center text-sm text-ink-700/60 sm:mt-24">
          Prefer faster checkout next time?{' '}
          <Link to="/register" className="font-bold text-brand-600 hover:underline">Create a free account</Link> — never required to book.
        </p>
      )}

      {/* ---------------- POPULAR DESTINATIONS ---------------- */}
      <section id="destinations" className="mx-auto max-w-6xl px-6 py-20">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <span className="text-xs font-bold uppercase tracking-widest text-brand-600">Where to next</span>
            <h2 className="mt-1 font-display text-3xl font-extrabold text-ink-950 sm:text-4xl">Popular destinations</h2>
          </div>
          <Link to="/destinations" className="text-sm font-bold text-brand-600 hover:underline">
            View all destinations →
          </Link>
        </div>

        <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {popularRoutes.map((r, i) => (
            <RouteCard key={`${r.from}-${r.to}`} route={r} delay={i % 4} />
          ))}
        </div>
      </section>

      {/* ---------------- WHY CATCHBUSLK ---------------- */}
      <section className="bg-ink-950 py-20">
        <div className="mx-auto max-w-6xl px-6">
          <span className="text-xs font-bold uppercase tracking-widest text-brand-400">Why CatchBusLK</span>
          <h2 className="mt-1 font-display text-3xl font-extrabold text-white sm:text-4xl">Built for how Sri Lanka travels</h2>

          <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            <Feature icon="ticket" title="Book without an account" desc="Search, select a seat, and pay as a guest — sign up only if you want saved history." />
            <Feature icon="seat" title="Pick your own seat" desc="A live seat map shows exactly what's free, so there's never a surprise at boarding." />
            <Feature icon="qr" title="QR e-tickets" desc="Your ticket lives on your phone — conductors verify it with a scan, no printing needed." />
            <Feature icon="shield" title="Secure payments" desc="Encrypted checkout and clear cancellation & refund policies on every route." />
          </div>
        </div>
      </section>

      {/* ---------------- STAKEHOLDER STRIP ---------------- */}
      <section className="mx-auto max-w-6xl px-6 py-20">
        <div className="grid gap-5 sm:grid-cols-2">
          <RoleTeaser
            title="Conductors & Drivers"
            desc="Sign in to your staff console to verify QR tickets, update boarding status, and view your route sheet."
            to="/staff/login"
          />
          <RoleTeaser
            title="Operators & Administrators"
            desc="The same staff portal now includes admin sign-in — manage fleets, schedules, users and RBAC in one place."
            to="/staff/login"
          />
        </div>
      </section>
    </div>
  )
}

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-1 block px-1 text-[11px] font-bold uppercase tracking-wider text-ink-700/50">{label}</span>
      {children}
      <style>{`.field{width:100%;background:transparent;padding:0.5rem 0.25rem;border-bottom:2px solid transparent;color:#0f1712;outline:none;font-weight:600;} .field:focus{border-color:#1fb85a}`}</style>
    </label>
  )
}

function RouteCard({ route, delay }) {
  return (
    <Link
      to={`/schedule?from=${encodeURIComponent(route.from)}&to=${encodeURIComponent(route.to)}`}
      className={`animate-rise stagger-${delay + 1} group block overflow-hidden rounded-2xl border border-ink-900/8 bg-white shadow-card transition hover:-translate-y-1 hover:shadow-card-lg`}
    >
      <div className="flex items-center justify-center bg-gradient-to-br from-brand-100 to-brand-50 py-8">
        <RouteBusIcon className="h-14 w-20 text-brand-500 transition group-hover:text-brand-600" />
      </div>
      <div className="p-5">
        <div className="flex items-center gap-2 text-sm font-bold text-ink-950">
          <span>{route.from}</span>
          <span className="route-dash flex-1 text-ink-300" />
          <span>{route.to}</span>
        </div>
        <div className="mt-3 flex items-center justify-between text-xs text-ink-700/60">
          <span>{route.duration}</span>
          <span>{route.frequency}</span>
        </div>
        <div className="mt-4 flex items-center justify-between border-t border-ink-900/8 pt-4">
          <span className="font-display text-lg font-extrabold text-brand-600">{route.price}</span>
          <span className="text-xs font-bold text-ink-800 group-hover:text-brand-600">View times →</span>
        </div>
      </div>
    </Link>
  )
}

const ICONS = {
  ticket: <path d="M4 8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v2a2 2 0 1 0 0 4v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-2a2 2 0 1 0 0-4V8Z" />,
  seat: <path d="M6 4v9a3 3 0 0 0 3 3h6M9 20h8a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2h-1" />,
  qr: <path d="M4 4h6v6H4V4Zm10 0h6v6h-6V4ZM4 14h6v6H4v-6Zm10 2h2v2m-2 2h4v-4h-2" />,
  shield: <path d="M12 3 5 6v6c0 5 3.5 8.5 7 10 3.5-1.5 7-5 7-10V6l-7-3Z" />
}

function Feature({ icon, title, desc }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 p-6">
      <div className="grid h-11 w-11 place-items-center rounded-xl bg-brand-500/15 text-brand-400">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
          {ICONS[icon]}
        </svg>
      </div>
      <h3 className="mt-4 font-display text-base font-bold text-white">{title}</h3>
      <p className="mt-2 text-sm text-slate-400">{desc}</p>
    </div>
  )
}

function RoleTeaser({ title, desc, to }) {
  return (
    <Link to={to} className="group block rounded-2xl border border-ink-900/8 bg-white p-6 shadow-card transition hover:-translate-y-0.5 hover:shadow-card-lg">
      <h3 className="font-display text-lg font-bold text-ink-950">{title}</h3>
      <p className="mt-2 text-sm text-ink-700/70">{desc}</p>
      <span className="mt-4 inline-block text-sm font-bold text-brand-600 group-hover:underline">Enter portal →</span>
    </Link>
  )
}

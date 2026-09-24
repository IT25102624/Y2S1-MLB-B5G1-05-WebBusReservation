import React, { useCallback, useEffect, useState } from 'react'
import client from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'

const money = value => value == null ? '—' : `LKR ${Number(value).toLocaleString(undefined, { minimumFractionDigits: 2 })}`
const date = value => value ? new Date(value).toLocaleDateString() : '—'

export default function PassengerDashboard() {
  const { user, sessionDashboard } = useAuth()
  const [summary, setSummary] = useState(sessionDashboard)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      const { data } = await client.get('/passenger/summary')
      setSummary(data)
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load your trip summary.')
    }
  }, [])

  useEffect(() => { load() }, [load])

  const trips = summary?.completedTrips || 0
  const discount = summary?.discountPercent || 0
  const target = summary?.nextDiscountTarget || 5
  const progress = discount >= 15 ? 100 : Math.min(100, Math.round((trips / target) * 100))

  return (
    <main className="min-h-[80vh] bg-sand-50 px-6 py-12">
      <div className="mx-auto max-w-6xl">
        <div className="rounded-3xl bg-brand-700 p-8 text-white shadow-card-lg">
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-brand-100">Passenger account</p>
          <h1 className="mt-2 font-display text-3xl font-extrabold">Welcome, {user?.fullName || 'traveller'}.</h1>
          <p className="mt-2 max-w-xl text-brand-100">Your trips, rewards and booking history in one simple place.</p>
        </div>

        {error && <p className="mt-5 rounded-xl bg-signal-rose/10 px-4 py-3 text-sm text-signal-rose">{error}</p>}

        <div className="mt-6 grid gap-5 md:grid-cols-3">
          <Card label="Completed trips" value={trips} detail="Trips you have finished" />
          <Card label="Current discount" value={`${discount}%`} detail={discount ? 'Applied to eligible future bookings' : 'Keep travelling to unlock a discount'} />
          <Card label="Next reward" value={discount >= 15 ? '15%' : `${summary?.tripsToNextDiscount || target} trips`}
            detail={discount >= 15 ? 'Top loyalty tier reached' : `Unlock ${summary?.nextDiscountPercent}% off`} />
        </div>

        <section className="mt-6 rounded-3xl border border-ink-900/10 bg-white p-6 shadow-card">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h2 className="font-display text-xl font-bold text-ink-950">Your loyalty progress</h2>
              <p className="mt-1 text-sm text-ink-700/60">5 trips = 5% off · 10 trips = 10% off · 20 trips = 15% off</p>
            </div>
            <span className="rounded-full bg-brand-50 px-4 py-2 text-sm font-bold text-brand-700">{discount}% discount</span>
          </div>
          <div className="mt-5 h-3 overflow-hidden rounded-full bg-sand-100">
            <div className="h-full rounded-full bg-brand-500 transition-all" style={{ width: `${progress}%` }} />
          </div>
          <p className="mt-2 text-sm text-ink-700/60">
            {discount >= 15 ? 'You have reached the highest reward tier.' : `${summary?.tripsToNextDiscount || target} more completed trip${(summary?.tripsToNextDiscount || target) === 1 ? '' : 's'} to your next discount.`}
          </p>
        </section>

        <section className="mt-6 rounded-3xl border border-ink-900/10 bg-white p-6 shadow-card">
          <h2 className="font-display text-xl font-bold text-ink-950">Recent trips</h2>
          {summary?.recentTrips?.length ? (
            <div className="mt-4 divide-y divide-ink-900/10">
              {summary.recentTrips.map(trip => (
                <div key={trip.bookingId} className="flex flex-wrap justify-between gap-3 py-4 text-sm">
                  <div>
                    <p className="font-semibold text-ink-950">{trip.sourceCity} → {trip.destinationCity}</p>
                    <p className="mt-1 text-ink-700/60">Booked {date(trip.bookingTime)} · {trip.bookingStatus}</p>
                  </div>
                  <p className="font-bold text-ink-950">{money(trip.totalAmount)}</p>
                </div>
              ))}
            </div>
          ) : <p className="mt-4 text-sm text-ink-700/60">Your completed and booked trips will appear here.</p>}
        </section>
      </div>
    </main>
  )
}

function Card({ label, value, detail }) {
  return <div className="rounded-3xl border border-ink-900/10 bg-white p-6 shadow-card">
    <p className="text-xs font-bold uppercase tracking-wider text-ink-700/50">{label}</p>
    <p className="mt-3 font-display text-3xl font-extrabold text-ink-950">{value}</p>
    <p className="mt-1 text-sm text-ink-700/60">{detail}</p>
  </div>
}

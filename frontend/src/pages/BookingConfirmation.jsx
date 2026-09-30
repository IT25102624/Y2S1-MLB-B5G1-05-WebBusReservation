import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function BookingConfirmation() {
  const navigate = useNavigate()
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [deleteChecked, setDeleteChecked] = useState(false)
  const [deleteConfirm, setDeleteConfirm] = useState(false)
  const [deleted, setDeleted] = useState(false)

  let booking = null
  try { booking = JSON.parse(localStorage.getItem('catchbuslk-last-booking') || 'null') } catch {}

  if (deleted) {
    return (
      <div className="min-h-screen bg-sand-50 px-6 py-20">
        <div className="mx-auto max-w-xl rounded-3xl bg-white p-8 text-center shadow-card-lg">
          <div className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-rose-100 text-3xl text-rose-600">✓</div>
          <span className="mt-5 block text-xs font-bold uppercase tracking-widest text-rose-600">Booking deleted</span>
          <h1 className="mt-2 font-display text-3xl font-extrabold text-ink-950">Your seat booking has been cancelled.</h1>
          <p className="mt-3 text-sm leading-6 text-ink-700/60">
            The selected seat is now available for other passengers.
            If a payment was already completed, the refund can be processed through the connected payment gateway.
          </p>
          <Link to="/" className="mt-6 inline-block rounded-xl bg-brand-500 px-6 py-3 font-bold text-white">Back to home</Link>
        </div>
      </div>
    )
  }

  if (!booking) return <div className="mx-auto max-w-xl px-6 py-20 text-center"><h1 className="font-display text-3xl font-extrabold">Booking not found</h1><Link to="/" className="mt-5 inline-block font-bold text-brand-600">Back home</Link></div>

  const editUrl = `/seat-selection?edit=1&ref=${encodeURIComponent(booking.reference)}&trip=${encodeURIComponent(booking.trip.id)}&from=${encodeURIComponent(booking.trip.from)}&to=${encodeURIComponent(booking.trip.to)}&date=${encodeURIComponent(booking.trip.date)}&time=${encodeURIComponent(booking.trip.time)}&duration=${encodeURIComponent(booking.trip.duration)}&operator=${encodeURIComponent(booking.trip.operator)}&busType=${encodeURIComponent(booking.trip.busType)}&price=${encodeURIComponent(booking.trip.price)}`

  const openDelete = () => {
    setDeleteOpen(true)
    setDeleteChecked(false)
    setDeleteConfirm(false)
  }

  const continueDelete = () => {
    if (!deleteChecked) return
    setDeleteConfirm(true)
  }

  const confirmDelete = () => {
    const cancelled = {
      ...booking,
      status: 'CANCELLED',
      cancelledAt: new Date().toISOString(),
      refundStatus: 'REFUND_TO_BE_PROCESSED',
    }
    localStorage.setItem('catchbuslk-last-cancelled-booking', JSON.stringify(cancelled))
    localStorage.removeItem('catchbuslk-last-booking')

    // Release any active seat locks belonging to this booking/trip.
    try {
      const locks = JSON.parse(localStorage.getItem('catchbuslk-seat-locks-v1') || '{}')
      booking.seats.forEach(seat => {
        Object.keys(locks)
          .filter(key => key.startsWith(`${booking.trip.id}:` ) && key.endsWith(`:${seat}`))
          .forEach(key => delete locks[key])
      })
      localStorage.setItem('catchbuslk-seat-locks-v1', JSON.stringify(locks))
    } catch {}

    setDeleteOpen(false)
    setDeleted(true)
  }

  return (
    <div className="min-h-screen bg-sand-50 px-6 py-12">
      <div className="mx-auto max-w-2xl rounded-3xl bg-white p-8 shadow-card-lg">
        <div className="text-center">
          <div className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-brand-100 text-3xl text-brand-600">✓</div>
          <span className="mt-5 block text-xs font-bold uppercase tracking-widest text-brand-600">Provisional booking created</span>
          <h1 className="mt-2 font-display text-3xl font-extrabold text-ink-950">You're almost ready to travel.</h1>
          <p className="mt-2 text-sm text-ink-700/60">Reference <strong className="text-ink-900">{booking.reference}</strong></p>
        </div>

        <div className="mt-8 grid gap-4 rounded-2xl bg-sand-50 p-5 sm:grid-cols-2">
          <Info label="Route" value={`${booking.trip.from} → ${booking.trip.to}`} />
          <Info label="Departure" value={`${booking.trip.date} · ${booking.trip.time}`} />
          <Info label="Seats" value={booking.seats.join(', ')} />
          <Info label="Passenger" value={booking.passenger.name} />
          <Info label="Email" value={booking.passenger.email} />
          <Info label="Total" value={`LKR ${booking.total.toLocaleString()}`} />
        </div>

        <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          <strong>Next step:</strong> connect this confirmation to your payment gateway/backend booking endpoint. The frontend currently stores this as a provisional booking so the full user flow can be tested without a backend.
        </div>

        <div className="mt-6 grid gap-3 sm:grid-cols-2">
          <button onClick={() => navigate(editUrl)} className="rounded-xl bg-brand-500 px-5 py-3 text-center font-bold text-white hover:bg-brand-600">
            Edit booking
          </button>
          <button onClick={openDelete} className="rounded-xl border border-rose-200 bg-rose-50 px-5 py-3 text-center font-bold text-rose-700 hover:bg-rose-100">
            Delete booking
          </button>
        </div>

        <div className="mt-3">
          <Link to="/" className="block rounded-xl border border-black/10 px-5 py-3 text-center font-bold">Back to home</Link>
        </div>
      </div>

      {deleteOpen && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-ink-950/55 px-5 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-3xl bg-white p-7 shadow-2xl">
            {!deleteConfirm ? (
              <>
                <div className="mx-auto grid h-14 w-14 place-items-center rounded-full bg-rose-100 text-2xl text-rose-600">!</div>
                <h2 className="mt-5 text-center font-display text-2xl font-extrabold text-ink-950">Delete your seat booking?</h2>
                <p className="mt-2 text-center text-sm leading-6 text-ink-700/60">
                  This will cancel seat <strong>{booking.seats.join(', ')}</strong> and make it available for another passenger.
                </p>

                <label className="mt-5 flex cursor-pointer items-start gap-3 rounded-2xl border border-black/10 bg-sand-50 p-4">
                  <input
                    type="checkbox"
                    checked={deleteChecked}
                    onChange={e => setDeleteChecked(e.target.checked)}
                    className="mt-1 h-5 w-5 accent-brand-500"
                  />
                  <span className="text-sm font-semibold text-ink-800">Yes, I want to delete this seat booking.</span>
                </label>

                <div className="mt-5 flex gap-3">
                  <button onClick={() => setDeleteOpen(false)} className="flex-1 rounded-xl border border-black/10 px-4 py-3 font-bold">No, keep it</button>
                  <button onClick={continueDelete} disabled={!deleteChecked} className="flex-1 rounded-xl bg-rose-600 px-4 py-3 font-bold text-white disabled:cursor-not-allowed disabled:opacity-40">Continue</button>
                </div>
              </>
            ) : (
              <>
                <div className="mx-auto grid h-14 w-14 place-items-center rounded-full bg-rose-100 text-2xl text-rose-600">?</div>
                <h2 className="mt-5 text-center font-display text-2xl font-extrabold text-ink-950">Confirm delete seat booking</h2>
                <p className="mt-2 text-center text-sm leading-6 text-ink-700/60">
                  Are you sure you want to cancel this booking? The seat will become available again.
                  Any completed payment should be refunded through the payment gateway according to its refund process.
                </p>
                <div className="mt-5 flex gap-3">
                  <button onClick={() => setDeleteConfirm(false)} className="flex-1 rounded-xl border border-black/10 px-4 py-3 font-bold">No</button>
                  <button onClick={confirmDelete} className="flex-1 rounded-xl bg-rose-600 px-4 py-3 font-bold text-white">Yes, delete</button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

function Info({ label, value }) {
  return <div><div className="text-xs font-bold uppercase tracking-wider text-ink-700/45">{label}</div><div className="mt-1 font-semibold text-ink-900">{value}</div></div>
}

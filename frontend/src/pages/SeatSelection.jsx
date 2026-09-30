import React, { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'

const LOCK_MS = 10 * 60 * 1000
const STORAGE_KEY = 'catchbuslk-seat-locks-v1'

function loadLocks() {
  try {
    const data = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    const now = Date.now()
    const active = Object.fromEntries(Object.entries(data).filter(([, v]) => v.expiresAt > now))
    localStorage.setItem(STORAGE_KEY, JSON.stringify(active))
    return active
  } catch { return {} }
}

function saveLocks(locks) { localStorage.setItem(STORAGE_KEY, JSON.stringify(locks)) }

function money(value) {
  return new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR', maximumFractionDigits: 0 }).format(value)
}

function parsePrice(price) {
  const n = Number(String(price || '').replace(/[^0-9.]/g, ''))
  return Number.isFinite(n) && n > 0 ? n : 800
}

export default function SeatSelection() {
  const [params] = useSearchParams()
  const location = useLocation()
  const navigate = useNavigate()
  const [locks, setLocks] = useState(loadLocks)
  const [selected, setSelected] = useState([])
  const [selectedBusType, setSelectedBusType] = useState(params.get('busType')?.toLowerCase().includes('a/c') ? 'AC_LUXURY' : 'NON_AC_NORMAL')
  const [secondsLeft, setSecondsLeft] = useState(600)
  const [step, setStep] = useState(1)
  const [error, setError] = useState('')
  const editMode = params.get('edit') === '1'
  const [editBooking] = useState(() => {
    if (!editMode) return null
    try {
      const saved = JSON.parse(localStorage.getItem('catchbuslk-last-booking') || 'null')
      return saved && saved.reference === params.get('ref') ? saved : null
    } catch { return null }
  })
  const [editInitialized, setEditInitialized] = useState(false)
  const [passenger, setPassenger] = useState(() => editBooking?.passenger || ({ name: '', email: '', phone: '' }))

  const trip = useMemo(() => ({
    id: params.get('trip') || 'colombo-kandy-0',
    from: params.get('from') || 'Colombo',
    to: params.get('to') || 'Kandy',
    date: params.get('date') || new Date().toISOString().slice(0, 10),
    time: params.get('time') || '08:00',
    duration: params.get('duration') || '3h 15m',
    operator: params.get('operator') || 'Comfort Line',
    busType: params.get('busType') || 'Luxury A/C',
    price: parsePrice(params.get('price') || 'LKR 850'),
  }), [params])

  // Two separate bus layouts:
  // Non A/C Normal = Sri Lanka Ashok Leyland style, 2 + 2, 54 seats.
  // A/C Luxury = 2 + 2 layout with wider front/rear spacing, 49 seats.
  const busLayouts = useMemo(() => ({
    NON_AC_NORMAL: {
      label: 'Non A/C Normal Bus',
      shortLabel: 'Non A/C Normal',
      operator: 'SLTB / Ashok Leyland',
      price: trip.price,
      rows: 14,
      seats: 54,
      layout: [
        [1,2,3,4], [5,6,7,8], [9,10,11,12], [13,14,15,16],
        [17,18,19,20], [21,22,23,24], [25,26,27,28], [29,30,31,32],
        [33,34,35,36], [37,38,39,40], [41,42,43,44], [45,46,47,48],
        [49,50,51,52], [53,54]
      ]
    },
    AC_LUXURY: {
      label: 'A/C Luxury Bus',
      shortLabel: 'A/C Luxury',
      operator: 'Luxury Coach',
      price: Math.max(trip.price + 400, 1200),
      rows: 13,
      seats: 49,
      layout: [
        [1,2,3,4], [5,6,7,8], [9,10,11,12], [13,14,15,16],
        [17,18,19,20], [21,22,23,24], [25,26,27,28], [29,30,31,32],
        [33,34,35,36], [37,38,39,40], [41,42,43,44], [45,46,47,48],
        [49]
      ]
    }
  }), [trip.price])

  const currentLayout = busLayouts[selectedBusType]

  const reserved = useMemo(() => {
    const deterministic = selectedBusType === 'NON_AC_NORMAL'
      ? [3, 8, 11, 19, 27, 31, 38, 45, 52]
      : [2, 7, 14, 21, 29, 36, 43]
    return new Set(deterministic.map((n) => `S${n}`))
  }, [selectedBusType])

  const activeOtherLocks = useMemo(() => {
    const result = new Set()
    Object.entries(locks).forEach(([key, value]) => {
      if (key.startsWith(`${trip.id}:${selectedBusType}:`) && value.owner !== 'current') result.add(key.split(':')[2])
    })
    return result
  }, [locks, trip.id, selectedBusType])

  useEffect(() => {
    if (!editMode || !editBooking || editInitialized) return

    const bookingBusType = String(editBooking.trip?.busType || '').toLowerCase().includes('a/c')
      ? 'AC_LUXURY'
      : 'NON_AC_NORMAL'

    const nextLocks = { ...loadLocks() }
    const expiresAt = Date.now() + LOCK_MS
    editBooking.seats.forEach(seat => {
      nextLocks[`${editBooking.trip.id}:${bookingBusType}:${seat}`] = {
        owner: 'current',
        tripId: editBooking.trip.id,
        busType: bookingBusType,
        expiresAt
      }
    })

    saveLocks(nextLocks)
    setLocks(nextLocks)
    setSelectedBusType(bookingBusType)
    setSelected(editBooking.seats)
    setPassenger(editBooking.passenger || { name: '', email: '', phone: '' })
    setSecondsLeft(600)
    setStep(1)
    setEditInitialized(true)
  }, [editMode, editBooking, editInitialized])

  useEffect(() => {
    const tick = () => {
      const now = Date.now()
      const fresh = loadLocks()
      setLocks(fresh)
      const mine = Object.values(fresh).filter(v => v.owner === 'current' && v.tripId === trip.id && v.busType === selectedBusType)
      if (mine.length) setSecondsLeft(Math.max(0, Math.ceil((Math.min(...mine.map(v => v.expiresAt)) - now) / 1000)))
      else if (step > 1) setSecondsLeft(0)
    }
    tick()
    const id = setInterval(tick, 1000)
    return () => clearInterval(id)
  }, [trip.id, selectedBusType, step])

  useEffect(() => {
    const myKeys = Object.keys(locks).filter(k => locks[k]?.owner === 'current' && locks[k]?.tripId === trip.id && locks[k]?.busType === selectedBusType)
    if (!myKeys.length && selected.length) setSelected([])
  }, [locks, trip.id, selectedBusType])

  const toggleSeat = (seatId) => {
    setError('')
    if (reserved.has(seatId) || activeOtherLocks.has(seatId)) return
    if (selected.includes(seatId)) {
      const next = selected.filter(s => s !== seatId)
      const nextLocks = { ...locks }
      delete nextLocks[`${trip.id}:${selectedBusType}:${seatId}`]
      saveLocks(nextLocks); setLocks(nextLocks); setSelected(next); return
    }
    if (selected.length >= 6) { setError('You can select up to 6 seats per booking.'); return }
    const now = Date.now()
    const nextLocks = { ...loadLocks() }
    nextLocks[`${trip.id}:${selectedBusType}:${seatId}`] = { owner: 'current', tripId: trip.id, busType: selectedBusType, expiresAt: now + LOCK_MS }
    saveLocks(nextLocks); setLocks(nextLocks); setSelected([...selected, seatId]); setSecondsLeft(600)
  }


  const changeBusType = (type) => {
    if (type === selectedBusType) return
    const nextLocks = { ...loadLocks() }
    Object.keys(nextLocks)
      .filter(k => nextLocks[k]?.owner === 'current' && nextLocks[k]?.tripId === trip.id && nextLocks[k]?.busType === selectedBusType)
      .forEach(k => delete nextLocks[k])
    saveLocks(nextLocks)
    setLocks(nextLocks)
    setSelected([])
    setError('')
    setSecondsLeft(600)
    setSelectedBusType(type)
  }

  const total = selected.length * currentLayout.price

  const continuePassenger = () => {
    if (!selected.length) { setError('Please select at least one seat.'); return }
    setStep(2); setError('')
  }

  const confirmBooking = (e) => {
    e.preventDefault()
    if (!passenger.name || !passenger.email || !passenger.phone) { setError('Please complete passenger details.'); return }
    if (secondsLeft <= 0) { setError('Your seat hold has expired. Please select your seats again.'); setStep(1); return }
    const booking = {
      reference: editMode && editBooking ? editBooking.reference : `CB${Date.now().toString().slice(-8)}`,
      trip: { ...trip, busType: currentLayout.label, operator: currentLayout.operator, price: currentLayout.price },
      seats: selected,
      passenger,
      total,
      status: 'PROVISIONAL',
      createdAt: editMode && editBooking ? editBooking.createdAt : new Date().toISOString(),
      modifiedAt: editMode ? new Date().toISOString() : undefined,
      expiresAt: Date.now() + LOCK_MS,
    }
    localStorage.setItem('catchbuslk-last-booking', JSON.stringify(booking))
    const nextLocks = { ...loadLocks() }
    selected.forEach(seat => { delete nextLocks[`${trip.id}:${selectedBusType}:${seat}`] })
    saveLocks(nextLocks)
    navigate(`/booking-confirmation?ref=${booking.reference}`)
  }

  const backToTimes = () => navigate(`/schedule?from=${encodeURIComponent(trip.from)}&to=${encodeURIComponent(trip.to)}&date=${trip.date}`)

  return (
    <div className="min-h-screen bg-[#f4f7f4] pb-20">
      <div className="border-b border-black/5 bg-white">
        <div className="mx-auto max-w-6xl px-6 py-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <Link to="/" className="font-display text-xl font-extrabold text-ink-950">Catch<span className="text-brand-500">Bus</span>LK</Link>
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-ink-700/50">
              <span className={step >= 1 ? 'text-brand-600' : ''}>1. Seats</span><span>→</span><span className={step >= 2 ? 'text-brand-600' : ''}>2. Passenger</span><span>→</span><span>3. Confirm</span>
            </div>
          </div>
        </div>
      </div>

      <main className="mx-auto max-w-6xl px-6 pt-8">
        <button onClick={backToTimes} className="text-sm font-bold text-ink-700/60 hover:text-brand-600">← Back to View Times</button>
        <div className="mt-5 grid gap-6 lg:grid-cols-[1fr_360px]">
          <section>
            <div className="rounded-3xl border border-black/5 bg-white p-5 shadow-card sm:p-7">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <span className="text-[11px] font-bold uppercase tracking-[.18em] text-brand-600">{editMode ? 'Edit your booking' : 'Choose your seats'}</span>
                  <h1 className="mt-1 font-display text-3xl font-extrabold text-ink-950">{editMode ? 'Change your seat' : `${trip.from} → ${trip.to}`}</h1>
                  {editMode && <p className="mt-2 text-sm font-semibold text-ink-700/60">Booking {editBooking?.reference || ''} · Choose another available seat, then confirm your updated booking.</p>}
                  <p className="mt-1 text-sm text-ink-700/60">{trip.date} · {trip.time} · {currentLayout.operator} · {currentLayout.label}</p>
                </div>
                {selected.length > 0 && <div className="rounded-2xl bg-brand-50 px-4 py-3 text-right"><div className="text-[10px] font-bold uppercase tracking-widest text-brand-700">Seats held for</div><div className="font-mono text-xl font-bold text-brand-700">{Math.floor(secondsLeft / 60)}:{String(secondsLeft % 60).padStart(2, '0')}</div></div>}
              </div>

              {step === 1 ? (
                <>
                  <div className="mt-7 grid gap-3 sm:grid-cols-2">
                    <button
                      type="button"
                      onClick={() => changeBusType('NON_AC_NORMAL')}
                      className={`rounded-2xl border-2 p-4 text-left transition ${
                        selectedBusType === 'NON_AC_NORMAL'
                          ? 'border-brand-500 bg-brand-50 shadow-card'
                          : 'border-black/10 bg-white hover:border-brand-300'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-3">
                        <div>
                          <div className="text-[10px] font-bold uppercase tracking-widest text-brand-600">Option 01</div>
                          <div className="mt-1 font-display text-lg font-extrabold text-ink-950">Non A/C Normal Bus</div>
                          <div className="mt-1 text-xs text-ink-700/60">Sri Lanka Ashok Leyland · 2 + 2 seats</div>
                        </div>
                        <span className={`grid h-7 w-7 place-items-center rounded-full border-2 ${selectedBusType === 'NON_AC_NORMAL' ? 'border-brand-500 bg-brand-500 text-white' : 'border-black/15'}`}>✓</span>
                      </div>
                    </button>

                    <button
                      type="button"
                      onClick={() => changeBusType('AC_LUXURY')}
                      className={`rounded-2xl border-2 p-4 text-left transition ${
                        selectedBusType === 'AC_LUXURY'
                          ? 'border-brand-500 bg-brand-50 shadow-card'
                          : 'border-black/10 bg-white hover:border-brand-300'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-3">
                        <div>
                          <div className="text-[10px] font-bold uppercase tracking-widest text-brand-600">Option 02</div>
                          <div className="mt-1 font-display text-lg font-extrabold text-ink-950">A/C Luxury Bus</div>
                          <div className="mt-1 text-xs text-ink-700/60">Luxury coach · 2 + 2 seats</div>
                        </div>
                        <span className={`grid h-7 w-7 place-items-center rounded-full border-2 ${selectedBusType === 'AC_LUXURY' ? 'border-brand-500 bg-brand-500 text-white' : 'border-black/15'}`}>✓</span>
                      </div>
                    </button>
                  </div>

                  <div className="mt-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-sand-50 px-4 py-3">
                    <div>
                      <div className="font-bold text-ink-950">{currentLayout.label}</div>
                      <div className="text-xs text-ink-700/55">{currentLayout.seats} seats · {money(currentLayout.price)} per seat</div>
                    </div>
                    <div className="flex flex-wrap items-center gap-4 text-xs font-semibold text-ink-700/60">
                      <Legend cls="bg-white border-ink-900/20" label="Available" />
                      <Legend cls="bg-brand-500 border-brand-500" label="Selected" />
                      <Legend cls="bg-ink-200 border-ink-300" label="Unavailable" />
                    </div>
                  </div>

                  <div className="mx-auto mt-6 max-w-md rounded-[2rem] border border-ink-900/10 bg-sand-50 p-4 shadow-inner">
                    <div className="mb-5 flex items-center justify-between rounded-2xl bg-white px-4 py-3 text-xs font-bold text-ink-700/60">
                      <span>FRONT</span><span>Driver</span>
                    </div>

                    <div className="space-y-3">
                      {currentLayout.layout.map((rowSeats, rowIndex) => (
                        <div key={rowIndex} className="grid grid-cols-[1fr_1fr_28px_1fr_1fr] items-center gap-2">
                          <Seat id={`S${rowSeats[0]}`} reserved={reserved.has(`S${rowSeats[0]}`)} locked={activeOtherLocks.has(`S${rowSeats[0]}`)} selected={selected.includes(`S${rowSeats[0]}`)} onClick={toggleSeat} />
                          <Seat id={`S${rowSeats[1] || ''}`} hidden={!rowSeats[1]} reserved={rowSeats[1] ? reserved.has(`S${rowSeats[1]}`) : false} locked={rowSeats[1] ? activeOtherLocks.has(`S${rowSeats[1]}`) : false} selected={rowSeats[1] ? selected.includes(`S${rowSeats[1]}`) : false} onClick={toggleSeat} />
                          <span className="text-center text-[10px] text-ink-700/30">{rowIndex + 1}</span>
                          <Seat id={`S${rowSeats[2] || ''}`} hidden={!rowSeats[2]} reserved={rowSeats[2] ? reserved.has(`S${rowSeats[2]}`) : false} locked={rowSeats[2] ? activeOtherLocks.has(`S${rowSeats[2]}`) : false} selected={rowSeats[2] ? selected.includes(`S${rowSeats[2]}`) : false} onClick={toggleSeat} />
                          <Seat id={`S${rowSeats[3] || ''}`} hidden={!rowSeats[3]} reserved={rowSeats[3] ? reserved.has(`S${rowSeats[3]}`) : false} locked={rowSeats[3] ? activeOtherLocks.has(`S${rowSeats[3]}`) : false} selected={rowSeats[3] ? selected.includes(`S${rowSeats[3]}`) : false} onClick={toggleSeat} />
                        </div>
                      ))}
                    </div>
                    <div className="mt-5 rounded-xl bg-ink-900 px-4 py-3 text-center text-xs font-bold text-white">REAR</div>
                  </div>

                  {error && <p className="mt-5 rounded-xl bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">{error}</p>}
                  <button onClick={continuePassenger} className="mt-6 w-full rounded-2xl bg-brand-500 px-6 py-4 font-bold text-white shadow-card hover:bg-brand-600">Continue with {selected.length || '0'} seat{selected.length === 1 ? '' : 's'} →</button>
                </>
              ) : (
                <form onSubmit={confirmBooking} className="mt-8 space-y-5">
                  <div className="rounded-2xl bg-brand-50 p-4 text-sm text-brand-800"><strong>Your seats are temporarily locked.</strong> Complete passenger details before the 10-minute hold expires.</div>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <Field label="Full name" value={passenger.name} onChange={v => setPassenger({ ...passenger, name: v })} placeholder="Passenger name" />
                    <Field label="Mobile number" value={passenger.phone} onChange={v => setPassenger({ ...passenger, phone: v })} placeholder="07X XXX XXXX" />
                    <Field label="Email address" value={passenger.email} onChange={v => setPassenger({ ...passenger, email: v })} placeholder="you@example.com" type="email" />
                  </div>
                  <div className="border-t border-black/5 pt-5">
                    <h2 className="font-display text-lg font-extrabold text-ink-950">Boarding point</h2>
                    <div className="mt-3 grid gap-3 sm:grid-cols-2"><label className="cursor-pointer rounded-2xl border-2 border-brand-500 bg-brand-50 p-4"><input type="radio" defaultChecked name="boarding" className="mr-2 accent-brand-500" /> {trip.from} Bus Stand</label><label className="cursor-pointer rounded-2xl border border-black/10 bg-white p-4"><input type="radio" name="boarding" className="mr-2 accent-brand-500" /> Main Highway Stop</label></div>
                  </div>
                  {error && <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">{error}</p>}
                  <div className="flex gap-3"><button type="button" onClick={() => setStep(1)} className="rounded-2xl border border-black/10 px-5 py-4 font-bold text-ink-800">Back</button><button type="submit" className="flex-1 rounded-2xl bg-brand-500 px-6 py-4 font-bold text-white shadow-card hover:bg-brand-600">{editMode ? 'Save updated booking →' : 'Confirm provisional booking →'}</button></div>
                </form>
              )}
            </div>
          </section>

          <aside className="lg:sticky lg:top-6 lg:self-start">
            <div className="rounded-3xl bg-ink-950 p-6 text-white shadow-card-lg">
              <span className="text-[10px] font-bold uppercase tracking-[.2em] text-brand-400">Trip summary</span>
              <div className="mt-4 text-xl font-extrabold">{trip.from} <span className="text-brand-400">→</span> {trip.to}</div>
              <div className="mt-1 text-sm text-slate-400">{trip.date} · {trip.time}</div>
              <div className="mt-5 space-y-3 border-t border-white/10 pt-5 text-sm"><Summary label="Operator" value={trip.operator} /><Summary label="Bus" value={currentLayout.label} /><Summary label="Selected" value={selected.length ? selected.join(', ') : 'None'} /></div>
              <div className="mt-6 border-t border-white/10 pt-5"><div className="flex justify-between text-sm text-slate-400"><span>{selected.length} × {money(currentLayout.price)}</span><span>{money(total)}</span></div><div className="mt-2 flex justify-between text-lg font-extrabold"><span>Total</span><span className="text-brand-400">{money(total)}</span></div></div>
            </div>
          </aside>
        </div>

        <section className="mt-10 rounded-3xl border border-black/5 bg-white p-6 shadow-card">
          <span className="text-[11px] font-bold uppercase tracking-[.18em] text-brand-600">Need to know</span>
          <h2 className="mt-1 font-display text-2xl font-extrabold text-ink-950">Booking questions</h2>
          <div className="mt-5 grid gap-3 md:grid-cols-2"><FAQ q="Can I change my seat?" a="Yes. Go back before confirmation and choose another available seat. A selected seat remains held only while your booking timer is active." /><FAQ q="What happens if I do not finish payment?" a="The temporary seat lock expires after 10 minutes and the seats return to availability." /><FAQ q="Can two passengers choose the same seat?" a="No. A seat is immediately locked when selected, preventing another active session from taking it." /><FAQ q="Do I need an account?" a="No. CatchBusLK supports guest booking. An account can be used later for saved trip history." /></div>
        </section>
      </main>
    </div>
  )
}

function Seat({ id, reserved, locked, selected, onClick, hidden = false }) {
  if (hidden) return <span className="aspect-square" aria-hidden="true" />
  const disabled = reserved || locked
  return <button type="button" disabled={disabled} onClick={() => onClick(id)} aria-label={`Seat ${id.replace('S', '')}`} className={`aspect-square rounded-lg border text-[11px] font-extrabold transition ${selected ? 'border-brand-500 bg-brand-500 text-white shadow-md scale-105' : disabled ? 'cursor-not-allowed border-ink-200 bg-ink-100 text-ink-400' : 'border-ink-900/15 bg-white text-ink-800 hover:-translate-y-0.5 hover:border-brand-400 hover:text-brand-600'}`}>{id.replace('S', '')}</button>
}
function Legend({ cls, label }) { return <span className="flex items-center gap-2"><i className={`h-4 w-4 rounded border ${cls}`} />{label}</span> }
function Summary({ label, value }) { return <div className="flex justify-between gap-4"><span className="text-slate-500">{label}</span><span className="text-right font-semibold">{value}</span></div> }
function Field({ label, value, onChange, placeholder, type = 'text' }) { return <label className="block"><span className="mb-1.5 block text-xs font-bold uppercase tracking-wider text-ink-700/50">{label}</span><input required type={type} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} className="w-full rounded-xl border border-black/10 bg-sand-50 px-4 py-3 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/10" /></label> }
function FAQ({ q, a }) { return <details className="group rounded-2xl border border-black/8 bg-sand-50 p-4"><summary className="cursor-pointer list-none font-bold text-ink-900">{q}<span className="float-right text-brand-600">+</span></summary><p className="mt-3 pr-5 text-sm leading-6 text-ink-700/65">{a}</p></details> }

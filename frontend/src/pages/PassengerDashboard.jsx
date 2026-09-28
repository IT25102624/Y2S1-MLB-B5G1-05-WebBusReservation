import React, { useCallback, useEffect, useState } from 'react'
import client from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'

const money = value => value == null ? '—' : `LKR ${Number(value).toLocaleString(undefined, { minimumFractionDigits: 2 })}`
const date = value => value ? new Date(value).toLocaleDateString() : '—'
const paymentApp = import.meta.env.VITE_PAYMENT_APP_URL || 'http://localhost:8080'

export default function PassengerDashboard() {
  const { user, sessionDashboard } = useAuth()
  const [summary, setSummary] = useState(sessionDashboard)
  const [inbox, setInbox] = useState([])
  const [refund, setRefund] = useState({ bookingId: '', accountHolderName: '', bankName: '', branchName: '', accountNumber: '', reason: 'Passenger cancellation' })
  const [refundMessage, setRefundMessage] = useState('')
  const [showRefund, setShowRefund] = useState(false)
  const [showBankEdit, setShowBankEdit] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      const { data } = await client.get('/passenger/summary')
      setSummary(data)
      const [messages, bank] = await Promise.all([client.get('/passenger/inbox'), client.get('/passenger/refund-bank-details')])
      setInbox(messages.data || [])
      if (bank.data?.accountHolderName) setRefund(r => ({ ...r, ...bank.data }))
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

  const requestRefund = async e => {
    e.preventDefault(); setRefundMessage('')
    try {
      await client.post('/passenger/refunds', refund)
      setRefundMessage('Refund request sent to the administrator.')
      setShowRefund(false)
      setInbox((await client.get('/passenger/inbox')).data || [])
    } catch (err) { setRefundMessage(err.response?.data?.message || 'Refund request failed.') }
  }

  const updateBankDetails = async e => {
    e.preventDefault(); setRefundMessage('')
    try {
      const bankDetails = { accountHolderName: refund.accountHolderName, bankName: refund.bankName, branchName: refund.branchName, accountNumber: refund.accountNumber }
      await client.put('/passenger/refund-bank-details', bankDetails)
      setRefundMessage('Existing bank details and pending refund mail updated successfully.')
      setShowBankEdit(false)
      setInbox((await client.get('/passenger/inbox')).data || [])
    } catch (err) { setRefundMessage(err.response?.data?.message || 'Bank details update failed.') }
  }

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

        <div className="mt-6">
          <section className="rounded-3xl border border-ink-900/10 bg-white p-6 shadow-card">
            <div className="flex items-center justify-between gap-3"><h2 className="font-display text-xl font-bold text-ink-950">Inbox</h2><div className="flex gap-2"><button onClick={() => setShowRefund(true)} className="rounded-lg bg-brand-600 px-3 py-2 text-sm font-bold text-white">Request a refund</button><button onClick={load} className="text-sm font-bold text-brand-600">Refresh</button></div></div>
            <div className="mt-4 divide-y divide-ink-900/10">{inbox.length ? inbox.map(m => <details key={m.notificationId} className="group py-3"><summary className="flex cursor-pointer list-none items-center justify-between gap-3"><strong className="text-sm text-ink-950">✉️ {m.subject}</strong><span className="whitespace-nowrap text-xs text-ink-700/40">{new Date(m.createdAt).toLocaleString()}</span></summary><div className="mt-3 rounded-xl bg-ink-50 p-4"><p className="whitespace-pre-line text-sm leading-6 text-ink-700/70">{m.message}</p>{m.notificationType === 'PAYMENT_SUCCESS' && m.ticketToken && <a href={`${paymentApp}/api/v1/payments/tickets/${encodeURIComponent(m.ticketToken)}/qr.png?download=true`} className="mt-3 inline-flex rounded-lg bg-brand-600 px-3 py-2 text-xs font-bold text-white">⬇ Download QR ticket</a>}{m.notificationType === 'REFUND_REQUEST_SENT' && m.refundStatus === 'REQUESTED' && <button onClick={() => setShowBankEdit(true)} className="mt-3 block rounded-lg border border-brand-600 px-3 py-2 text-xs font-bold text-brand-700">✏️ Update bank details</button>}</div></details>) : <div className="rounded-2xl bg-gradient-to-br from-indigo-50 to-pink-50 px-5 py-8 text-center"><div className="text-5xl">📭</div><strong className="mt-2 block text-brand-700">No messages yet</strong></div>}</div>
          </section>
          {refundMessage && <p className="mt-3 rounded-lg bg-brand-50 px-3 py-2 text-sm text-brand-700">{refundMessage}</p>}
        </div>
        {showRefund && <div className="fixed inset-0 z-50 grid place-items-center bg-black/60 px-5"><section className="w-full max-w-2xl rounded-3xl bg-white p-6 shadow-card-lg"><div className="flex justify-between gap-3"><div><h2 className="font-display text-xl font-bold text-ink-950">Request a refund</h2><p className="mt-1 text-sm text-ink-700/60">Bank details are saved for future requests.</p></div><button onClick={() => setShowRefund(false)} className="text-xl text-ink-700/50">×</button></div><form onSubmit={requestRefund} className="mt-4 grid grid-cols-2 gap-3">{[['bookingId','Booking number'],['accountHolderName','Account holder'],['bankName','Bank name'],['branchName','Branch'],['accountNumber','Account number'],['reason','Reason']].map(([key,label]) => <label key={key} className="col-span-2 sm:col-span-1"><span className="mb-1 block text-xs font-bold uppercase text-ink-700/50">{label}</span><input required value={refund[key]} onChange={e => setRefund({ ...refund, [key]: e.target.value })} className="w-full rounded-xl border border-ink-900/10 px-3 py-2.5 text-sm"/></label>)}<div className="col-span-2 flex justify-end gap-2"><button type="button" onClick={() => setShowRefund(false)} className="rounded-xl border px-4 py-3 font-bold">Cancel</button><button className="rounded-xl bg-brand-600 px-4 py-3 font-bold text-white">Send refund request</button></div></form></section></div>}
        {showBankEdit && <div className="fixed inset-0 z-50 grid place-items-center bg-black/60 px-5"><section className="w-full max-w-2xl rounded-3xl bg-white p-6 shadow-card-lg"><div className="flex justify-between gap-3"><div><h2 className="font-display text-xl font-bold text-ink-950">Update refund bank details</h2><p className="mt-1 text-sm text-ink-700/60">The existing database row and pending admin mail will be updated.</p></div><button onClick={() => setShowBankEdit(false)} className="text-xl text-ink-700/50">×</button></div><form onSubmit={updateBankDetails} className="mt-4 grid grid-cols-2 gap-3">{[['accountHolderName','Account holder'],['bankName','Bank name'],['branchName','Branch'],['accountNumber','Account number']].map(([key,label]) => <label key={key} className="col-span-2 sm:col-span-1"><span className="mb-1 block text-xs font-bold uppercase text-ink-700/50">{label}</span><input required value={refund[key]} onChange={e => setRefund({ ...refund, [key]: e.target.value })} className="w-full rounded-xl border border-ink-900/10 px-3 py-2.5 text-sm"/></label>)}<div className="col-span-2 flex justify-end gap-2"><button type="button" onClick={() => setShowBankEdit(false)} className="rounded-xl border px-4 py-3 font-bold">Cancel</button><button className="rounded-xl bg-brand-600 px-4 py-3 font-bold text-white">Update bank details</button></div></form></section></div>}
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

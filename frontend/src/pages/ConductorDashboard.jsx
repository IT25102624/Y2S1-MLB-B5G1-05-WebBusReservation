import React, { useState } from 'react'
import client from '../api/client'
import StaffDashboard from './StaffDashboard.jsx'
import QrScanner from '../components/QrScanner.jsx'

export default function ConductorDashboard() {
  const [reference, setReference] = useState('')
  const [remarks, setRemarks] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [scanning, setScanning] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setResult(null)
    if (!reference.trim()) return setError('Scan or enter the passenger booking QR payload.')
    setLoading(true)
    try {
      const { data } = await client.post('/conductor/board', { bookingReference: reference.trim(), remarks })
      setResult(data)
      setReference('')
      setRemarks('')
    } catch (err) {
      setError(err.response?.data?.message || 'Could not verify this ticket.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <StaffDashboard role="CONDUCTOR" />
      <div className="min-h-[40vh] bg-ops-950 px-6 pb-16">
        <div className="mx-auto max-w-5xl">
          <div className="rounded-3xl border border-signal-cyan/20 bg-ops-800/70 p-6">
            <h2 className="font-display text-xl font-bold text-white">Passenger QR / booking verification</h2>
            <p className="mt-1 text-sm text-slate-500">Scan a passenger ticket after signing in, or enter its booking reference manually.</p>
            <form onSubmit={submit} className="mt-5 grid gap-4 md:grid-cols-2">
              <input value={reference} onChange={e => setReference(e.target.value)} required
                placeholder="Booking reference / QR payload"
                className="rounded-xl border border-white/10 bg-ops-900 px-4 py-3 font-mono text-white outline-none focus:border-signal-cyan/60" />
              <input value={remarks} onChange={e => setRemarks(e.target.value)}
                placeholder="Remarks (optional)"
                className="rounded-xl border border-white/10 bg-ops-900 px-4 py-3 text-white outline-none focus:border-signal-cyan/60" />
              <button disabled={loading} className="rounded-xl bg-signal-cyan px-5 py-3 font-bold text-ops-950 disabled:opacity-60 md:col-span-2">
                {loading ? 'Verifying…' : 'Verify & mark boarded'}
              </button>
            </form>
            <button type="button" onClick={() => setScanning(true)}
              className="mt-3 rounded-xl border border-signal-cyan/30 px-4 py-2 text-sm font-bold text-signal-cyan">
              Scan passenger QR
            </button>
            {scanning && <QrScanner onScan={(value) => { setReference(value); setScanning(false) }} onClose={() => setScanning(false)} />}
            {error && <p className="mt-3 rounded-lg bg-signal-rose/10 px-3 py-2 text-sm text-signal-rose">{error}</p>}
            {result && <p className="mt-3 rounded-lg bg-signal-cyan/10 px-3 py-2 text-sm text-signal-cyan">
              Booking {result.bookingReference} marked as boarded at {new Date(result.verifiedAt).toLocaleTimeString()}.
              {' '}This trip now has {result.boardedCount} boarded passenger{result.boardedCount === 1 ? '' : 's'}.
            </p>}
          </div>
        </div>
      </div>
    </>
  )
}

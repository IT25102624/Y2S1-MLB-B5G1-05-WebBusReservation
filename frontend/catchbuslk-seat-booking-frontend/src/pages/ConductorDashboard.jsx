import React, { useState } from 'react'
import client from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'

/** Implements "Verify Ticket / Scan QR" + "Update Boarding Status" from the
 * use case. A real camera-based QR scanner would replace the manual input
 * below; the API contract is already wired to accept a bookingReference. */
export default function ConductorDashboard() {
  const { user } = useAuth()
  const [reference, setReference] = useState('')
  const [remarks, setRemarks] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setResult(null)
    setLoading(true)
    try {
      const { data } = await client.post('/conductor/board', { bookingReference: reference, remarks })
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
    <div className="min-h-screen bg-ops-950 px-6 py-12">
    <div className="mx-auto max-w-3xl">
      <span className="text-xs font-semibold uppercase tracking-[0.3em] text-signal-cyan">Boarding console</span>
      <h1 className="mt-1 font-display text-3xl text-white">Verify &amp; board passengers</h1>
      <p className="mt-1 text-sm text-slate-500">{user?.fullName} · Employee ID {user?.employeeId || '—'} · Bus {user?.assignedBusPlate || 'unassigned'}</p>

      <div className="mt-8 rounded-3xl border border-signal-cyan/20 bg-ops-800/70 p-8">
        <div className="mx-auto mb-8 grid h-40 w-40 place-items-center rounded-2xl border border-dashed border-signal-cyan/40 bg-ops-900">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none">
            <rect x="3" y="3" width="7" height="7" stroke="#5eead4" strokeWidth="1.5"/>
            <rect x="14" y="3" width="7" height="7" stroke="#5eead4" strokeWidth="1.5"/>
            <rect x="3" y="14" width="7" height="7" stroke="#5eead4" strokeWidth="1.5"/>
            <path d="M14 14h3v3h-3zM19 14h2M14 19h2M19 19h2" stroke="#5eead4" strokeWidth="1.5"/>
          </svg>
        </div>
        <p className="text-center text-xs text-slate-500 mb-6">Camera scanning wires into the same endpoint — enter a reference manually for now.</p>

        <form onSubmit={submit} className="space-y-4">
          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-500">Booking reference / QR payload</span>
            <input
              value={reference} onChange={(e) => setReference(e.target.value)} required
              placeholder="BUS-2026-0000451"
              className="w-full rounded-xl border border-white/10 bg-ops-900/70 px-4 py-3 font-mono text-white outline-none focus:border-signal-cyan/60"
            />
          </label>
          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-500">Remarks (optional)</span>
            <input
              value={remarks} onChange={(e) => setRemarks(e.target.value)}
              placeholder="e.g. boarded at Negombo stop"
              className="w-full rounded-xl border border-white/10 bg-ops-900/70 px-4 py-3 text-white outline-none focus:border-signal-cyan/60"
            />
          </label>

          {error && <p className="rounded-lg bg-signal-rose/10 px-3 py-2 text-sm text-signal-rose">{error}</p>}
          {result && (
            <p className="rounded-lg bg-signal-cyan/10 px-3 py-2 text-sm text-signal-cyan">
              Boarded — {result.bookingReference} confirmed at {new Date(result.verifiedAt).toLocaleTimeString()}
            </p>
          )}

          <button disabled={loading} className="w-full rounded-xl bg-signal-cyan py-3 font-semibold text-ops-950 transition hover:brightness-95 disabled:opacity-60">
            {loading ? 'Verifying…' : 'Verify & mark boarded'}
          </button>
        </form>
      </div>
    </div>
    </div>
  )
}

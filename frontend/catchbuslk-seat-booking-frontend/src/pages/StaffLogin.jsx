import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import Logo from '../components/Logo.jsx'

const ROLES = [
  { key: 'CONDUCTOR', label: 'Chief Conductor', hint: 'Scan QR tickets & manage boarding' },
  { key: 'DRIVER', label: 'Bus Driver', hint: 'View assigned routes & schedules' },
  { key: 'ADMIN', label: 'Administrator', hint: 'Manage fleets, users & RBAC' }
]

/**
 * Unified operations portal: Conductor and Driver sign-in share one endpoint
 * (/api/auth/staff/login), while Administrator uses a distinct one
 * (/api/auth/admin/login) — the tab only changes which call + redirect fires,
 * keeping the UI as a single "Staff & Admin" entry point as requested.
 */
export default function StaffLogin() {
  const { loginStaff, loginAdmin } = useAuth()
  const navigate = useNavigate()
  const [role, setRole] = useState('CONDUCTOR')
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      if (role === 'ADMIN') {
        await loginAdmin(form)
        navigate('/admin')
      } else {
        const res = await loginStaff(form)
        navigate(res.user.role === 'CONDUCTOR' ? '/conductor' : '/driver')
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid credentials for this role.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative min-h-[88vh] overflow-hidden bg-ops-950">
      <div className="pointer-events-none absolute inset-0 opacity-40" style={{
        backgroundImage: 'linear-gradient(rgba(94,234,212,0.06) 1px, transparent 1px), linear-gradient(90deg, rgba(94,234,212,0.06) 1px, transparent 1px)',
        backgroundSize: '42px 42px'
      }} />

      <div className="relative mx-auto flex min-h-[88vh] max-w-5xl items-center justify-center px-6 py-16">
        <div className="grid w-full gap-10 lg:grid-cols-2 lg:items-center">
          <div className="hidden animate-rise flex-col justify-center lg:flex">
            <Logo dark className="mb-8" />
            <span className="text-xs font-bold uppercase tracking-[0.3em] text-signal-cyan">Operations portal</span>
            <h1 className="mt-3 font-display text-4xl font-extrabold text-white">One sign-in for<br/>everyone who runs the fleet.</h1>
            <p className="mt-4 max-w-sm text-slate-400">
              Conductors verify tickets and board passengers, drivers check their
              route sheet, and administrators manage the whole platform — same
              portal, role-aware from the moment you sign in.
            </p>
            <div className="relative mt-10 h-40 w-72 overflow-hidden rounded-2xl border border-signal-cyan/30 bg-ops-900">
              <div className="absolute inset-x-6 top-6 h-2 rounded bg-white/10" />
              <div className="absolute inset-x-6 top-11 h-2 w-2/3 rounded bg-white/5" />
              <div className="scanline absolute inset-x-0 top-0 h-16" />
              <div className="absolute bottom-4 left-6 font-mono text-[11px] text-signal-cyan/80">AUTHENTICATING…</div>
            </div>
          </div>

          <div className="animate-rise stagger-1 rounded-3xl border border-signal-cyan/20 bg-ops-800/80 p-8 shadow-glow">
            <Logo dark className="mb-6 lg:hidden" />
            <div className="mb-6 grid grid-cols-3 gap-1 rounded-xl border border-white/10 bg-ops-900/70 p-1">
              {ROLES.map((r) => (
                <button
                  key={r.key}
                  type="button"
                  onClick={() => setRole(r.key)}
                  className={`rounded-lg py-2 text-xs font-bold transition ${
                    role === r.key ? 'bg-signal-cyan text-ops-950' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {r.key === 'CONDUCTOR' ? 'Conductor' : r.key === 'DRIVER' ? 'Driver' : 'Admin'}
                </button>
              ))}
            </div>

            <h2 className="font-display text-2xl font-bold text-white">{ROLES.find((r) => r.key === role).label} sign-in</h2>
            <p className="mt-1 text-sm text-slate-400">{ROLES.find((r) => r.key === role).hint}. Accounts are provisioned by an administrator.</p>

            <form onSubmit={submit} className="mt-6 space-y-4">
              <label className="block">
                <span className="mb-1.5 block text-xs font-bold uppercase tracking-wider text-slate-400">
                  {role === 'ADMIN' ? 'Admin email' : 'Employee email'}
                </span>
                <input
                  type="email" required value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  placeholder={role === 'ADMIN' ? 'admin@catchbus.lk' : 'employeeid@catchbus.lk'}
                  className="w-full rounded-xl border border-white/10 bg-ops-900/70 px-4 py-3 font-mono text-sm text-white placeholder-slate-600 outline-none focus:border-signal-cyan/60"
                />
              </label>
              <label className="block">
                <span className="mb-1.5 block text-xs font-bold uppercase tracking-wider text-slate-400">Password</span>
                <input
                  type="password" required value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  placeholder="••••••••"
                  className="w-full rounded-xl border border-white/10 bg-ops-900/70 px-4 py-3 text-white placeholder-slate-600 outline-none focus:border-signal-cyan/60"
                />
              </label>

              {error && <p className="rounded-lg bg-signal-rose/10 px-3 py-2 text-sm text-signal-rose">{error}</p>}

              <button disabled={loading} className="w-full rounded-xl bg-signal-cyan py-3 font-bold text-ops-950 transition hover:brightness-95 disabled:opacity-60">
                {loading ? 'Verifying…' : `Sign in as ${role === 'CONDUCTOR' ? 'Conductor' : role === 'DRIVER' ? 'Driver' : 'Administrator'}`}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}

import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import Logo from '../components/Logo.jsx'

const ROLES = [
  { key: 'CONDUCTOR', label: 'Chief Conductor', hint: 'Sign in to view your schedule, route operations, passenger boarding and issue reporting.' },
  { key: 'DRIVER', label: 'Bus Driver', hint: 'Sign in to view your schedule, live route operations and issue reporting.' },
  { key: 'ADMIN', label: 'Administrator', hint: 'Manage fleets, users & RBAC.' }
]

export default function StaffLogin() {
  const { loginStaff, loginAdmin } = useAuth()
  const navigate = useNavigate()
  const [role, setRole] = useState('CONDUCTOR')
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [submitted, setSubmitted] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setSubmitted(true)
    setError('')
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return setError('Enter a valid email address.')
    if (form.password.length < 8) return setError('Password must contain at least 8 characters.')
    setLoading(true)
    try {
      if (role === 'ADMIN') {
        await loginAdmin({ email: form.email.trim(), password: form.password })
        navigate('/admin')
      } else {
        const res = await loginStaff({
          email: form.email.trim(),
          password: form.password
        })
        navigate(res.user.role === 'CONDUCTOR' ? '/conductor' : '/driver')
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid credentials for this role.')
    } finally {
      setLoading(false)
    }
  }

  const selected = ROLES.find(r => r.key === role)

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
            <h1 className="mt-3 font-display text-4xl font-extrabold text-white">One operations login for the whole fleet.</h1>
            <p className="mt-4 max-w-sm text-slate-400">
              Choose your role and sign in with your normal credentials. Conductors can scan passenger tickets after signing in.
            </p>
          </div>

          <div className="animate-rise stagger-1 rounded-3xl border border-signal-cyan/20 bg-ops-800/80 p-8 shadow-glow">
            <Logo dark className="mb-6 lg:hidden" />

            <div className="mb-6 grid grid-cols-3 gap-1 rounded-xl border border-white/10 bg-ops-900/70 p-1">
              {ROLES.map((r) => (
                <button key={r.key} type="button"
                  onClick={() => { setRole(r.key); setError(''); }}
                  className={`rounded-lg py-2 text-xs font-bold transition ${role === r.key ? 'bg-signal-cyan text-ops-950' : 'text-slate-400 hover:text-slate-200'}`}>
                  {r.key === 'CONDUCTOR' ? 'Conductor' : r.key === 'DRIVER' ? 'Driver' : 'Admin'}
                </button>
              ))}
            </div>

            <h2 className="font-display text-2xl font-bold text-white">{selected.label} sign-in</h2>
            <p className="mt-1 text-sm text-slate-400">{selected.hint}</p>

            <form onSubmit={submit} className="mt-6 space-y-4">
              <label className="block">
                <span className="mb-1.5 block text-xs font-bold uppercase tracking-wider text-slate-400">
                  {role === 'ADMIN' ? 'Admin email' : 'Employee email'}
                </span>
                <input type="email" required value={form.email}
                  onChange={e => setForm({ ...form, email: e.target.value })}
                  placeholder={role === 'ADMIN' ? 'admin@catchbus.lk' : 'employee@catchbus.lk'}
                  className={`w-full rounded-xl border bg-ops-900/70 px-4 py-3 font-mono text-sm text-white placeholder-slate-600 outline-none focus:border-signal-cyan/60 ${submitted && !form.email ? 'border-signal-rose' : 'border-white/10'}`} />
              </label>
              <label className="block">
                <span className="mb-1.5 block text-xs font-bold uppercase tracking-wider text-slate-400">Password</span>
                <input type="password" required minLength={8} value={form.password}
                  onChange={e => setForm({ ...form, password: e.target.value })}
                  placeholder="••••••••"
                  className={`w-full rounded-xl border bg-ops-900/70 px-4 py-3 text-white placeholder-slate-600 outline-none focus:border-signal-cyan/60 ${submitted && !form.password ? 'border-signal-rose' : 'border-white/10'}`} />
              </label>

              {error && <p className="rounded-lg bg-signal-rose/10 px-3 py-2 text-sm text-signal-rose">{error}</p>}

              <button disabled={loading} className="w-full rounded-xl bg-signal-cyan py-3 font-bold text-ops-950 transition hover:brightness-95 disabled:opacity-60">
                {loading ? 'Authenticating…' : `Sign in as ${role === 'CONDUCTOR' ? 'Conductor' : role === 'DRIVER' ? 'Driver' : 'Administrator'}`}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}

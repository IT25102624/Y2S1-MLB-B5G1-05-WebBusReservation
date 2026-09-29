import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import TicketCard from '../components/TicketCard.jsx'
import Logo from '../components/Logo.jsx'

export default function PassengerLogin() {
  const { loginPassenger } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await loginPassenger(form)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.message || 'Could not sign in. Check your details and try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      eyebrow="Passenger sign-in"
      title="Welcome back."
      subtitle="Signing in is optional — you can always book as a guest instead."
    >
      <form onSubmit={submit} className="space-y-4">
        <FormField label="Email address" type="email" value={form.email}
          onChange={(v) => setForm({ ...form, email: v })} placeholder="you@example.com" required />
        <FormField label="Password" type="password" value={form.password}
          onChange={(v) => setForm({ ...form, password: v })} placeholder="••••••••" required />

        {error && <p className="rounded-lg bg-signal-rose/10 px-3 py-2 text-sm text-signal-rose">{error}</p>}

        <button disabled={loading} className="w-full rounded-xl bg-brand-500 py-3 font-bold text-white transition hover:bg-brand-600 disabled:opacity-60">
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-ink-700/60">
        New here? <Link to="/register" className="font-bold text-brand-600 hover:underline">Create an account</Link>
      </p>
      <p className="mt-2 text-center text-sm text-ink-700/50">
        Or skip this entirely — <Link to="/" className="underline hover:text-ink-800">book as a guest</Link>.
      </p>
    </AuthShell>
  )
}

export function AuthShell({ eyebrow, title, subtitle, children }) {
  return (
    <div className="relative flex min-h-[80vh] items-center justify-center bg-gradient-to-b from-brand-50 to-sand-50 px-6 py-16">
      <TicketCard className="relative w-full max-w-md animate-rise p-8">
        <Logo className="mb-6" />
        <span className="text-xs font-bold uppercase tracking-widest text-brand-600">{eyebrow}</span>
        <h1 className="mt-2 font-display text-3xl font-extrabold text-ink-950">{title}</h1>
        {subtitle && <p className="mt-2 text-sm text-ink-700/60">{subtitle}</p>}
        <div className="mt-8">{children}</div>
      </TicketCard>
    </div>
  )
}

export function FormField({ label, type = 'text', value, onChange, placeholder, required, mono }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-bold uppercase tracking-wider text-ink-700/50">{label}</span>
      <input
        type={type}
        value={value}
        required={required}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className={`w-full rounded-xl border border-ink-900/10 bg-sand-50 px-4 py-3 text-ink-950 placeholder-ink-400 outline-none transition focus:border-brand-500/60 ${mono ? 'font-mono' : ''}`}
      />
    </label>
  )
}

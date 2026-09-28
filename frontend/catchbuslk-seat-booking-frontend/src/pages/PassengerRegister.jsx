import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { AuthShell, FormField } from './PassengerLogin.jsx'

export default function PassengerRegister() {
  const { registerPassenger } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', phoneNumber: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await registerPassenger(form)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.details?.[0] || 'Registration failed. Please check your details.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      eyebrow="Create an account"
      title="Save your trips."
      subtitle="Purely optional — you can also book without ever creating one."
    >
      <form onSubmit={submit} className="space-y-4">
        <FormField label="Full name" value={form.fullName} onChange={(v) => setForm({ ...form, fullName: v })} placeholder="Jane Perera" required />
        <FormField label="Email address" type="email" value={form.email} onChange={(v) => setForm({ ...form, email: v })} placeholder="you@example.com" required />
        <FormField label="Phone number" value={form.phoneNumber} onChange={(v) => setForm({ ...form, phoneNumber: v })} placeholder="+94 77 123 4567" required />
        <FormField label="Password" type="password" value={form.password} onChange={(v) => setForm({ ...form, password: v })} placeholder="At least 8 characters" required />

        {error && <p className="rounded-lg bg-signal-rose/10 px-3 py-2 text-sm text-signal-rose">{error}</p>}

        <button disabled={loading} className="w-full rounded-xl bg-brand-500 py-3 font-bold text-white transition hover:bg-brand-600 disabled:opacity-60">
          {loading ? 'Creating account…' : 'Create account'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-ink-700/60">
        Already registered? <Link to="/login" className="font-bold text-brand-600 hover:underline">Sign in</Link>
      </p>
    </AuthShell>
  )
}

import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { AuthShell, FormField } from './PassengerLogin.jsx'

export default function PassengerRegister() {
  const { registerPassenger } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', nic: '', email: '', phoneNumber: '', password: '' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      await registerPassenger(form)
      setSuccess('Account created successfully! Redirecting…')
      setTimeout(() => {
        navigate('/')
      }, 800)
    } catch (err) {
      const msg = err.response?.data?.message || (typeof err.response?.data === 'string' ? err.response?.data : null) || 'Registration failed. Please verify your details.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      eyebrow="Create an account"
      title="Save your trips &amp; luggage."
      subtitle="Create a passenger account to track luggage, receive SMS alerts, and submit reviews."
    >
      <form onSubmit={submit} className="space-y-4">
        <FormField
          label="Full name"
          value={form.fullName}
          onChange={(v) => setForm({ ...form, fullName: v })}
          placeholder="e.g. Kasun Dilhara"
          required
        />
        <FormField
          label="National Identity Card (NIC)"
          value={form.nic}
          onChange={(v) => setForm({ ...form, nic: v })}
          placeholder="e.g. 2000261230V (or leave blank for auto-generate)"
        />
        <FormField
          label="Email address"
          type="email"
          value={form.email}
          onChange={(v) => setForm({ ...form, email: v })}
          placeholder="you@example.com"
          required
        />
        <FormField
          label="Phone number"
          value={form.phoneNumber}
          onChange={(v) => setForm({ ...form, phoneNumber: v })}
          placeholder="+94 77 123 4567"
          required
        />
        <FormField
          label="Password"
          type="password"
          value={form.password}
          onChange={(v) => setForm({ ...form, password: v })}
          placeholder="At least 6 characters"
          required
        />

        {error && <p className="rounded-lg bg-signal-rose/10 px-3 py-2 text-sm text-signal-rose">{error}</p>}
        {success && <p className="rounded-lg bg-brand-500/10 px-3 py-2 text-sm text-brand-700 font-semibold">{success}</p>}

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-brand-500 py-3 font-bold text-white shadow-card transition hover:bg-brand-600 disabled:opacity-60"
        >
          {loading ? 'Creating account…' : 'Create account'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-ink-700/60">
        Already registered? <Link to="/login" className="font-bold text-brand-600 hover:underline">Sign in</Link>
      </p>
    </AuthShell>
  )
}

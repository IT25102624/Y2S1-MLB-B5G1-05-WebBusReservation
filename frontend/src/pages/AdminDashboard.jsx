import React, { useEffect, useState } from 'react'
import client from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import OperationsMessages from '../components/OperationsMessages.jsx'

const ROLE_COLORS = {
  ADMIN: 'bg-white text-ops-950',
  CONDUCTOR: 'bg-signal-cyan/20 text-signal-cyan',
  DRIVER: 'bg-amber-500/20 text-amber-400',
  PASSENGER: 'bg-white/10 text-slate-300'
}

export default function AdminDashboard() {
  const { user } = useAuth()
  const [users, setUsers] = useState([])
  const [roleFilter, setRoleFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [positionSaving, setPositionSaving] = useState(null)

  const load = async (role) => {
    setLoading(true)
    try {
      const { data } = await client.get('/admin/users', { params: role ? { role } : {} })
      setUsers(data)
    } catch (err) {
      setError('Failed to load users. Are you signed in as an admin?')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load(roleFilter) }, [roleFilter])

  const updateStatus = async (id, status) => {
    await client.patch(`/admin/users/${id}/rbac`, { status })
    load(roleFilter)
  }

  const deactivate = async (id) => {
    if (!confirm('Deactivate this account?')) return
    await client.delete(`/admin/users/${id}`)
    load(roleFilter)
  }

  const updatePosition = async (id, role) => {
    setPositionSaving(id)
    try {
      await client.patch(`/admin/users/${id}/position`, { role })
      await load(roleFilter)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not change this staff position.')
    } finally {
      setPositionSaving(null)
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-12">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <span className="text-xs font-semibold uppercase tracking-[0.3em] text-slate-500">Control room</span>
          <h1 className="mt-1 font-display text-3xl text-white">User &amp; role management</h1>
          <p className="mt-1 text-sm text-slate-500">Signed in as {user?.fullName} · {user?.email}</p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="rounded-lg bg-amber-500 px-4 py-2.5 text-sm font-semibold text-ops-950 hover:bg-amber-400"
        >
          + Provision staff account
        </button>
        <a href="/register?admin=1" className="rounded-lg border border-white/15 px-3 py-2 text-xs font-semibold text-slate-300 hover:border-amber-400 hover:text-amber-300">
          Create passenger user
        </a>
      </div>

      <div className="mt-8 flex gap-2">
        {['', 'PASSENGER', 'CONDUCTOR', 'DRIVER', 'ADMIN'].map((r) => (
          <button
            key={r || 'ALL'}
            onClick={() => setRoleFilter(r)}
            className={`rounded-full border px-4 py-1.5 text-xs font-semibold uppercase tracking-wide transition ${
              roleFilter === r ? 'border-white bg-white text-ops-950' : 'border-white/10 text-slate-400 hover:text-slate-200'
            }`}
          >
            {r || 'All'}
          </button>
        ))}
      </div>

      <div className="mt-6 overflow-hidden rounded-2xl border border-white/10">
        <table className="w-full text-left text-sm">
          <thead className="bg-ops-800 text-xs uppercase tracking-wider text-slate-500">
            <tr>
              <th className="px-5 py-3">Name</th>
              <th className="px-5 py-3">Email</th>
              <th className="px-5 py-3">Role</th>
              <th className="px-5 py-3">Status</th>
              <th className="px-5 py-3">Employee ID</th>
              <th className="px-5 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5 bg-ops-900/60">
            {loading && (
              <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-500">Loading users…</td></tr>
            )}
            {error && (
              <tr><td colSpan={6} className="px-5 py-8 text-center text-signal-rose">{error}</td></tr>
            )}
            {!loading && !error && users.length === 0 && (
              <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-500">No users found.</td></tr>
            )}
            {users.map((u) => (
              <tr key={u.id} className="hover:bg-white/[0.02]">
                <td className="px-5 py-3 text-slate-100">{u.fullName}</td>
                <td className="px-5 py-3 font-mono text-xs text-slate-400">{u.email}</td>
                <td className="px-5 py-3">
                  <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${ROLE_COLORS[u.role]}`}>{u.role}</span>
                </td>
                <td className="px-5 py-3 text-slate-400">{u.status}</td>
                <td className="px-5 py-3 font-mono text-xs text-slate-500">{u.employeeId || '—'}</td>
                <td className="px-5 py-3 text-right space-x-2">
                  {(u.role === 'CONDUCTOR' || u.role === 'DRIVER') && (
                    <select
                      value={u.role}
                      disabled={positionSaving === u.id}
                      onChange={(e) => updatePosition(u.id, e.target.value)}
                      aria-label={`Change position for ${u.fullName}`}
                      className="mr-2 rounded-md border border-white/10 bg-ops-950 px-2 py-1 text-xs text-slate-200 outline-none focus:border-amber-500/60 disabled:opacity-50"
                    >
                      <option value="CONDUCTOR">Conductor</option>
                      <option value="DRIVER">Driver</option>
                    </select>
                  )}
                  {u.status !== 'ACTIVE' && (
                    <button onClick={() => updateStatus(u.id, 'ACTIVE')} className="text-xs font-semibold text-signal-cyan hover:underline">Activate</button>
                  )}
                  {u.status === 'ACTIVE' && (
                    <button onClick={() => updateStatus(u.id, 'SUSPENDED')} className="text-xs font-semibold text-amber-400 hover:underline">Suspend</button>
                  )}
                  <button onClick={() => deactivate(u.id)} className="text-xs font-semibold text-signal-rose hover:underline">Deactivate</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <OperationsMessages admin />

      {showCreate && <CreateStaffModal onClose={() => setShowCreate(false)} onCreated={() => { setShowCreate(false); load(roleFilter) }} />}
    </div>
  )
}

function CreateStaffModal({ onClose, onCreated }) {
  const [form, setForm] = useState({ fullName: '', email: '', phoneNumber: '', employeeId: '', role: 'CONDUCTOR', assignedBusPlate: '', temporaryPassword: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await client.post('/admin/users/staff', form)
      onCreated()
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.details?.[0] || 'Could not create account.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/60 px-6 backdrop-blur-sm">
      <form onSubmit={submit} className="w-full max-w-lg rounded-2xl border border-white/10 bg-ops-900 p-8">
        <h2 className="font-display text-xl text-white">Provision a staff account</h2>
        <p className="mt-1 text-sm text-slate-500">Conductor and Driver accounts are created by an admin — they never self-register.</p>

        <div className="mt-6 grid grid-cols-2 gap-4">
          <TextInput label="Full name" value={form.fullName} onChange={set('fullName')} required />
          <TextInput label="Employee ID" value={form.employeeId} onChange={set('employeeId')} required mono />
          <TextInput label="Email" type="email" value={form.email} onChange={set('email')} required className="col-span-2" />
          <TextInput label="Phone" value={form.phoneNumber} onChange={set('phoneNumber')} required />
          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-500">Role</span>
            <select value={form.role} onChange={set('role')} className="w-full rounded-lg border border-white/10 bg-ops-950 px-3 py-2.5 text-white">
              <option value="CONDUCTOR">Conductor</option>
              <option value="DRIVER">Driver</option>
            </select>
          </label>
          <TextInput label="Assigned bus plate (optional)" value={form.assignedBusPlate} onChange={set('assignedBusPlate')} mono />
          <TextInput label="Temporary password" type="password" value={form.temporaryPassword} onChange={set('temporaryPassword')} required className="col-span-2" />
        </div>

        {error && <p className="mt-4 rounded-lg bg-signal-rose/10 px-3 py-2 text-sm text-signal-rose">{error}</p>}

        <div className="mt-6 flex justify-end gap-3">
          <button type="button" onClick={onClose} className="rounded-lg px-4 py-2 text-sm text-slate-400 hover:text-slate-200">Cancel</button>
          <button disabled={loading} className="rounded-lg bg-amber-500 px-5 py-2 text-sm font-semibold text-ops-950 hover:bg-amber-400 disabled:opacity-60">
            {loading ? 'Creating…' : 'Create account'}
          </button>
        </div>
      </form>
    </div>
  )
}

function TextInput({ label, type = 'text', value, onChange, required, mono, className = '' }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-500">{label}</span>
      <input
        type={type} required={required} value={value} onChange={onChange}
        className={`w-full rounded-lg border border-white/10 bg-ops-950 px-3 py-2.5 text-white outline-none focus:border-amber-500/60 ${mono ? 'font-mono text-sm' : ''}`}
      />
    </label>
  )
}

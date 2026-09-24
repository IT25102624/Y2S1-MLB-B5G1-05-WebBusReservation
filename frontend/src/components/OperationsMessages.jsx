import React, { useCallback, useEffect, useState } from 'react'
import client from '../api/client'

export default function OperationsMessages({ admin = false }) {
  const [messages, setMessages] = useState([])
  const [form, setForm] = useState({ subject: '', message: '', urgent: false })
  const [status, setStatus] = useState('')

  const load = useCallback(async () => {
    try {
      const { data } = await client.get(admin ? '/admin/messages' : '/staff/messages')
      setMessages(data)
    } catch (error) {
      setStatus(error.response?.data?.message || 'Could not load operations messages.')
    }
  }, [admin])

  useEffect(() => { load() }, [load])

  const submit = async (event) => {
    event.preventDefault()
    setStatus('')
    try {
      await client.post('/staff/messages', form)
      setForm({ subject: '', message: '', urgent: false })
      setStatus('Message sent to the operations team.')
      load()
    } catch (error) {
      setStatus(error.response?.data?.message || 'Could not send the message.')
    }
  }

  return (
    <section className="mt-5 rounded-3xl border border-white/10 bg-ops-800/70 p-6">
      <h2 className="font-display text-xl font-bold text-white">
        {admin ? 'Urgent staff messages' : 'Report an issue'}
      </h2>
      <p className="mt-1 text-sm text-slate-500">
        {admin ? 'Messages from conductors and drivers are shown here, with urgent reports first.' : 'Send route, vehicle, passenger or safety issues directly to operations.'}
      </p>
      {!admin && (
        <form onSubmit={submit} className="mt-5 grid gap-3">
          <input required value={form.subject} onChange={e => setForm({ ...form, subject: e.target.value })}
            placeholder="Subject" className="rounded-xl border border-white/10 bg-ops-900 px-4 py-3 text-white outline-none focus:border-signal-cyan/60" />
          <textarea required rows="3" value={form.message} onChange={e => setForm({ ...form, message: e.target.value })}
            placeholder="Describe the issue and what support you need"
            className="rounded-xl border border-white/10 bg-ops-900 px-4 py-3 text-white outline-none focus:border-signal-cyan/60" />
          <label className="flex items-center gap-2 text-sm text-slate-300">
            <input type="checkbox" checked={form.urgent} onChange={e => setForm({ ...form, urgent: e.target.checked })} />
            Mark as urgent
          </label>
          <button className="w-fit rounded-xl bg-signal-cyan px-5 py-3 font-bold text-ops-950">Send report</button>
        </form>
      )}
      {status && <p className="mt-3 text-sm text-signal-cyan">{status}</p>}
      <div className="mt-5 space-y-3">
        {messages.length === 0 && <p className="text-sm text-slate-500">No messages yet.</p>}
        {messages.map(item => (
          <article key={item.messageId} className={`rounded-2xl border p-4 ${item.urgent ? 'border-signal-rose/50 bg-signal-rose/10' : 'border-white/10 bg-ops-900/60'}`}>
            <div className="flex flex-wrap justify-between gap-2">
              <h3 className="font-bold text-white">{item.subject}</h3>
              {item.urgent && <span className="text-xs font-bold uppercase tracking-wider text-signal-rose">Urgent</span>}
            </div>
            <p className="mt-2 text-sm text-slate-300">{item.message}</p>
            <p className="mt-2 text-xs text-slate-500">{item.senderName} · {item.senderRole} · {new Date(item.createdAt).toLocaleString()}</p>
          </article>
        ))}
      </div>
    </section>
  )
}

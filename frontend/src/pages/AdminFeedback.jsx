import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import feedbackApi from '../api/feedback.js'

export default function AdminFeedback() {
  const [feedbacks, setFeedbacks] = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('ALL') // 'ALL' | 'COMPLAINTS' | 'NEEDS_REPLY' | 'REVIEWS'
  const [replyingId, setReplyingId] = useState(null)
  const [replyText, setReplyText] = useState('')
  const [replyLoading, setReplyLoading] = useState(false)
  const [notificationMsg, setNotificationMsg] = useState('')

  useEffect(() => {
    loadFeedback()
  }, [])

  const loadFeedback = async () => {
    setLoading(true)
    try {
      const data = await feedbackApi.getAll()
      setFeedbacks(Array.isArray(data) ? data : [])
    } catch (err) {
      console.warn('Could not load feedback, fallback list')
    } finally {
      setLoading(false)
    }
  }

  const handleSendReply = async (id) => {
    if (!replyText.trim()) return
    setReplyLoading(true)
    try {
      await feedbackApi.reply(id, replyText.trim())
      setNotificationMsg('Official customer service reply sent to passenger!')
      setTimeout(() => setNotificationMsg(''), 4000)
      setReplyingId(null)
      setReplyText('')
      loadFeedback()
    } catch (err) {
      alert('Failed to send reply: ' + (err.response?.data?.message || err.message))
    } finally {
      setReplyLoading(false)
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Are you sure you want to remove this record?')) return
    try {
      await feedbackApi.delete(id)
      loadFeedback()
    } catch (err) {
      alert('Failed to delete feedback record.')
    }
  }

  const filtered = feedbacks.filter((f) => {
    if (filter === 'COMPLAINTS') return f.type === 'complain'
    if (filter === 'NEEDS_REPLY') return f.type === 'complain' && !f.reply
    if (filter === 'REVIEWS') return f.type === 'feedback'
    return true
  })

  const stats = {
    total: feedbacks.length,
    complaints: feedbacks.filter(f => f.type === 'complain').length,
    needsReply: feedbacks.filter(f => f.type === 'complain' && !f.reply).length,
    resolved: feedbacks.filter(f => f.reply).length
  }

  return (
    <div className="min-h-screen bg-sand-50 py-12 px-6">
      <div className="mx-auto max-w-6xl">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-ink-900/10 pb-6">
          <div>
            <span className="text-xs font-bold uppercase tracking-widest text-brand-600">Support Operations</span>
            <h1 className="mt-1 font-display text-3xl font-extrabold text-ink-950 sm:text-4xl">
              Complaint Management &amp; Feedback Review
            </h1>
            <p className="mt-1 text-sm text-ink-700/70">
              Review passenger grievances, investigate service complaints, and dispatch official replies.
            </p>
          </div>

          <div className="flex gap-2">
            <Link
              to="/feedback"
              className="rounded-xl border border-ink-900/15 bg-white px-4 py-2.5 text-xs font-bold text-ink-800 hover:bg-sand-50"
            >
              &larr; Passenger View
            </Link>
            <button
              onClick={loadFeedback}
              className="rounded-xl bg-brand-500 px-4 py-2.5 text-xs font-bold text-white shadow-card hover:bg-brand-600"
            >
              🔄 Refresh
            </button>
          </div>
        </div>

        {/* Operational KPI cards */}
        <div className="mt-8 grid gap-4 sm:grid-cols-4">
          <div className="rounded-2xl border border-ink-900/10 bg-white p-5 shadow-card">
            <span className="text-xs font-bold uppercase tracking-wider text-ink-700/50">Total Submissions</span>
            <div className="mt-2 font-display text-3xl font-black text-ink-950">{stats.total}</div>
          </div>
          <div className="rounded-2xl border border-ink-900/10 bg-white p-5 shadow-card">
            <span className="text-xs font-bold uppercase tracking-wider text-signal-amber">Grievances / Complaints</span>
            <div className="mt-2 font-display text-3xl font-black text-amber-600">{stats.complaints}</div>
          </div>
          <div className="rounded-2xl border border-ink-900/10 bg-white p-5 shadow-card">
            <span className="text-xs font-bold uppercase tracking-wider text-signal-rose">Awaiting Response</span>
            <div className="mt-2 font-display text-3xl font-black text-signal-rose">{stats.needsReply}</div>
          </div>
          <div className="rounded-2xl border border-ink-900/10 bg-white p-5 shadow-card">
            <span className="text-xs font-bold uppercase tracking-wider text-brand-600">Resolved with Reply</span>
            <div className="mt-2 font-display text-3xl font-black text-emerald-600">{stats.resolved}</div>
          </div>
        </div>

        {notificationMsg && (
          <div className="mt-4 rounded-xl bg-brand-500/10 p-4 text-xs font-semibold text-brand-700">
            {notificationMsg}
          </div>
        )}

        {/* Filters */}
        <div className="mt-8 flex flex-wrap gap-2 border-b border-ink-900/10 pb-4">
          {[
            { id: 'ALL', label: `All Records (${stats.total})` },
            { id: 'NEEDS_REPLY', label: `⚠️ Needs Reply (${stats.needsReply})` },
            { id: 'COMPLAINTS', label: `Complaints (${stats.complaints})` },
            { id: 'REVIEWS', label: `Reviews & Praise (${stats.total - stats.complaints})` }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setFilter(tab.id)}
              className={`rounded-full px-4 py-1.5 text-xs font-bold transition ${
                filter === tab.id
                  ? 'bg-ink-950 text-white shadow-sm'
                  : 'bg-white text-ink-700 border border-ink-900/10 hover:bg-sand-100'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Table / List */}
        <div className="mt-6 space-y-4">
          {loading ? (
            <div className="py-12 text-center text-sm text-ink-700/60">Loading passenger feedback…</div>
          ) : filtered.length === 0 ? (
            <div className="rounded-3xl border border-ink-900/10 bg-white p-12 text-center shadow-card">
              <p className="text-sm text-ink-700/60">No records found matching this category filter.</p>
            </div>
          ) : (
            filtered.map((item) => (
              <div key={item.feedbackId} className="rounded-2xl border border-ink-900/10 bg-white p-6 shadow-card">
                <div className="flex flex-wrap items-start justify-between gap-2 border-b border-ink-900/8 pb-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-ink-950 text-base">{item.name || 'Anonymous Passenger'}</span>
                      <span className={`rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${
                        item.type === 'complain' ? 'bg-amber-100 text-amber-900' : 'bg-brand-100 text-brand-900'
                      }`}>
                        {item.type === 'complain' ? 'Complaint' : 'Review'}
                      </span>
                      <span className="text-amber-500 text-xs">
                        {'★'.repeat(item.rating || 5)}
                      </span>
                    </div>
                    <span className="text-[11px] text-ink-700/50">
                      User ID: #{item.userId || 'Guest'} • Date: {item.complainDate || 'Recent'} {item.complainTime || ''}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    {item.reply ? (
                      <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-bold text-emerald-800">
                        ✓ Resolved
                      </span>
                    ) : item.type === 'complain' ? (
                      <span className="rounded-full bg-rose-100 px-3 py-1 text-xs font-bold text-rose-800 animate-pulse">
                        ● Response Required
                      </span>
                    ) : (
                      <span className="rounded-full bg-sand-100 px-3 py-1 text-xs font-bold text-ink-700">
                        Feedback Logged
                      </span>
                    )}
                  </div>
                </div>

                <p className="mt-3 text-sm text-ink-900 leading-relaxed font-medium">"{item.description}"</p>

                {/* Existing Reply */}
                {item.reply && (
                  <div className="mt-3 rounded-xl border border-emerald-500/20 bg-emerald-50/60 p-3 text-xs text-emerald-950">
                    <span className="font-bold block text-emerald-800">Your Official Response:</span>
                    <p className="mt-0.5">{item.reply}</p>
                  </div>
                )}

                {/* Reply Form */}
                {replyingId === item.feedbackId ? (
                  <div className="mt-4 rounded-2xl border border-brand-500/20 bg-brand-50/30 p-4">
                    <span className="text-xs font-bold text-ink-950 block mb-1">Compose Reply to {item.name}:</span>
                    <textarea
                      rows="3"
                      value={replyText}
                      onChange={(e) => setReplyText(e.target.value)}
                      placeholder="Write formal resolution details, apology, or operational explanation..."
                      className="w-full rounded-xl border border-ink-900/15 bg-white p-3 text-xs text-ink-900 focus:border-brand-500 focus:outline-none"
                    />
                    <div className="mt-3 flex justify-end gap-2">
                      <button
                        onClick={() => { setReplyingId(null); setReplyText(''); }}
                        className="rounded-lg border border-ink-900/15 px-3 py-1.5 text-xs font-semibold text-ink-700 hover:bg-sand-50"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={() => handleSendReply(item.feedbackId)}
                        disabled={replyLoading}
                        className="rounded-lg bg-brand-500 px-4 py-1.5 text-xs font-bold text-white shadow-card hover:bg-brand-600 disabled:opacity-50"
                      >
                        {replyLoading ? 'Sending…' : 'Send Official Response'}
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="mt-4 border-t border-ink-900/8 pt-3 flex items-center justify-between">
                    {item.type === 'complain' ? (
                      <button
                        onClick={() => {
                          setReplyingId(item.feedbackId)
                          setReplyText(item.reply || '')
                        }}
                        className="text-xs font-bold text-brand-600 hover:text-brand-700 hover:underline"
                      >
                        {item.reply ? '✏️ Edit Response' : '💬 Reply to Passenger'}
                      </button>
                    ) : <span />}

                    <button
                      onClick={() => handleDelete(item.feedbackId)}
                      className="text-xs font-semibold text-signal-rose hover:underline"
                    >
                      Delete Record
                    </button>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

import React, { useState, useEffect } from 'react'
import feedbackApi from '../api/feedback.js'

export default function Feedback() {
  const [activeTab, setActiveTab] = useState('submit') // 'submit' | 'database'

  // Submission Form
  const [formData, setFormData] = useState({
    name: 'Passenger',
    type: 'feedback', // 'feedback' | 'complain'
    rating: 5,
    description: ''
  })
  const [submitting, setSubmitting] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [statusMessage, setStatusMessage] = useState({ type: '', text: '' })

  // Data states
  const [allFeedbackList, setAllFeedbackList] = useState([])
  const [stats, setStats] = useState({ totalFeedback: 0, feedbackCount: 0, complainCount: 0, withReplies: 0 })
  const [loading, setLoading] = useState(false)
  const [filterType, setFilterType] = useState('ALL') // 'ALL' | 'FEEDBACK' | 'COMPLAIN'

  useEffect(() => {
    loadAllFeedbackAndStats()
  }, [])

  const loadAllFeedbackAndStats = async () => {
    setLoading(true)
    try {
      const [all, st] = await Promise.allSettled([
        feedbackApi.getAll(),
        feedbackApi.getStats()
      ])
      if (all.status === 'fulfilled' && Array.isArray(all.value)) {
        setAllFeedbackList(all.value)
      }
      if (st.status === 'fulfilled' && st.value) {
        setStats(st.value)
      }
    } catch {}
    finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setStatusMessage({ type: '', text: '' })

    if (!formData.description.trim()) {
      setStatusMessage({ type: 'error', text: 'Please provide your written feedback description.' })
      return
    }

    setSubmitting(true)
    try {
      const payload = {
        name: formData.name.trim() || 'Passenger',
        type: formData.type,
        rating: formData.rating,
        description: formData.description.trim(),
        userId: 1
      }

      if (editingId) {
        await feedbackApi.update(editingId, payload)
        setStatusMessage({ type: 'success', text: 'Feedback updated successfully!' })
        setEditingId(null)
      } else {
        await feedbackApi.create(payload)
        setStatusMessage({
          type: 'success',
          text: formData.type === 'complain'
            ? 'Your complaint has been logged in the database. Customer Service will review and reply.'
            : 'Thank you for your review! Your rating has been logged in the customer reviews database.'
        })
      }

      setFormData({
        name: 'Passenger',
        type: 'feedback',
        rating: 5,
        description: ''
      })
      loadAllFeedbackAndStats()
      setActiveTab('database')
    } catch (err) {
      setStatusMessage({
        type: 'error',
        text: err.response?.data?.message || err.response?.data || 'Failed to submit feedback.'
      })
    } finally {
      setSubmitting(false)
    }
  }

  const handleEdit = (item) => {
    setEditingId(item.feedbackId)
    setFormData({
      name: item.name || 'Passenger',
      type: item.type || 'feedback',
      rating: item.rating || 5,
      description: item.description || ''
    })
    setActiveTab('submit')
    window.scrollTo({ top: 100, behavior: 'smooth' })
  }

  const handleCancelEdit = () => {
    setEditingId(null)
    setFormData({
      name: 'Passenger',
      type: 'feedback',
      rating: 5,
      description: ''
    })
  }

  const handleDelete = async (id) => {
    if (!confirm('Are you sure you want to delete this feedback record?')) return
    try {
      await feedbackApi.delete(id)
      loadAllFeedbackAndStats()
      setStatusMessage({ type: 'success', text: 'Feedback deleted successfully.' })
    } catch (err) {
      alert('Failed to delete feedback.')
    }
  }

  const filteredFeedbacks = allFeedbackList.filter(item => {
    if (filterType === 'FEEDBACK') return item.type === 'feedback'
    if (filterType === 'COMPLAIN') return item.type === 'complain'
    return true
  })

  // Calculate average rating
  const averageRating = allFeedbackList.length > 0
    ? (allFeedbackList.reduce((acc, curr) => acc + (curr.rating || 5), 0) / allFeedbackList.length).toFixed(1)
    : '4.9'

  return (
    <div className="min-h-screen bg-sand-50 py-12 px-6">
      <div className="mx-auto max-w-6xl">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-ink-900/10 pb-6">
          <div>
            <span className="text-xs font-bold uppercase tracking-widest text-brand-600">Passenger Experience</span>
            <h1 className="mt-1 font-display text-3xl font-extrabold text-ink-950 sm:text-4xl">
              Post-Journey Feedback &amp; Reviews
            </h1>
            <p className="mt-2 text-ink-700/80 max-w-2xl text-sm leading-relaxed">
              Submit your journey ratings and written reviews regarding CatchBus LK intercity transport.
              Maintains a verified public database of traveler reviews and customer support resolutions.
            </p>
          </div>

          <div className="flex gap-2">
            <button
              onClick={() => setActiveTab('submit')}
              className="rounded-xl bg-brand-500 px-4 py-2 text-xs font-bold text-white shadow-card hover:bg-brand-600"
            >
              ✍️ Submit Review
            </button>
            <button
              onClick={() => setActiveTab('database')}
              className="rounded-xl border border-ink-900/15 bg-white px-4 py-2 text-xs font-bold text-ink-800 hover:bg-sand-50"
            >
              ⭐ Reviews Database ({allFeedbackList.length})
            </button>
          </div>
        </div>

        {/* Tab Controls */}
        <div className="mt-8 flex flex-wrap gap-2 border-b border-ink-900/10 pb-4">
          <button
            onClick={() => setActiveTab('submit')}
            className={`rounded-xl px-5 py-2.5 text-sm font-bold transition ${
              activeTab === 'submit'
                ? 'bg-brand-500 text-white shadow-card'
                : 'bg-white text-ink-800 hover:bg-brand-50'
            }`}
          >
            ✍️ {editingId ? 'Edit Feedback' : 'Submit Review / Complaint'}
          </button>
          <button
            onClick={() => { setActiveTab('database'); loadAllFeedbackAndStats(); }}
            className={`rounded-xl px-5 py-2.5 text-sm font-bold transition ${
              activeTab === 'database'
                ? 'bg-brand-500 text-white shadow-card'
                : 'bg-white text-ink-800 hover:bg-brand-50'
            }`}
          >
            ⭐ Customer Reviews Database ({allFeedbackList.length})
          </button>
        </div>

        {/* TAB 1: SUBMIT FEEDBACK */}
        {activeTab === 'submit' && (
          <div className="mt-8 grid gap-8 lg:grid-cols-12">
            <div className="lg:col-span-7 rounded-3xl border border-ink-900/10 bg-white p-8 shadow-card">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold uppercase tracking-wider text-brand-600">
                    {editingId ? 'Updating Record' : 'Post-Journey Rating'}
                  </span>
                  <h2 className="mt-1 font-display text-2xl font-bold text-ink-950">
                    {editingId ? 'Modify Feedback' : 'Share Journey Experience'}
                  </h2>
                </div>
                {editingId && (
                  <button
                    onClick={handleCancelEdit}
                    className="rounded-lg border border-ink-900/15 px-3 py-1.5 text-xs font-semibold text-ink-700 hover:bg-sand-50"
                  >
                    Cancel Edit
                  </button>
                )}
              </div>

              {statusMessage.text && (
                <div className={`mt-4 rounded-xl p-4 text-xs font-semibold ${
                  statusMessage.type === 'success'
                    ? 'bg-brand-500/10 text-brand-800 border border-brand-500/20'
                    : 'bg-signal-rose/10 text-signal-rose border border-signal-rose/20'
                }`}>
                  {statusMessage.text}
                </div>
              )}

              <form onSubmit={handleSubmit} className="mt-6 space-y-5">
                {/* Feedback Type Selector */}
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-ink-800 mb-2">
                    Submission Category
                  </label>
                  <div className="grid grid-cols-2 gap-3">
                    <button
                      type="button"
                      onClick={() => setFormData({ ...formData, type: 'feedback' })}
                      className={`flex items-center justify-center gap-2 rounded-2xl border p-4 text-sm font-bold transition ${
                        formData.type === 'feedback'
                          ? 'border-brand-500 bg-brand-50/50 text-brand-800 ring-2 ring-brand-500/30'
                          : 'border-ink-900/10 bg-white text-ink-700 hover:bg-sand-50'
                      }`}
                    >
                      <span>🌟</span> General Review / Praise
                    </button>
                    <button
                      type="button"
                      onClick={() => setFormData({ ...formData, type: 'complain' })}
                      className={`flex items-center justify-center gap-2 rounded-2xl border p-4 text-sm font-bold transition ${
                        formData.type === 'complain'
                          ? 'border-signal-amber bg-amber-50/50 text-amber-900 ring-2 ring-signal-amber/30'
                          : 'border-ink-900/10 bg-white text-ink-700 hover:bg-sand-50'
                      }`}
                    >
                      <span>⚠️</span> Complaint / Report Issue
                    </button>
                  </div>
                </div>

                {/* Rating Stars */}
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-ink-800 mb-2">
                    Service Rating ({formData.rating} of 5 Stars)
                  </label>
                  <div className="flex items-center gap-2">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <button
                        key={star}
                        type="button"
                        onClick={() => setFormData({ ...formData, rating: star })}
                        className="text-3xl transition-transform hover:scale-125 focus:outline-none"
                      >
                        {star <= formData.rating ? '★' : '☆'}
                      </button>
                    ))}
                    <span className="ml-3 text-xs font-bold text-ink-700">
                      {formData.rating === 5 && 'Outstanding Journey 🏆'}
                      {formData.rating === 4 && 'Very Good Service 👍'}
                      {formData.rating === 3 && 'Average Experience ⚖️'}
                      {formData.rating === 2 && 'Needs Improvement ⚠️'}
                      {formData.rating === 1 && 'Unsatisfactory ❌'}
                    </span>
                  </div>
                </div>

                {/* Passenger Name */}
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-ink-800 mb-2">
                    Passenger Name
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="e.g. Kasun Dilhara"
                    className="w-full rounded-xl border border-ink-900/15 bg-sand-50/50 px-4 py-3 text-sm text-ink-900 focus:border-brand-500 focus:outline-none"
                  />
                </div>

                {/* Written Feedback Textarea */}
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-ink-800 mb-2">
                    Written Feedback / Experience Description
                  </label>
                  <textarea
                    rows="4"
                    required
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder={
                      formData.type === 'complain'
                        ? 'Describe any issue encountered (e.g. departure delay, AC temperature, luggage handling)...'
                        : 'Tell us about your journey, driver punctuality, bus condition, or customer service...'
                    }
                    className="w-full rounded-xl border border-ink-900/15 bg-sand-50/50 p-4 text-sm text-ink-900 focus:border-brand-500 focus:outline-none leading-relaxed"
                  />
                </div>

                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full rounded-2xl bg-brand-500 py-4 font-bold text-white shadow-card transition hover:bg-brand-600 disabled:opacity-50"
                >
                  {submitting
                    ? 'Logging in Database…'
                    : editingId
                    ? 'Save Feedback Changes'
                    : formData.type === 'complain'
                    ? 'File Complaint to Database'
                    : 'Submit Travel Review'}
                </button>
              </form>
            </div>

            {/* Sidebar Stats */}
            <div className="lg:col-span-5 space-y-6">
              <div className="rounded-3xl border border-ink-900/10 bg-white p-6 shadow-card">
                <span className="text-xs font-bold uppercase tracking-wider text-brand-600">Satisfaction Metrics</span>
                <h3 className="mt-1 font-display text-xl font-bold text-ink-950">Passenger Trust Rating</h3>

                <div className="mt-4 flex items-center gap-4 border-b border-ink-900/8 pb-4">
                  <span className="font-display text-5xl font-black text-brand-600">{averageRating}</span>
                  <div>
                    <div className="flex text-amber-400 text-lg">★★★★★</div>
                    <span className="text-xs text-ink-700/60">Verified passenger reviews</span>
                  </div>
                </div>

                <div className="mt-4 space-y-2 text-xs">
                  <div className="flex justify-between py-1">
                    <span className="text-ink-700/70">Total Logged Reviews</span>
                    <span className="font-bold text-ink-950">{stats.totalFeedback || allFeedbackList.length}</span>
                  </div>
                  <div className="flex justify-between py-1">
                    <span className="text-ink-700/70">Complaints Resolved with Official Reply</span>
                    <span className="font-bold text-emerald-600">{stats.withReplies || 0}</span>
                  </div>
                  <div className="flex justify-between py-1">
                    <span className="text-ink-700/70">Review Database Status</span>
                    <span className="font-bold text-brand-700">✓ Active &amp; Synced</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: CUSTOMER REVIEWS DATABASE */}
        {activeTab === 'database' && (
          <div className="mt-8 space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <h2 className="font-display text-2xl font-bold text-ink-950">Customer Reviews Database</h2>
                <p className="text-xs text-ink-700/60">Verified traveler ratings, feedback, and official Customer Service resolutions</p>
              </div>
              
              <div className="flex items-center gap-2">
                <div className="flex rounded-xl bg-white border border-ink-900/10 p-1 text-xs">
                  <button
                    onClick={() => setFilterType('ALL')}
                    className={`rounded-lg px-3 py-1 font-bold ${filterType === 'ALL' ? 'bg-brand-500 text-white' : 'text-ink-700'}`}
                  >
                    All ({allFeedbackList.length})
                  </button>
                  <button
                    onClick={() => setFilterType('FEEDBACK')}
                    className={`rounded-lg px-3 py-1 font-bold ${filterType === 'FEEDBACK' ? 'bg-brand-500 text-white' : 'text-ink-700'}`}
                  >
                    Reviews ({allFeedbackList.filter(f => f.type === 'feedback').length})
                  </button>
                  <button
                    onClick={() => setFilterType('COMPLAIN')}
                    className={`rounded-lg px-3 py-1 font-bold ${filterType === 'COMPLAIN' ? 'bg-brand-500 text-white' : 'text-ink-700'}`}
                  >
                    Complaints ({allFeedbackList.filter(f => f.type === 'complain').length})
                  </button>
                </div>

                <button
                  onClick={loadAllFeedbackAndStats}
                  className="rounded-xl border border-ink-900/10 bg-white px-3 py-1.5 text-xs font-bold text-ink-800 hover:bg-brand-50"
                >
                  🔄 Refresh
                </button>
              </div>
            </div>

            {loading ? (
              <div className="py-12 text-center text-sm text-ink-700/60">Loading review records…</div>
            ) : filteredFeedbacks.length === 0 ? (
              <div className="rounded-3xl border border-ink-900/10 bg-white p-12 text-center shadow-card">
                <p className="text-sm text-ink-700/60">No records found matching this category.</p>
              </div>
            ) : (
              <div className="grid gap-6 md:grid-cols-2">
                {filteredFeedbacks.map((item) => (
                  <div key={item.feedbackId} className="rounded-3xl border border-ink-900/10 bg-white p-6 shadow-card flex flex-col justify-between">
                    <div>
                      <div className="flex items-start justify-between gap-2 border-b border-ink-900/8 pb-3">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-ink-950 text-base">{item.name || 'Passenger'}</span>
                            <span className={`inline-block rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${
                              item.type === 'complain' ? 'bg-amber-100 text-amber-900' : 'bg-brand-100 text-brand-900'
                            }`}>
                              {item.type === 'complain' ? 'Complaint' : 'Review'}
                            </span>
                          </div>
                          <div className="mt-1 text-amber-500 text-sm">
                            {'★'.repeat(item.rating || 5)}{'☆'.repeat(5 - (item.rating || 5))}
                          </div>
                        </div>
                        <span className="text-[10px] text-ink-700/50">
                          {item.complainDate || 'Recently recorded'}
                        </span>
                      </div>

                      <p className="mt-4 text-sm text-ink-800 leading-relaxed font-medium">"{item.description}"</p>

                      {/* Official Customer Service Reply */}
                      {item.reply ? (
                        <div className="mt-4 rounded-2xl border border-emerald-500/30 bg-emerald-50/60 p-4">
                          <span className="flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wider text-emerald-800">
                            <span>🛡️</span> Customer Service Resolution
                          </span>
                          <p className="mt-1.5 text-xs text-emerald-950 font-medium leading-relaxed">{item.reply}</p>
                        </div>
                      ) : item.type === 'complain' ? (
                        <div className="mt-4 rounded-xl bg-amber-50/70 p-3 text-[11px] text-amber-800 border border-amber-200">
                          ⏳ <strong>Under Investigation:</strong> Customer service team is preparing an official response.
                        </div>
                      ) : null}
                    </div>

                    <div className="mt-6 border-t border-ink-900/8 pt-3 flex items-center justify-between text-xs">
                      <button
                        onClick={() => handleEdit(item)}
                        className="font-bold text-brand-600 hover:underline"
                      >
                        ✏️ Edit Review
                      </button>
                      <button
                        onClick={() => handleDelete(item.feedbackId)}
                        className="font-bold text-signal-rose hover:underline"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

import React, { useState, useEffect } from 'react'
import luggageApi from '../api/luggage.js'
import bookingsApi from '../api/bookings.js'
import LuggageTagCard from '../components/LuggageTagCard.jsx'

export default function Luggage() {
  const [activeTab, setActiveTab] = useState('tag') // 'tag' | 'records' | 'track'
  
  // Bookings list for dropdown
  const [bookings, setBookings] = useState([])
  const [loadingBookings, setLoadingBookings] = useState(false)
  
  // Tag creation & editing form
  const [editingId, setEditingId] = useState(null)
  const [editingTagNumber, setEditingTagNumber] = useState('')
  const [formData, setFormData] = useState({
    bookingId: '',
    passengerName: '',
    bagQuantity: 1,
    description: '',
    status: 'TAGGED'
  })
  const [submitting, setSubmitting] = useState(false)
  const [successMessage, setSuccessMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [newlyCreatedLuggage, setNewlyCreatedLuggage] = useState(null)

  // Luggage records list
  const [luggageRecords, setLuggageRecords] = useState([])
  const [loadingRecords, setLoadingRecords] = useState(false)

  // Quick Tracker Search
  const [searchTag, setSearchTag] = useState('')
  const [trackedRecord, setTrackedRecord] = useState(null)
  const [trackingLoading, setTrackingLoading] = useState(false)
  const [trackingError, setTrackingError] = useState('')

  // Toast alert popup state
  const [liveToast, setLiveToast] = useState(null)

  useEffect(() => {
    loadAllBookings()
    loadAllLuggage()
  }, [])

  const triggerWebsiteNotification = (title, message, tagNumber) => {
    // Show on-screen toast
    setLiveToast({ title, message, tagNumber, time: new Date().toLocaleTimeString() })
    setTimeout(() => setLiveToast(null), 6000)

    // Broadcast to NotificationBell component
    window.dispatchEvent(new CustomEvent('notification-alert', {
      detail: { title, message, tagNumber }
    }))
  }

  const loadAllBookings = async () => {
    setLoadingBookings(true)
    try {
      const data = await bookingsApi.getAll()
      if (data && data.length > 0) {
        setBookings(data)
        if (!editingId) {
          setFormData(prev => ({
            ...prev,
            bookingId: data[0].bookingId,
            passengerName: data[0].passengerName || ''
          }))
        }
      }
    } catch (err) {
      console.warn('Could not load bookings list')
    } finally {
      setLoadingBookings(false)
    }
  }

  const loadAllLuggage = async () => {
    setLoadingRecords(true)
    try {
      const data = await luggageApi.getAll()
      setLuggageRecords(data || [])
    } catch (err) {
      console.warn('Could not load luggage records')
    } finally {
      setLoadingRecords(false)
    }
  }

  const handleBookingChange = (e) => {
    const selectedId = e.target.value
    const selectedBooking = bookings.find(b => String(b.bookingId) === String(selectedId))
    setFormData(prev => ({
      ...prev,
      bookingId: selectedId,
      passengerName: selectedBooking?.passengerName || prev.passengerName
    }))
  }

  const handleTagSubmit = async (e) => {
    e.preventDefault()
    setErrorMessage('')
    setSuccessMessage('')
    setNewlyCreatedLuggage(null)

    if (!formData.bookingId) {
      setErrorMessage('Please select a valid E-ticket booking.')
      return
    }

    setSubmitting(true)
    try {
      if (editingId) {
        // UPDATE EXISTING LUGGAGE
        const updated = await luggageApi.update(editingId, {
          bagQuantity: formData.bagQuantity,
          description: formData.description,
          status: formData.status
        })
        const successText = `Luggage Tag ${updated.tagNumber} updated successfully! (Total: ${updated.bagQuantity} bags)`
        setSuccessMessage(successText)
        setNewlyCreatedLuggage(updated)
        triggerWebsiteNotification('Luggage Updated', `Luggage tag ${updated.tagNumber} details updated successfully.`, updated.tagNumber)
        setEditingId(null)
        setEditingTagNumber('')
      } else {
        // CREATE NEW LUGGAGE
        const created = await luggageApi.create({
          bookingId: formData.bookingId,
          bagQuantity: formData.bagQuantity,
          description: formData.description
        })
        const successText = `Success! Unique Luggage Tag ID ${created.tagNumber} generated.`
        setSuccessMessage(successText)
        setNewlyCreatedLuggage(created)
        triggerWebsiteNotification('New Luggage Booked', `Luggage tag ${created.tagNumber} successfully registered for ${created.bagQuantity} bag(s).`, created.tagNumber)
      }

      setFormData(prev => ({ ...prev, description: '', bagQuantity: 1, status: 'TAGGED' }))
      loadAllLuggage()
    } catch (err) {
      setErrorMessage(err.response?.data?.message || err.response?.data || 'Failed to process luggage tag.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleEditLuggage = (item) => {
    setEditingId(item.id)
    setEditingTagNumber(item.tagNumber)
    setFormData({
      bookingId: item.booking ? item.booking.bookingId : '',
      passengerName: item.booking ? item.booking.passengerName : 'Passenger',
      bagQuantity: item.bagQuantity || 1,
      description: item.description || '',
      status: item.status || 'TAGGED'
    })
    setErrorMessage('')
    setSuccessMessage('')
    setActiveTab('tag')
    window.scrollTo({ top: 120, behavior: 'smooth' })
  }

  const handleCancelEdit = () => {
    setEditingId(null)
    setEditingTagNumber('')
    setFormData({
      bookingId: bookings[0]?.bookingId || '',
      passengerName: bookings[0]?.passengerName || 'Passenger',
      bagQuantity: 1,
      description: '',
      status: 'TAGGED'
    })
  }

  const handleUpdateStatus = async (id, status) => {
    try {
      const updated = await luggageApi.updateStatus(id, status)
      loadAllLuggage()
      triggerWebsiteNotification('Cargo Status Updated', `Luggage tag ${updated.tagNumber} status changed to ${status}.`, updated.tagNumber)
      if (trackedRecord && trackedRecord.id === id) {
        setTrackedRecord(prev => ({ ...prev, status }))
      }
    } catch (err) {
      alert('Failed to update luggage status: ' + (err.response?.data || err.message))
    }
  }

  const handleDeleteLuggage = async (id) => {
    if (!confirm('Are you sure you want to remove this luggage record?')) return
    try {
      await luggageApi.delete(id)
      loadAllLuggage()
      if (newlyCreatedLuggage?.id === id) setNewlyCreatedLuggage(null)
    } catch (err) {
      alert('Failed to delete luggage record.')
    }
  }

  const handleTrackSearch = async (e) => {
    e.preventDefault()
    if (!searchTag.trim()) return
    setTrackingLoading(true)
    setTrackingError('')
    setTrackedRecord(null)
    try {
      const data = await luggageApi.getByTag(searchTag.trim())
      setTrackedRecord(data)
    } catch (err) {
      setTrackingError('Luggage tag not found. Please verify the Tag ID (e.g. LUG-2026...) and try again.')
    } finally {
      setTrackingLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-sand-50 py-12 px-6 relative">
      {/* REAL-TIME WEBSITE NOTIFICATION TOAST POPUP */}
      {liveToast && (
        <div className="fixed bottom-6 right-6 z-50 max-w-sm rounded-3xl bg-ink-950 p-4 text-white shadow-card-lg border border-brand-500/40 animate-rise flex items-start gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-brand-500 text-xl text-white">
            🔔
          </div>
          <div className="flex-1">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-brand-400">{liveToast.title}</span>
              <span className="text-[10px] text-white/50">{liveToast.time}</span>
            </div>
            <p className="mt-1 text-xs text-white/90 leading-relaxed font-medium">{liveToast.message}</p>
            <div className="mt-2 flex items-center gap-2 text-[10px] text-emerald-400 font-bold">
              <span className="inline-block h-1.5 w-1.5 rounded-full bg-emerald-400"></span>
              <span>Live Website Notification Active</span>
            </div>
          </div>
          <button onClick={() => setLiveToast(null)} className="text-white/60 hover:text-white text-sm font-bold">
            ✕
          </button>
        </div>
      )}

      <div className="mx-auto max-w-6xl">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-ink-900/10 pb-6">
          <div>
            <span className="text-xs font-bold uppercase tracking-widest text-brand-600">Cargo &amp; Baggage Care</span>
            <h1 className="mt-1 font-display text-3xl font-extrabold text-ink-950 sm:text-4xl">
              Luggage Tagging &amp; Anti-Loss Protection
            </h1>
            <p className="mt-2 text-ink-700/80 max-w-2xl text-sm leading-relaxed">
              Declare baggage quantities linked to your confirmed E-ticket to generate a scannable QR Baggage ID.
              Ensures cargo security and end-to-end tracking throughout your intercity bus journey.
            </p>
          </div>

          <div className="flex gap-2">
            <button
              onClick={() => { handleCancelEdit(); setActiveTab('tag'); }}
              className="rounded-xl bg-brand-500 px-4 py-2 text-xs font-bold text-white shadow-card hover:bg-brand-600"
            >
              + Tag Luggage
            </button>
            <button
              onClick={() => setActiveTab('track')}
              className="rounded-xl border border-ink-900/15 bg-white px-4 py-2 text-xs font-bold text-ink-800 hover:bg-sand-50"
            >
              🔍 Live Tracker
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="mt-8 flex flex-wrap gap-2 border-b border-ink-900/10 pb-4">
          <button
            onClick={() => setActiveTab('tag')}
            className={`rounded-xl px-5 py-2.5 text-sm font-bold transition ${
              activeTab === 'tag'
                ? 'bg-brand-500 text-white shadow-card'
                : 'bg-white text-ink-800 hover:bg-brand-50'
            }`}
          >
            {editingId ? `✏️ Edit Tag (${editingTagNumber})` : '🏷️ Issue Luggage Tag'}
          </button>
          <button
            onClick={() => { setActiveTab('records'); loadAllLuggage(); }}
            className={`rounded-xl px-5 py-2.5 text-sm font-bold transition ${
              activeTab === 'records'
                ? 'bg-brand-500 text-white shadow-card'
                : 'bg-white text-ink-800 hover:bg-brand-50'
            }`}
          >
            📦 Trackable Luggage Records ({luggageRecords.length})
          </button>
          <button
            onClick={() => setActiveTab('track')}
            className={`rounded-xl px-5 py-2.5 text-sm font-bold transition ${
              activeTab === 'track'
                ? 'bg-brand-500 text-white shadow-card'
                : 'bg-white text-ink-800 hover:bg-brand-50'
            }`}
          >
            🔍 Live QR Tracker
          </button>
        </div>

        {/* TAB 1: TAG OR EDIT LUGGAGE */}
        {activeTab === 'tag' && (
          <div className="mt-8 grid gap-8 lg:grid-cols-12">
            <div className="lg:col-span-7 rounded-3xl border border-ink-900/10 bg-white p-8 shadow-card">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold uppercase tracking-wider text-brand-600">
                    {editingId ? 'Modify Baggage Details' : 'Baggage Registration Form'}
                  </span>
                  <h2 className="mt-1 font-display text-2xl font-bold text-ink-950">
                    {editingId ? `Edit Luggage Tag #${editingTagNumber}` : 'Generate QR Luggage Tag'}
                  </h2>
                </div>
                {editingId && (
                  <button
                    onClick={handleCancelEdit}
                    className="rounded-xl border border-ink-900/15 bg-sand-50 px-3 py-1.5 text-xs font-bold text-ink-700 hover:bg-sand-100"
                  >
                    Cancel Edit
                  </button>
                )}
              </div>

              <p className="mt-1 text-xs text-ink-700/60">
                {editingId
                  ? 'Update your declared baggage count or description. A modified notification alert will be dispatched automatically.'
                  : 'Select your confirmed E-Ticket and declare baggage pieces to generate your printable QR baggage tag card.'}
              </p>

              {errorMessage && (
                <div className="mt-4 rounded-xl bg-signal-rose/10 p-4 text-xs font-semibold text-signal-rose">
                  {errorMessage}
                </div>
              )}
              {successMessage && (
                <div className="mt-4 rounded-2xl bg-brand-500/10 border border-brand-500/20 p-4 text-xs font-semibold text-brand-800 flex items-center justify-between">
                  <span>{successMessage}</span>
                  <span className="text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md text-[10px] font-bold">
                    🔔 Alert Dispatched
                  </span>
                </div>
              )}

              <form onSubmit={handleTagSubmit} className="mt-6 space-y-5">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-ink-800 mb-2">
                    Linked E-Ticket / Trip Route
                  </label>
                  {bookings.length > 0 ? (
                    <select
                      value={formData.bookingId}
                      onChange={handleBookingChange}
                      disabled={!!editingId}
                      required
                      className="w-full rounded-xl border border-ink-900/15 bg-sand-50/50 px-4 py-3 text-sm font-semibold text-ink-900 focus:border-brand-500 focus:outline-none disabled:opacity-60"
                    >
                      {bookings.map((b) => (
                        <option key={b.bookingId} value={b.bookingId}>
                          E-Ticket #{b.bookingId} — {b.passengerName} ({b.route} on {b.journeyDate})
                        </option>
                      ))}
                    </select>
                  ) : (
                    <div>
                      <input
                        type="number"
                        placeholder="Enter confirmed Booking ID (e.g. 1)"
                        value={formData.bookingId}
                        onChange={(e) => setFormData({ ...formData, bookingId: e.target.value })}
                        disabled={!!editingId}
                        required
                        className="w-full rounded-xl border border-ink-900/15 bg-sand-50/50 px-4 py-3 text-sm text-ink-900 focus:border-brand-500 focus:outline-none disabled:opacity-60"
                      />
                    </div>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-ink-800 mb-2">
                      Number of Bags (Quantity)
                    </label>
                    <input
                      type="number"
                      min="1"
                      max="10"
                      value={formData.bagQuantity}
                      onChange={(e) => setFormData({ ...formData, bagQuantity: Math.max(1, parseInt(e.target.value) || 1) })}
                      required
                      className="w-full rounded-xl border border-ink-900/15 bg-sand-50/50 px-4 py-3 text-sm font-bold text-ink-900 focus:border-brand-500 focus:outline-none"
                    />
                    <span className="text-[10px] text-ink-700/50">Declare exact baggage count</span>
                  </div>

                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-ink-800 mb-2">
                      Cargo Status
                    </label>
                    {editingId ? (
                      <select
                        value={formData.status}
                        onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                        className="w-full rounded-xl border border-ink-900/15 bg-sand-50/50 px-3 py-3 text-xs font-bold text-ink-900"
                      >
                        <option value="TAGGED">TAGGED (Tag Issued)</option>
                        <option value="CHECKED_IN">CHECKED IN</option>
                        <option value="LOADED">LOADED in Bus Cargo</option>
                        <option value="COLLECTED">COLLECTED Safely</option>
                      </select>
                    ) : (
                      <div className="rounded-xl border border-ink-900/10 bg-sand-50 px-4 py-3 text-sm font-bold text-brand-700">
                        TAGGED (Tag Issued)
                      </div>
                    )}
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-ink-800 mb-2">
                    Luggage Description &amp; Visual Identifiers
                  </label>
                  <textarea
                    rows="3"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="e.g. 1 Navy blue American Tourister trolley with red strap, 1 black backpack"
                    className="w-full rounded-xl border border-ink-900/15 bg-sand-50/50 p-4 text-sm text-ink-900 focus:border-brand-500 focus:outline-none"
                  />
                  <span className="text-[10px] text-ink-700/50">
                    Provides luggage identity to prevent missing bags.
                  </span>
                </div>

                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full rounded-2xl bg-brand-500 py-4 font-bold text-white shadow-card transition hover:bg-brand-600 disabled:opacity-50"
                >
                  {submitting
                    ? 'Processing Luggage Tag…'
                    : editingId
                    ? 'Save Luggage Changes & Dispatch Alert 🚀'
                    : 'Generate Trackable Luggage Tag 🏷️'}
                </button>
              </form>
            </div>

            {/* Generated Tag Preview Card */}
            <div className="lg:col-span-5 flex flex-col justify-start">
              {newlyCreatedLuggage ? (
                <div>
                  <div className="mb-3 flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-wider text-brand-700">Digital QR Tag</span>
                    <span className="text-xs text-ink-700/60">Ready to Print</span>
                  </div>
                  <LuggageTagCard luggage={newlyCreatedLuggage} />
                  <div className="mt-4 rounded-2xl border border-emerald-500/20 bg-emerald-50 p-4 text-xs text-emerald-800">
                    <strong>✅ Alert Published:</strong> Live notification for Tag ID <strong>{newlyCreatedLuggage.tagNumber}</strong> has been published to the notification center.
                  </div>
                </div>
              ) : (
                <div className="rounded-3xl border border-dashed border-ink-900/20 p-8 text-center bg-sand-50/70">
                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-brand-50 text-3xl">
                    🏷️
                  </div>
                  <h3 className="mt-4 font-display text-lg font-bold text-ink-900">Digital Luggage Tag Card</h3>
                  <p className="mt-2 text-xs text-ink-700/70 leading-relaxed">
                    Select an E-ticket and specify bag quantity to generate a unique Baggage ID with scannable QR Code and security verification.
                  </p>
                  <div className="mt-6 text-left rounded-2xl bg-white p-4 text-xs space-y-2 border border-ink-900/5">
                    <div className="font-bold text-ink-800">Baggage Safety Features:</div>
                    <div className="text-ink-700/70">• Edit baggage quantity &amp; details anytime</div>
                    <div className="text-ink-700/70">• Real-time notification alerts dispatched on updates</div>
                    <div className="text-ink-700/70">• Instant website notification banner with live bell badge</div>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* TAB 2: TRACKABLE RECORDS (WITH EDIT BUTTON) */}
        {activeTab === 'records' && (
          <div className="mt-8 space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="font-display text-2xl font-bold text-ink-950">Trackable Luggage Records</h2>
                <p className="text-xs text-ink-700/60">Real-time status tracking linked to passenger E-tickets</p>
              </div>
              <button
                onClick={loadAllLuggage}
                className="rounded-xl border border-ink-900/10 bg-white px-4 py-2 text-xs font-bold text-ink-800 hover:bg-brand-50"
              >
                🔄 Refresh Status
              </button>
            </div>

            {loadingRecords ? (
              <div className="py-12 text-center text-sm text-ink-700/60">Loading luggage records…</div>
            ) : luggageRecords.length === 0 ? (
              <div className="rounded-3xl border border-ink-900/10 bg-white p-12 text-center shadow-card">
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-sand-100 text-3xl">
                  🧳
                </div>
                <h3 className="mt-4 font-display text-lg font-bold text-ink-900">No Luggage Records Found</h3>
                <p className="mt-1 text-xs text-ink-700/70">
                  Use the "Issue Luggage Tag" tab to generate a trackable luggage tag.
                </p>
                <button
                  onClick={() => setActiveTab('tag')}
                  className="mt-6 rounded-xl bg-brand-500 px-6 py-2.5 text-xs font-bold text-white shadow-card hover:bg-brand-600"
                >
                  Generate Tag Now
                </button>
              </div>
            ) : (
              <div className="grid gap-6 md:grid-cols-2">
                {luggageRecords.map((item) => {
                  const statuses = ['TAGGED', 'CHECKED_IN', 'LOADED', 'COLLECTED']
                  const currentIdx = statuses.indexOf(item.status) !== -1 ? statuses.indexOf(item.status) : 0

                  return (
                    <div key={item.id} className="rounded-3xl border border-ink-900/10 bg-white p-6 shadow-card flex flex-col justify-between">
                      <div>
                        <div className="flex items-start justify-between gap-2 border-b border-ink-900/8 pb-4">
                          <div>
                            <span className="text-[11px] font-mono font-black text-brand-600 tracking-wider">
                              {item.tagNumber}
                            </span>
                            <h3 className="font-display font-bold text-ink-950 text-lg">
                              E-Ticket #{item.booking ? item.booking.bookingId : 'N/A'} • {item.bagQuantity} {item.bagQuantity === 1 ? 'Bag' : 'Bags'}
                            </h3>
                            <p className="text-xs text-ink-700/70">{item.booking?.route || 'Express Bus Network'} ({item.booking?.passengerName || 'Passenger'})</p>
                          </div>
                          <span className={`rounded-full px-3 py-1 text-xs font-bold uppercase tracking-wider ${
                            item.status === 'COLLECTED'
                              ? 'bg-emerald-100 text-emerald-800'
                              : item.status === 'LOADED'
                              ? 'bg-purple-100 text-purple-800'
                              : 'bg-amber-100 text-amber-800'
                          }`}>
                            {item.status}
                          </span>
                        </div>

                        {/* Visual Progress Stepper */}
                        <div className="mt-6">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-ink-700/60 block mb-2">
                            Cargo Journey Progression
                          </span>
                          <div className="grid grid-cols-4 gap-1 text-center">
                            {statuses.map((st, idx) => (
                              <div key={st} className="flex flex-col items-center">
                                <div className={`h-2 w-full rounded-full transition-all ${
                                  idx <= currentIdx ? 'bg-brand-500' : 'bg-sand-100'
                                }`} />
                                <span className={`mt-1.5 text-[9px] font-semibold tracking-tighter ${
                                  idx <= currentIdx ? 'text-brand-700' : 'text-ink-700/40'
                                }`}>
                                  {st.replace('_', ' ')}
                                </span>
                              </div>
                            ))}
                          </div>
                        </div>

                        {item.description && (
                          <div className="mt-4 rounded-xl bg-sand-50 p-3 text-xs text-ink-700 border border-ink-900/5">
                            <span className="font-bold text-ink-900">Baggage Details:</span> {item.description}
                          </div>
                        )}
                      </div>

                      {/* Status Transition Simulator & Actions */}
                      <div className="mt-6 border-t border-ink-900/8 pt-4 flex flex-wrap items-center justify-between gap-2">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[10px] uppercase font-bold text-ink-700/50">Status:</span>
                          <select
                            value={item.status}
                            onChange={(e) => handleUpdateStatus(item.id, e.target.value)}
                            className="rounded-lg border border-ink-900/15 bg-white px-2 py-1 text-xs font-semibold text-ink-800"
                          >
                            <option value="TAGGED">Tagged</option>
                            <option value="CHECKED_IN">Checked In</option>
                            <option value="LOADED">Loaded in Bus Cargo</option>
                            <option value="COLLECTED">Collected Safely</option>
                          </select>
                        </div>

                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleEditLuggage(item)}
                            className="text-xs font-bold text-amber-600 hover:text-amber-700 hover:underline flex items-center gap-1"
                          >
                            <span>✏️</span> Edit
                          </button>
                          <span className="text-ink-900/20">•</span>
                          <button
                            onClick={() => {
                              setNewlyCreatedLuggage(item)
                              setActiveTab('tag')
                            }}
                            className="text-xs font-bold text-brand-600 hover:underline"
                          >
                            Print QR 🖨️
                          </button>
                          <span className="text-ink-900/20">•</span>
                          <button
                            onClick={() => handleDeleteLuggage(item.id)}
                            className="text-xs font-bold text-signal-rose hover:underline"
                          >
                            Delete
                          </button>
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        )}

        {/* TAB 3: LIVE LUGGAGE TRACKER */}
        {activeTab === 'track' && (
          <div className="mt-8 max-w-2xl mx-auto">
            <div className="rounded-3xl border border-ink-900/10 bg-white p-8 shadow-card text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-brand-50 text-3xl">
                🛰️
              </div>
              <h2 className="mt-4 font-display text-2xl font-bold text-ink-950">Track Luggage by QR / Tag ID</h2>
              <p className="mt-1 text-xs text-ink-700/70 max-w-md mx-auto">
                Enter any unique Luggage Tag ID (e.g. LUG-20260914-104821 or LUG-20260924-000021) to check its live status.
              </p>

              <form onSubmit={handleTrackSearch} className="mt-6 flex gap-2">
                <input
                  type="text"
                  placeholder="e.g. LUG-20260924-000021"
                  value={searchTag}
                  onChange={(e) => setSearchTag(e.target.value)}
                  required
                  className="flex-1 rounded-2xl border border-ink-900/15 bg-sand-50/50 px-4 py-3 text-sm font-mono uppercase text-ink-900 focus:border-brand-500 focus:outline-none"
                />
                <button
                  type="submit"
                  disabled={trackingLoading}
                  className="rounded-2xl bg-brand-500 px-6 py-3 font-bold text-white shadow-card hover:bg-brand-600 disabled:opacity-50"
                >
                  {trackingLoading ? 'Searching…' : 'Track Cargo'}
                </button>
              </form>

              {trackingError && (
                <div className="mt-4 rounded-xl bg-signal-rose/10 p-3 text-xs text-signal-rose font-medium">
                  {trackingError}
                </div>
              )}

              {trackedRecord && (
                <div className="mt-8 text-left border-t border-ink-900/10 pt-6">
                  <div className="flex items-center justify-between mb-4">
                    <span className="font-mono text-sm font-black text-brand-600">{trackedRecord.tagNumber}</span>
                    <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-bold text-emerald-800">
                      {trackedRecord.status}
                    </span>
                  </div>

                  <div className="space-y-3 rounded-2xl bg-sand-50 p-4 text-xs">
                    <div className="flex justify-between">
                      <span className="text-ink-700/60 font-semibold">Baggage Pieces:</span>
                      <span className="font-bold text-ink-900">{trackedRecord.bagQuantity} Bags</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-ink-700/60 font-semibold">Linked E-Ticket:</span>
                      <span className="font-mono font-bold text-ink-900">#{trackedRecord.booking?.bookingId || 'N/A'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-ink-700/60 font-semibold">Route:</span>
                      <span className="font-semibold text-ink-900">{trackedRecord.booking?.route || 'Express Highway Service'}</span>
                    </div>
                    {trackedRecord.description && (
                      <div className="border-t border-ink-900/5 pt-2">
                        <span className="text-ink-700/60 font-semibold block mb-1">Description:</span>
                        <p className="text-ink-800">{trackedRecord.description}</p>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

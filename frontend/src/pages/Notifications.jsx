import React, { useState, useEffect } from 'react'
import notificationsApi from '../api/notifications.js'
import bookingsApi from '../api/bookings.js'

export default function Notifications() {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(false)
  const [filter, setFilter] = useState('ALL') // 'ALL' | 'UNREAD' | 'LUGGAGE' | 'BOOKING'

  // Booking list for optional selection in trigger
  const [bookings, setBookings] = useState([])

  // Live Alert Trigger state
  const [simulating, setSimulating] = useState(false)
  const [simForm, setSimForm] = useState({
    bookingId: '',
    message: 'Your bus journey schedule has been updated. Please arrive at the terminal 15 minutes before departure.',
    type: 'JOURNEY_REMINDER'
  })
  const [simResult, setSimResult] = useState(null)

  useEffect(() => {
    loadAllNotifications()
    loadBookings()

    const handleCustomAlert = () => {
      loadAllNotifications()
    }
    window.addEventListener('notification-alert', handleCustomAlert)
    return () => window.removeEventListener('notification-alert', handleCustomAlert)
  }, [])

  const loadBookings = async () => {
    try {
      const data = await bookingsApi.getAll()
      if (data && data.length > 0) {
        setBookings(data)
        setSimForm(prev => ({ ...prev, bookingId: String(data[0].bookingId) }))
      }
    } catch (err) {
      console.warn('Could not load bookings')
    }
  }

  const loadAllNotifications = async () => {
    setLoading(true)
    try {
      const data = await notificationsApi.getAll()
      setNotifications(Array.isArray(data) ? data : [])
    } catch (err) {
      console.warn('Could not load notifications', err)
    } finally {
      setLoading(false)
    }
  }

  const handleMarkAsRead = async (id) => {
    try {
      await notificationsApi.markAsRead(id)
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n))
      window.dispatchEvent(new CustomEvent('notification-alert'))
    } catch (err) {
      console.error(err)
    }
  }

  const handleMarkAllAsRead = async () => {
    try {
      await notificationsApi.markAllAsRead()
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })))
      window.dispatchEvent(new CustomEvent('notification-alert'))
    } catch (err) {
      console.error(err)
    }
  }

  const handleSendLiveAlert = async (e) => {
    e.preventDefault()
    setSimulating(true)
    setSimResult(null)
    try {
      const newNotif = await notificationsApi.create({
        bookingId: simForm.bookingId || null,
        message: simForm.message,
        type: simForm.type
      })
      setSimResult({
        success: true,
        message: 'Live notification published and delivered across website system!',
        timestamp: new Date().toLocaleTimeString()
      })
      setNotifications(prev => [newNotif, ...prev])
      window.dispatchEvent(new CustomEvent('notification-alert', { detail: newNotif }))
    } catch (err) {
      setSimResult({ success: false, error: 'Failed to send alert. Please ensure the server is active.' })
    } finally {
      setSimulating(false)
    }
  }

  const filteredList = notifications.filter(n => {
    if (filter === 'UNREAD') return !n.isRead
    if (filter === 'LUGGAGE') return (n.type || '').toUpperCase().includes('LUGGAGE')
    if (filter === 'BOOKING') return (n.type || '').toUpperCase().includes('BOOKING')
    return true
  })

  const formatTimestamp = (dateStr) => {
    if (!dateStr) return 'Just now'
    try {
      const d = new Date(dateStr)
      return d.toLocaleDateString([], { month: 'short', day: 'numeric' }) + ' at ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    } catch {
      return 'Just now'
    }
  }

  return (
    <div className="min-h-screen bg-sand-50 py-12 px-6">
      <div className="mx-auto max-w-6xl">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-ink-900/10 pb-6">
          <div>
            <span className="text-xs font-bold uppercase tracking-widest text-brand-600">Passenger Communications</span>
            <h1 className="mt-1 font-display text-3xl font-extrabold text-ink-950 sm:text-4xl">
              Live Notifications &amp; Alerts
            </h1>
            <p className="mt-2 text-ink-700/80 max-w-2xl text-sm leading-relaxed">
              Real-time platform notifications for booking confirmations, luggage tagging, cargo updates, and travel schedules.
            </p>
          </div>

          <div className="flex gap-2">
            <button
              onClick={handleMarkAllAsRead}
              className="rounded-xl border border-ink-900/15 bg-white px-4 py-2 text-xs font-bold text-ink-800 hover:bg-sand-50 transition"
            >
              ✓ Mark All as Read
            </button>
            <button
              onClick={loadAllNotifications}
              className="rounded-xl bg-brand-500 px-4 py-2 text-xs font-bold text-white shadow-card hover:bg-brand-600 transition"
            >
              🔄 Refresh Alerts
            </button>
          </div>
        </div>

        <div className="mt-8 grid gap-8 lg:grid-cols-12">
          {/* Main Notifications List */}
          <div className="lg:col-span-8 space-y-4">
            {/* Filter pills */}
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-ink-900/10 pb-3">
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => setFilter('ALL')}
                  className={`rounded-xl px-4 py-2 text-xs font-bold transition ${
                    filter === 'ALL' ? 'bg-brand-500 text-white shadow-card' : 'bg-white text-ink-800 hover:bg-sand-50'
                  }`}
                >
                  All Alerts ({notifications.length})
                </button>
                <button
                  onClick={() => setFilter('UNREAD')}
                  className={`rounded-xl px-4 py-2 text-xs font-bold transition ${
                    filter === 'UNREAD' ? 'bg-brand-500 text-white shadow-card' : 'bg-white text-ink-800 hover:bg-sand-50'
                  }`}
                >
                  Unread ({notifications.filter(n => !n.isRead).length})
                </button>
                <button
                  onClick={() => setFilter('LUGGAGE')}
                  className={`rounded-xl px-4 py-2 text-xs font-bold transition ${
                    filter === 'LUGGAGE' ? 'bg-brand-500 text-white shadow-card' : 'bg-white text-ink-800 hover:bg-sand-50'
                  }`}
                >
                  🧳 Luggage Alerts ({notifications.filter(n => (n.type || '').toUpperCase().includes('LUGGAGE')).length})
                </button>
                <button
                  onClick={() => setFilter('BOOKING')}
                  className={`rounded-xl px-4 py-2 text-xs font-bold transition ${
                    filter === 'BOOKING' ? 'bg-brand-500 text-white shadow-card' : 'bg-white text-ink-800 hover:bg-sand-50'
                  }`}
                >
                  🎟️ Booking Alerts ({notifications.filter(n => (n.type || '').toUpperCase().includes('BOOKING')).length})
                </button>
              </div>
              <span className="text-xs text-ink-700/50 font-medium">Live Website Feed Active</span>
            </div>

            {loading ? (
              <div className="py-12 text-center text-sm text-ink-700/60 font-medium">Loading notifications…</div>
            ) : filteredList.length === 0 ? (
              <div className="rounded-3xl border border-ink-900/10 bg-white p-12 text-center shadow-card">
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-sand-100 text-3xl">
                  🔔
                </div>
                <h3 className="mt-4 font-display text-lg font-bold text-ink-900">No Notifications in this filter</h3>
                <p className="mt-1 text-xs text-ink-700/70">
                  New notifications will appear here whenever bookings, luggage tags, or service alerts are created.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {filteredList.map((notif) => (
                  <div
                    key={notif.id}
                    className={`rounded-2xl border p-5 transition shadow-card flex flex-col justify-between ${
                      notif.isRead
                        ? 'border-ink-900/10 bg-white opacity-85'
                        : 'border-brand-500/30 bg-gradient-to-r from-brand-50/40 to-white ring-1 ring-brand-500/20'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex items-start gap-3.5">
                        <div className={`mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-xl ${
                          (notif.type || '').includes('LUGGAGE')
                            ? 'bg-purple-100 text-purple-700'
                            : (notif.type || '').includes('BOOKING')
                            ? 'bg-brand-100 text-brand-700'
                            : 'bg-sand-100 text-ink-800'
                        }`}>
                          {(notif.type || '').includes('LUGGAGE') ? '🧳' : (notif.type || '').includes('BOOKING') ? '🎟️' : '🔔'}
                        </div>

                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="font-bold text-xs uppercase tracking-wider text-ink-900">
                              {(notif.type || 'ALERT').replace(/_/g, ' ')}
                            </span>
                            {!notif.isRead && (
                              <span className="rounded-full bg-brand-500 px-2 py-0.5 text-[10px] font-extrabold text-white animate-pulse">
                                NEW
                              </span>
                            )}
                          </div>
                          <p className="mt-1.5 text-sm text-ink-900 leading-relaxed font-medium">
                            {notif.message}
                          </p>
                        </div>
                      </div>

                      {!notif.isRead && (
                        <button
                          onClick={() => handleMarkAsRead(notif.id)}
                          className="shrink-0 rounded-lg bg-brand-50 px-2.5 py-1 text-xs font-bold text-brand-600 hover:bg-brand-100 transition"
                        >
                          Mark Read
                        </button>
                      )}
                    </div>

                    {/* Footer Details */}
                    <div className="mt-3.5 flex flex-wrap items-center justify-between gap-2 border-t border-ink-900/5 pt-2.5 text-[11px]">
                      <div className="flex items-center gap-2">
                        <span className="flex items-center gap-1 font-semibold text-emerald-700 bg-emerald-50 px-2.5 py-0.5 rounded-md border border-emerald-500/20">
                          <span className="inline-block h-1.5 w-1.5 rounded-full bg-emerald-500"></span> Live Alert
                        </span>
                        {notif.route && (
                          <span className="font-semibold text-ink-700/70 bg-sand-100 px-2 py-0.5 rounded-md">
                            🚌 {notif.route}
                          </span>
                        )}
                      </div>
                      <span className="text-ink-700/60 font-medium">
                        {formatTimestamp(notif.createdAt)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Quick Notification Creator / Test Alert Dispatcher */}
          <div className="lg:col-span-4 space-y-6">
            <div className="rounded-3xl border border-ink-900/10 bg-white p-6 shadow-card">
              <span className="text-xs font-bold uppercase tracking-wider text-brand-600">Interactive Dispatch</span>
              <h3 className="mt-1 font-display text-lg font-bold text-ink-950">Send Notification Alert</h3>
              <p className="mt-1 text-xs text-ink-700/70 leading-relaxed">
                Generate and broadcast a real-time notification directly to the website alert feed and navbar bell.
              </p>

              {simResult && (
                <div className={`mt-4 rounded-xl p-3 text-xs ${
                  simResult.success ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-rose-50 text-rose-800'
                }`}>
                  {simResult.success ? (
                    <div>
                      <div className="font-bold">✓ Notification Sent Successfully!</div>
                      <div className="mt-1 text-[11px] text-emerald-700">{simResult.message}</div>
                      <div className="mt-1 text-[10px] text-emerald-600 font-mono">{simResult.timestamp}</div>
                    </div>
                  ) : simResult.error}
                </div>
              )}

              <form onSubmit={handleSendLiveAlert} className="mt-4 space-y-3">
                <div>
                  <label className="block text-[11px] font-bold uppercase text-ink-700 mb-1">
                    Alert Category
                  </label>
                  <select
                    value={simForm.type}
                    onChange={(e) => setSimForm({ ...simForm, type: e.target.value })}
                    className="w-full rounded-xl border border-ink-900/15 bg-sand-50/50 px-3 py-2 text-xs font-bold text-ink-900 focus:border-brand-500 focus:outline-none"
                  >
                    <option value="BOOKING_CONFIRMED">Booking Confirmed</option>
                    <option value="LUGGAGE_TAGGED">Luggage Tagged</option>
                    <option value="JOURNEY_REMINDER">Journey Departure Reminder</option>
                    <option value="SERVICE_ALERT">Service Delay Update</option>
                    <option value="SECURITY_NOTICE">Terminal Notice</option>
                  </select>
                </div>

                {bookings.length > 0 && (
                  <div>
                    <label className="block text-[11px] font-bold uppercase text-ink-700 mb-1">
                      Target E-Ticket Booking (Optional)
                    </label>
                    <select
                      value={simForm.bookingId}
                      onChange={(e) => setSimForm({ ...simForm, bookingId: e.target.value })}
                      className="w-full rounded-xl border border-ink-900/15 bg-sand-50/50 px-3 py-2 text-xs text-ink-900 focus:border-brand-500 focus:outline-none"
                    >
                      <option value="">-- General Alert (All Visitors) --</option>
                      {bookings.map(b => (
                        <option key={b.bookingId} value={b.bookingId}>
                          Ticket #{b.bookingId} ({b.passengerName} - {b.route})
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div>
                  <label className="block text-[11px] font-bold uppercase text-ink-700 mb-1">
                    Notification Message
                  </label>
                  <textarea
                    rows="3"
                    value={simForm.message}
                    onChange={(e) => setSimForm({ ...simForm, message: e.target.value })}
                    className="w-full rounded-xl border border-ink-900/15 bg-sand-50/50 p-3 text-xs text-ink-900 focus:border-brand-500 focus:outline-none"
                    placeholder="Enter notification text..."
                    required
                  />
                </div>

                <button
                  type="submit"
                  disabled={simulating}
                  className="w-full rounded-xl bg-brand-500 py-3 text-xs font-bold text-white shadow-card transition hover:bg-brand-600 disabled:opacity-50"
                >
                  {simulating ? 'Broadcasting…' : '🚀 Send Notification Alert'}
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

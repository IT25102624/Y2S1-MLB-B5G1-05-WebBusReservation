import React, { useCallback, useEffect, useState } from 'react'
import client from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import OperationsMessages from '../components/OperationsMessages.jsx'

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : '—'
}

export default function StaffDashboard({ role }) {
  const { user, sessionDashboard } = useAuth()
  const [dashboard, setDashboard] = useState(sessionDashboard)
  const [error, setError] = useState('')
  const [gpsMessage, setGpsMessage] = useState('')
  const [sharing, setSharing] = useState(false)

  const load = useCallback(async () => {
    try {
      const { data } = await client.get('/staff/dashboard')
      setDashboard(data)
      setError('')
    } catch (e) {
      setError(e.response?.data?.message || 'Could not load your staff schedule.')
    }
  }, [])

  useEffect(() => {
    load()
    const id = setInterval(load, 10000)
    return () => clearInterval(id)
  }, [load])

  const sendLocation = useCallback(() => {
    if (!navigator.geolocation) {
      setGpsMessage('Geolocation is not supported by this browser.')
      return
    }
    setGpsMessage('Reading GPS…')
    navigator.geolocation.getCurrentPosition(async (pos) => {
      try {
        await client.post('/staff/location', {
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
          speedKmh: pos.coords.speed == null ? null : pos.coords.speed * 3.6,
          headingDegrees: pos.coords.heading
        })
        setGpsMessage(`Bus GPS updated at ${new Date().toLocaleTimeString()}`)
      } catch (e) {
        setGpsMessage(e.response?.data?.message || 'Could not send bus location.')
      }
    }, () => setGpsMessage('Location permission was denied.'), { enableHighAccuracy: true, timeout: 10000 })
  }, [])

  useEffect(() => {
    if (!sharing) return
    sendLocation()
    const id = setInterval(sendLocation, 15000)
    return () => clearInterval(id)
  }, [sharing, sendLocation])

  const schedule = dashboard?.currentSchedule
  const previous = dashboard?.previousBus
  const location = dashboard?.previousBusLiveLocation

  return (
    <div className="min-h-screen bg-ops-950 px-6 py-12">
      <div className="mx-auto max-w-5xl">
        <span className="text-xs font-bold uppercase tracking-[0.3em] text-signal-cyan">
          {role === 'CONDUCTOR' ? 'Conductor console' : 'Driver console'}
        </span>
        <h1 className="mt-1 font-display text-3xl font-bold text-white">
          {role === 'CONDUCTOR' ? 'Boarding & route overview' : 'My route & live operations'}
        </h1>
        <p className="mt-2 text-sm text-slate-500">{user?.fullName} · {user?.employeeId || 'Employee'} · Bus {user?.assignedBusPlate || 'unassigned'}</p>

        {error && <p className="mt-6 rounded-xl bg-signal-rose/10 px-4 py-3 text-sm text-signal-rose">{error}</p>}

        <div className="mt-8 grid gap-5 lg:grid-cols-2">
          <section className="rounded-3xl border border-signal-cyan/20 bg-ops-800/70 p-6">
            <h2 className="font-display text-xl font-bold text-white">Your assigned schedule</h2>
            {schedule ? (
              <div className="mt-5 space-y-3 text-sm">
                <Row label="Bus" value={schedule.busNumber} />
                <Row label="Route" value={`${schedule.sourceCity} → ${schedule.destinationCity}`} />
                <Row label="Departure" value={formatDate(schedule.departureTime)} />
                <Row label="Arrival" value={formatDate(schedule.arrivalTime)} />
                <Row label="Status" value={schedule.tripStatus} />
              </div>
            ) : <p className="mt-5 text-sm text-slate-500">No active assignment was found for this account.</p>}
          </section>

          <section className="rounded-3xl border border-signal-amber/20 bg-ops-800/70 p-6">
            <h2 className="font-display text-xl font-bold text-white">Previous bus live location</h2>
            {previous ? (
              <>
                <div className="mt-5 space-y-3 text-sm">
                  <Row label="Bus" value={previous.busNumber} />
                  <Row label="Route" value={`${previous.sourceCity} → ${previous.destinationCity}`} />
                  <Row label="Trip departure" value={formatDate(previous.departureTime)} />
                  <Row label="Last GPS update" value={location ? formatDate(location.recordedAt) : 'No GPS update yet'} />
                  <Row label="Coordinates" value={location ? `${location.latitude}, ${location.longitude}` : '—'} />
                </div>
                {location && (
                  <div className="mt-5 overflow-hidden rounded-2xl border border-white/10">
                    <iframe title="Preceding bus live map" className="h-64 w-full border-0"
                      src={`https://www.openstreetmap.org/export/embed.html?bbox=${Number(location.longitude) - 0.03}%2C${Number(location.latitude) - 0.03}%2C${Number(location.longitude) + 0.03}%2C${Number(location.latitude) + 0.03}&layer=mapnik&marker=${location.latitude}%2C${location.longitude}`} />
                  </div>
                )}
              </>
            ) : <p className="mt-5 text-sm text-slate-500">No preceding trip was found on the same route.</p>}
          </section>
          <OperationsMessages />
        </div>

        <section className="mt-5 rounded-3xl border border-white/10 bg-ops-800/70 p-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="font-display text-xl font-bold text-white">Share this bus GPS</h2>
              <p className="mt-1 text-sm text-slate-500">
                Your current bus location becomes the live location that the following bus can see.
              </p>
            </div>
            <button type="button" onClick={() => setSharing(v => !v)}
              className={`rounded-xl px-5 py-3 font-bold ${sharing ? 'bg-signal-rose text-white' : 'bg-signal-cyan text-ops-950'}`}>
              {sharing ? 'Stop GPS sharing' : 'Start GPS sharing'}
            </button>
          </div>
          {gpsMessage && <p className="mt-3 text-xs text-slate-400">{gpsMessage}</p>}
        </section>
      </div>
    </div>
  )
}

function Row({ label, value }) {
  return <div className="flex justify-between gap-4 border-b border-white/5 pb-2">
    <span className="text-slate-500">{label}</span>
    <span className="text-right font-medium text-slate-200">{value}</span>
  </div>
}

import React from 'react'
import { Routes, Route, useLocation } from 'react-router-dom'
import Navbar from './components/Navbar.jsx'
import Footer from './components/Footer.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'

import Home from './pages/Home.jsx'
import Destinations from './pages/Destinations.jsx'
import Schedule from './pages/Schedule.jsx'
import PassengerLogin from './pages/PassengerLogin.jsx'
import PassengerRegister from './pages/PassengerRegister.jsx'
import StaffLogin from './pages/StaffLogin.jsx'
import AdminDashboard from './pages/AdminDashboard.jsx'
import ConductorDashboard from './pages/ConductorDashboard.jsx'

import Luggage from './pages/Luggage.jsx'
import Notifications from './pages/Notifications.jsx'
import Feedback from './pages/Feedback.jsx'
import AdminFeedback from './pages/AdminFeedback.jsx'

export default function App() {
  const location = useLocation()
  // The operations portal and its dashboards render their own full-bleed
  // dark backgrounds — skip the public site's footer there.
  const isOpsSurface = ['/staff/login', '/admin', '/conductor', '/driver'].some((p) => location.pathname.startsWith(p)) && !location.pathname.startsWith('/admin/feedback')

  return (
    <div className="min-h-screen bg-sand-50">
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/destinations" element={<Destinations />} />
        <Route path="/schedule" element={<Schedule />} />

        <Route path="/luggage" element={<Luggage />} />
        <Route path="/luggage/track" element={<Luggage />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/feedback" element={<Feedback />} />
        <Route path="/admin/feedback" element={<AdminFeedback />} />

        {/* Passenger — optional auth */}
        <Route path="/login" element={<PassengerLogin />} />
        <Route path="/register" element={<PassengerRegister />} />

        {/* Unified staff & admin sign-in */}
        <Route path="/staff/login" element={<StaffLogin />} />

        <Route
          path="/conductor"
          element={
            <ProtectedRoute roles={['CONDUCTOR', 'ADMIN']} redirectTo="/staff/login">
              <ConductorDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/driver"
          element={
            <ProtectedRoute roles={['DRIVER', 'ADMIN']} redirectTo="/staff/login">
              <div className="min-h-[80vh] bg-ops-950 px-6 py-12 text-slate-300">
                <div className="mx-auto max-w-3xl">
                  <span className="text-xs font-bold uppercase tracking-[0.3em] text-signal-amber">Driver console</span>
                  <h1 className="mt-1 font-display text-3xl font-bold text-white">Route &amp; schedule view</h1>
                  <p className="mt-3 text-slate-400">Owned by the Route &amp; Schedule Management module — wire up here.</p>
                </div>
              </div>
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin"
          element={
            <ProtectedRoute roles={['ADMIN']} redirectTo="/staff/login">
              <div className="min-h-screen bg-ops-950">
                <AdminDashboard />
              </div>
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<Home />} />
      </Routes>
      {!isOpsSurface && <Footer />}
    </div>
  )
}

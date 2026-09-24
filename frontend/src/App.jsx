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
import DriverDashboard from './pages/DriverDashboard.jsx'
import PassengerDashboard from './pages/PassengerDashboard.jsx'

export default function App() {
  const location = useLocation()
  // The operations portal and its dashboards render their own full-bleed
  // dark backgrounds — skip the public site's footer there.
  const isOpsSurface = ['/staff/login', '/admin', '/conductor', '/driver'].some((p) => location.pathname.startsWith(p))

  return (
    <div className="min-h-screen bg-sand-50">
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/destinations" element={<Destinations />} />
        <Route path="/schedule" element={<Schedule />} />

        {/* Passenger — optional auth */}
        <Route path="/login" element={<PassengerLogin />} />
        <Route path="/register" element={<PassengerRegister />} />
        <Route
          path="/passenger"
          element={
            <ProtectedRoute roles={['PASSENGER']} redirectTo="/login">
              <PassengerDashboard />
            </ProtectedRoute>
          }
        />

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
              <DriverDashboard />
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

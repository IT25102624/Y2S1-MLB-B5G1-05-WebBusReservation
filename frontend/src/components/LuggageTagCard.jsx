import React, { useState } from 'react'
import { generateQRCodeSVG } from '../utils/qrGenerator.js'

export default function LuggageTagCard({ luggage, onPrint }) {
  const [showQrModal, setShowQrModal] = useState(false)
  if (!luggage) return null

  const handlePrint = () => {
    if (onPrint) {
      onPrint()
    } else {
      window.print()
    }
  }

  const booking = luggage.booking || {}
  const statusColors = {
    TAGGED: 'bg-amber-100 text-amber-800 border-amber-300',
    CHECKED_IN: 'bg-blue-100 text-blue-800 border-blue-300',
    LOADED: 'bg-purple-100 text-purple-800 border-purple-300',
    COLLECTED: 'bg-emerald-100 text-emerald-800 border-emerald-300'
  }

  // Pure decoded text payload for smartphone camera scanners
  const qrDataText = `CATCHBUS LK BAGGAGE TAG
Tag ID: ${luggage.tagNumber || 'LUG-PENDING'}
E-Ticket: #${booking.bookingId || luggage.bookingId || 'N/A'}
Passenger: ${booking.passengerName || 'Passenger'}
Route: ${booking.route || 'Express Bus Network'}
Bags: ${luggage.bagQuantity} ${luggage.bagQuantity === 1 ? 'Piece' : 'Pieces'}
Status: ${luggage.status || 'TAGGED'}${luggage.description ? `\nDetails: ${luggage.description}` : ''}`

  let qrSvg = ''
  try {
    qrSvg = generateQRCodeSVG(qrDataText, 240, 3)
  } catch (err) {
    console.error('QR generation error:', err)
  }

  return (
    <div className="luggage-printable-tag relative overflow-hidden rounded-3xl border-2 border-emerald-400 bg-white p-6 shadow-card transition print:border-2 print:border-emerald-500 print:shadow-none print:m-0">
      {/* Decorative hole punch for baggage tag */}
      <div className="absolute top-4 left-1/2 -translate-x-1/2 h-5 w-5 rounded-full border-2 border-ink-900/20 bg-sand-50" />

      {/* Header */}
      <div className="pt-4 text-center border-b border-ink-900/10 pb-4">
        <span className="text-[11px] font-bold uppercase tracking-widest text-brand-600">Catch Bus LK • Digital Baggage Tag</span>
        <h3 className="mt-1 font-mono text-2xl font-black tracking-wider text-ink-950">
          {luggage.tagNumber || 'LUG-PENDING'}
        </h3>
        <div className="mt-2 inline-flex items-center gap-1.5 rounded-full border px-3 py-0.5 text-xs font-bold uppercase tracking-wide border-amber-300 bg-amber-50">
          <span className={`inline-block h-2 w-2 rounded-full ${luggage.status === 'COLLECTED' ? 'bg-emerald-500' : 'bg-amber-500'}`} />
          <span className={(statusColors[luggage.status] || 'text-ink-800') + ' border-0'}>
            Status: {luggage.status || 'TAGGED'}
          </span>
        </div>
      </div>

      {/* Luggage Details */}
      <div className="mt-4 grid grid-cols-2 gap-3 text-xs">
        <div>
          <span className="block text-[10px] uppercase font-bold text-ink-700/50">E-Ticket Ref</span>
          <span className="font-mono font-bold text-ink-900">#{booking.bookingId || luggage.bookingId || 'N/A'}</span>
        </div>
        <div>
          <span className="block text-[10px] uppercase font-bold text-ink-700/50">Total Bags</span>
          <span className="font-bold text-brand-700 text-sm">{luggage.bagQuantity} {luggage.bagQuantity === 1 ? 'Piece' : 'Pieces'}</span>
        </div>
        <div className="col-span-2">
          <span className="block text-[10px] uppercase font-bold text-ink-700/50">Passenger Name</span>
          <span className="font-semibold text-ink-900">{booking.passengerName || 'Passenger'}</span>
        </div>
        <div className="col-span-2">
          <span className="block text-[10px] uppercase font-bold text-ink-700/50">Route / Journey</span>
          <span className="font-semibold text-ink-800">{booking.route || 'Express Bus Network'}</span>
        </div>
        {luggage.description && (
          <div className="col-span-2 rounded-xl bg-sand-50 p-2.5 border border-ink-900/5">
            <span className="block text-[10px] uppercase font-bold text-ink-700/50">Baggage Identifier</span>
            <p className="text-xs text-ink-700 font-medium">{luggage.description}</p>
          </div>
        )}
      </div>

      {/* SCANNABLE QR CODE SECTION */}
      <div className="mt-5 flex flex-col items-center justify-center border-t border-ink-900/10 pt-5 text-center">
        <div className="group relative rounded-2xl border-2 border-ink-900/10 bg-white p-2.5 shadow-sm">
          {qrSvg ? (
            <div
              className="h-44 w-44 flex items-center justify-center [&>svg]:w-full [&>svg]:h-full"
              dangerouslySetInnerHTML={{ __html: qrSvg }}
            />
          ) : (
            <div className="h-44 w-44 flex items-center justify-center bg-sand-50 text-xs text-ink-600">
              Generating QR Code…
            </div>
          )}
          <div
            className="absolute inset-0 flex items-center justify-center rounded-2xl bg-ink-950/80 opacity-0 transition-opacity group-hover:opacity-100 cursor-pointer print-hidden"
            onClick={() => setShowQrModal(true)}
          >
            <span className="text-[11px] font-bold text-white px-3 py-1 bg-brand-500 rounded-lg">
              📱 View QR Data
            </span>
          </div>
        </div>

        <div className="mt-2 flex items-center gap-1.5 text-[11px] font-bold text-ink-800">
          <span>📷</span> Scan QR code with any phone camera
        </div>
        <span className="font-mono text-[10px] text-ink-700/60 tracking-widest">{luggage.tagNumber}</span>
      </div>

      {/* Card Footer Actions */}
      <div className="mt-4 flex items-center justify-between border-t border-ink-900/10 pt-3 text-[11px] text-ink-700/60 print-hidden">
        <button
          type="button"
          onClick={() => setShowQrModal(true)}
          className="font-bold text-ink-700 hover:text-ink-950 hover:underline"
        >
          🔍 Preview QR Info
        </button>
        <button
          type="button"
          onClick={handlePrint}
          className="rounded-xl bg-brand-500 px-3.5 py-1.5 font-bold text-white shadow-card hover:bg-brand-600 transition"
        >
          Print Tag 🖨️
        </button>
      </div>

      {/* QR Code Scanned Data Modal Preview */}
      {showQrModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/60 p-4 backdrop-blur-sm animate-rise print-hidden">
          <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-card-lg border border-ink-900/10">
            <div className="flex items-center justify-between border-b border-ink-900/10 pb-3">
              <div className="flex items-center gap-2">
                <span className="text-xl">📱</span>
                <h4 className="font-display font-bold text-ink-950 text-base">Scanned QR Code Details</h4>
              </div>
              <button
                onClick={() => setShowQrModal(false)}
                className="h-8 w-8 rounded-full bg-sand-100 text-ink-700 hover:bg-sand-200 font-bold"
              >
                ✕
              </button>
            </div>

            <div className="mt-4 flex justify-center">
              <div
                className="h-44 w-44 rounded-xl border p-2 bg-white flex items-center justify-center [&>svg]:w-full [&>svg]:h-full"
                dangerouslySetInnerHTML={{ __html: qrSvg }}
              />
            </div>

            <div className="mt-4 rounded-2xl bg-sand-50 p-4 text-xs font-mono text-ink-900 whitespace-pre-line leading-relaxed border border-ink-900/5">
              {qrDataText}
            </div>

            <p className="mt-3 text-center text-[11px] text-ink-700/60">
              Conductors, station staff, or passengers can scan this QR code instantly from their smartphone to inspect luggage origin, passenger details, and cargo status.
            </p>

            <button
              onClick={() => setShowQrModal(false)}
              className="mt-4 w-full rounded-xl bg-brand-500 py-2.5 font-bold text-white hover:bg-brand-600 transition"
            >
              Close Preview
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

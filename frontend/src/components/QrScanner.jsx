import React, { useEffect, useRef, useState } from 'react'

/**
 * Camera QR scanner using the browser BarcodeDetector API.
 * Chrome/Android generally supports QR detection. The manual QR payload field
 * remains available for browsers that do not expose BarcodeDetector.
 */
export default function QrScanner({ onScan, onClose }) {
  const videoRef = useRef(null)
  const streamRef = useRef(null)
  const frameRef = useRef(null)
  const [status, setStatus] = useState('Starting camera…')
  const [supported, setSupported] = useState(true)

  useEffect(() => {
    let cancelled = false

    const stop = () => {
      if (frameRef.current) cancelAnimationFrame(frameRef.current)
      if (streamRef.current) streamRef.current.getTracks().forEach(t => t.stop())
      streamRef.current = null
    }

    const start = async () => {
      if (!('BarcodeDetector' in window)) {
        setSupported(false)
        setStatus('This browser does not support camera QR detection. Use the manual QR payload field below.')
        return
      }
      try {
        const detector = new window.BarcodeDetector({ formats: ['qr_code'] })
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
          audio: false
        })
        if (cancelled) { stream.getTracks().forEach(t => t.stop()); return }
        streamRef.current = stream
        videoRef.current.srcObject = stream
        await videoRef.current.play()
        setStatus('Point the rear camera at the staff QR code.')

        const scan = async () => {
          if (cancelled || !videoRef.current) return
          try {
            const codes = await detector.detect(videoRef.current)
            if (codes.length && codes[0].rawValue) {
              onScan(codes[0].rawValue)
              stop()
              return
            }
          } catch (_) {}
          frameRef.current = requestAnimationFrame(scan)
        }
        frameRef.current = requestAnimationFrame(scan)
      } catch (e) {
        setStatus(e.name === 'NotAllowedError'
          ? 'Camera permission was denied. Allow camera access and try again.'
          : 'Could not start the camera. Use the manual QR payload field instead.')
      }
    }

    start()
    return stop
  }, [onScan])

  return (
    <div className="mt-4 rounded-2xl border border-signal-cyan/30 bg-ops-900 p-4">
      <div className="relative aspect-video overflow-hidden rounded-xl bg-black">
        <video ref={videoRef} playsInline muted className="h-full w-full object-cover" />
        <div className="pointer-events-none absolute inset-[18%] rounded-2xl border-2 border-signal-cyan/80" />
      </div>
      <p className="mt-3 text-xs text-slate-400">{status}</p>
      {!supported && (
        <p className="mt-2 text-xs text-signal-amber">
          Tip: camera access normally requires HTTPS on a phone (localhost is allowed for local development).
        </p>
      )}
      <button type="button" onClick={onClose}
        className="mt-3 w-full rounded-lg border border-white/10 py-2 text-sm font-semibold text-slate-300 hover:bg-white/5">
        Close scanner
      </button>
    </div>
  )
}

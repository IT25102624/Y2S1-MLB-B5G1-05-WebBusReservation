'use strict';

class SeatBookingApp {
    constructor() {
        this.tripData = document.getElementById('trip-data');
        this.tripId = this.tripData.dataset.tripId;
        this.sessionId = this.getOrCreateSessionId();

        this.selectedSeatIds = [];
        this.selectedSeatObjects = [];
        this.lockExpiresAt = null;
        this.timerInterval = null;
        this.allSeats = [];

        this.init();
    }

    getOrCreateSessionId() {
        let sid = sessionStorage.getItem('busBookingSessionId');
        if (!sid) {
            sid = 'sess_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
            sessionStorage.setItem('busBookingSessionId', sid);
        }
        return sid;
    }

    async init() {
        await this.loadSeats();
        this.bindConfirmButton();
    }

    // ─── Load & Render Seats ──────────────────────────────────────
    async loadSeats() {
        try {
            const resp = await fetch(`/api/trips/${this.tripId}/seats?sessionId=${this.sessionId}`);
            if (!resp.ok) throw new Error('Failed to load seats');
            this.allSeats = await resp.json();
            this.renderSeatGrid(this.allSeats);
        } catch (e) {
            document.getElementById('seat-grid-container').innerHTML =
                `<div style="color:var(--red);text-align:center;padding:24px">Unable to load seat map. Please refresh.</div>`;
        }
    }

    renderSeatGrid(seats) {
        const container = document.getElementById('seat-grid-container');
        container.innerHTML = '';

        // Group by row
        const rowMap = {};
        let maxCol = 0;
        seats.forEach(s => {
            if (!rowMap[s.rowNum]) rowMap[s.rowNum] = [];
            rowMap[s.rowNum].push(s);
            if (s.colNum > maxCol) maxCol = s.colNum;
        });

        // Determine aisle position (after col 2 in a 4-col layout, after col 1 in 3-col)
        const aisleAfterCol = maxCol >= 4 ? 2 : (maxCol >= 3 ? 1 : null);

        const sortedRows = Object.keys(rowMap).map(Number).sort((a, b) => a - b);

        sortedRows.forEach(rowNum => {
            const rowDiv = document.createElement('div');
            rowDiv.className = 'seat-row';

            const rowSeats = rowMap[rowNum].sort((a, b) => a.colNum - b.colNum);

            rowSeats.forEach(seat => {
                // Insert aisle gap before the 3rd column in a 4-col layout
                if (aisleAfterCol && seat.colNum === aisleAfterCol + 1) {
                    const gap = document.createElement('div');
                    gap.className = 'aisle-gap';
                    rowDiv.appendChild(gap);
                }
                rowDiv.appendChild(this.createSeatElement(seat));
            });

            container.appendChild(rowDiv);
        });
    }

    createSeatElement(seat) {
        const el = document.createElement('div');
        const isSleeper = seat.seatType && (seat.seatType.toLowerCase().includes('sleeper'));
        el.className = `seat ${seat.status.toLowerCase()} ${isSleeper ? 'sleeper' : ''} ${seat.lockedByMe ? 'locked-by-me' : ''}`;
        el.dataset.seatId = seat.id;
        el.dataset.seatNumber = seat.seatNumber;
        el.dataset.price = seat.calculatedPrice;
        el.dataset.status = seat.status;
        el.dataset.lockedByMe = seat.lockedByMe;

        el.innerHTML = `
            <span class="seat-num">${seat.seatNumber}</span>
            <span class="seat-price-badge">Rs ${Math.round(seat.calculatedPrice)}</span>
        `;

        if (seat.status === 'AVAILABLE') {
            el.addEventListener('click', () => this.toggleSeat(el, seat));
        } else if (seat.lockedByMe) {
            el.addEventListener('click', () => this.toggleSeat(el, seat));
        }

        return el;
    }

    // ─── Seat Selection / Deselection ─────────────────────────────
    toggleSeat(el, seat) {
        const idx = this.selectedSeatIds.indexOf(seat.id);
        if (idx === -1) {
            this.selectSeat(el, seat);
        } else {
            this.deselectSeat(el, seat, idx);
        }
    }

    selectSeat(el, seat) {
        this.selectedSeatIds.push(seat.id);
        this.selectedSeatObjects.push(seat);
        el.classList.remove('available');
        el.classList.add('selected');
        this.lockSelectedSeats();
        this.updateCheckoutPanel();
    }

    deselectSeat(el, seat, idx) {
        this.selectedSeatIds.splice(idx, 1);
        this.selectedSeatObjects.splice(idx, 1);
        el.classList.remove('selected');
        el.classList.add('available');
        this.updateCheckoutPanel();

        if (this.selectedSeatIds.length === 0) {
            this.releaseAllLocks();
        }
    }

    // ─── Seat Locking ─────────────────────────────────────────────
    async lockSelectedSeats() {
        if (this.selectedSeatIds.length === 0) return;

        try {
            const resp = await fetch('/api/seats/lock', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    tripId: parseInt(this.tripId),
                    seatIds: this.selectedSeatIds,
                    sessionId: this.sessionId
                })
            });

            const result = await resp.json();

            if (result.success) {
                this.lockExpiresAt = result.expiresAt ? new Date(result.expiresAt) : new Date(Date.now() + result.lockDurationSeconds * 1000);
                this.startTimer();
            } else {
                this.showToast(result.message || 'Could not lock seat. It may have been taken.', 'error');
                // Reload seats to get current state
                await this.loadSeats();
                this.selectedSeatIds = [];
                this.selectedSeatObjects = [];
                this.updateCheckoutPanel();
            }
        } catch (e) {
            this.showToast('Network error. Please try again.', 'error');
        }
    }

    async releaseAllLocks() {
        if (this.selectedSeatIds.length === 0) return;
        try {
            await fetch('/api/seats/unlock', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    tripId: parseInt(this.tripId),
                    seatIds: [...this.selectedSeatIds],
                    sessionId: this.sessionId
                })
            });
        } catch (_) {}
        this.stopTimer();
    }

    // ─── Timer ────────────────────────────────────────────────────
    startTimer() {
        this.stopTimer();
        const container = document.getElementById('lock-timer-container');
        container.style.display = 'block';

        this.timerInterval = setInterval(() => {
            if (!this.lockExpiresAt) return;
            const remaining = Math.max(0, Math.floor((this.lockExpiresAt - Date.now()) / 1000));

            if (remaining <= 0) {
                this.stopTimer();
                document.getElementById('lock-timer').className = 'timer-badge expired';
                document.getElementById('timer-display').textContent = '0:00';
                this.showToast('Your seat hold has expired. Please re-select your seats.', 'error');
                this.selectedSeatIds = [];
                this.selectedSeatObjects = [];
                this.loadSeats();
                this.updateCheckoutPanel();
                return;
            }

            const m = Math.floor(remaining / 60);
            const s = remaining % 60;
            document.getElementById('timer-display').textContent = `${m}:${s.toString().padStart(2, '0')}`;
            document.getElementById('lock-timer').className = remaining < 60 ? 'timer-badge expired' : 'timer-badge';
        }, 1000);
    }

    stopTimer() {
        if (this.timerInterval) {
            clearInterval(this.timerInterval);
            this.timerInterval = null;
        }
        const container = document.getElementById('lock-timer-container');
        if (container) container.style.display = 'none';
    }

    // ─── Checkout Panel Update ─────────────────────────────────────
    updateCheckoutPanel() {
        const noSeatsMsg = document.getElementById('no-seats-msg');
        const detailsSection = document.getElementById('passenger-details-section');
        const sub = document.querySelector('.panel-sub');

        if (this.selectedSeatIds.length === 0) {
            noSeatsMsg.style.display = 'block';
            detailsSection.style.display = 'none';
            sub.textContent = 'No seats selected yet.';
            document.getElementById('confirm-booking-btn').disabled = true;
            return;
        }

        noSeatsMsg.style.display = 'none';
        detailsSection.style.display = 'block';
        sub.textContent = `${this.selectedSeatIds.length} seat(s) selected`;

        this.renderPassengerCards();
        this.fetchAndRenderFare();
        document.getElementById('confirm-booking-btn').disabled = false;
    }

    renderPassengerCards() {
        const container = document.getElementById('passenger-cards-container');
        container.innerHTML = '';

        this.selectedSeatObjects.forEach((seat, idx) => {
            const card = document.createElement('div');
            card.className = 'passenger-card';
            card.innerHTML = `
                <div class="passenger-card-header">Seat ${seat.seatNumber} — ${seat.seatTypeDisplayName || seat.seatType}</div>
                <div class="form-group">
                    <label>Passenger Name</label>
                    <input type="text" class="form-control pax-name" data-idx="${idx}" placeholder="Full name">
                </div>
                <div class="two-col">
                    <div class="form-group">
                        <label>Age</label>
                        <input type="number" class="form-control pax-age" data-idx="${idx}" placeholder="Age" min="1" max="120">
                    </div>
                    <div class="form-group">
                        <label>Gender</label>
                        <select class="form-control pax-gender" data-idx="${idx}">
                            <option value="">Select</option>
                            <option value="Male">Male</option>
                            <option value="Female">Female</option>
                            <option value="Other">Other</option>
                        </select>
                    </div>
                </div>
            `;
            container.appendChild(card);
        });
    }

    async fetchAndRenderFare() {
        if (this.selectedSeatIds.length === 0) return;

        try {
            const resp = await fetch(`/api/fare/calculate?tripId=${this.tripId}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(this.selectedSeatIds)
            });

            const fare = await resp.json();
            document.getElementById('fare-base').textContent  = `Rs ${fare.subTotal.toFixed(2)}`;
            document.getElementById('fare-tax').textContent   = `Rs ${(fare.taxAmount + fare.convenienceFee).toFixed(2)}`;
            document.getElementById('fare-total').textContent = `Rs ${fare.grandTotal.toFixed(2)}`;
        } catch (e) {
            // Silently skip fare update on error
        }
    }

    // ─── Confirm Booking ──────────────────────────────────────────
    bindConfirmButton() {
        document.getElementById('confirm-booking-btn').addEventListener('click', () => this.confirmBooking());
    }

    async confirmBooking() {
        const name  = document.getElementById('contact-name').value.trim();
        const phone = document.getElementById('contact-phone').value.trim();
        const email = document.getElementById('contact-email').value.trim();

        if (!name)  { this.showToast('Please enter your full name.', 'error'); return; }
        if (!phone) { this.showToast('Please enter your mobile number.', 'error'); return; }
        if (!email) { this.showToast('Please enter your email address.', 'error'); return; }

        const passengers = [];
        const nameInputs   = document.querySelectorAll('.pax-name');
        const ageInputs    = document.querySelectorAll('.pax-age');
        const genderInputs = document.querySelectorAll('.pax-gender');

        for (let i = 0; i < this.selectedSeatObjects.length; i++) {
            const paxName = nameInputs[i]?.value.trim();
            if (!paxName) { this.showToast(`Please enter a name for Seat ${this.selectedSeatObjects[i].seatNumber}.`, 'error'); return; }

            passengers.push({
                seatNumber: this.selectedSeatObjects[i].seatNumber,
                passengerName: paxName,
                age: parseInt(ageInputs[i]?.value) || null,
                gender: genderInputs[i]?.value || null
            });
        }

        const payload = {
            tripId: parseInt(this.tripId),
            sessionId: this.sessionId,
            primaryPassengerName:  name,
            primaryPassengerPhone: phone,
            primaryPassengerEmail: email,
            passengers: passengers
        };

        const btn = document.getElementById('confirm-booking-btn');
        btn.disabled = true;
        btn.textContent = 'Confirming...';

        try {
            const resp = await fetch('/api/bookings/confirm', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await resp.json();

            if (result.success) {
                this.stopTimer();
                this.showToast('Booking confirmed! Redirecting...', 'info');
                setTimeout(() => {
                    window.location.href = `/booking/confirmation?pnr=${result.pnr}`;
                }, 1000);
            } else {
                this.showToast(result.message || 'Booking failed. Please try again.', 'error');
                btn.disabled = false;
                btn.textContent = 'Confirm Booking';
            }
        } catch (e) {
            this.showToast('Network error. Please try again.', 'error');
            btn.disabled = false;
            btn.textContent = 'Confirm Booking';
        }
    }

    // ─── Toast ────────────────────────────────────────────────────
    showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast-msg ${type}`;
        toast.textContent = message;
        container.innerHTML = '';
        container.appendChild(toast);
        setTimeout(() => { if (toast.parentNode) toast.parentNode.removeChild(toast); }, 5000);
    }
}

// Init on page load
document.addEventListener('DOMContentLoaded', () => new SeatBookingApp());

const express = require('express');
const router = express.Router();

// Sample seats data
const seats = [
  { id: 'A1', row: 'A', number: 1, available: true },
  { id: 'A2', row: 'A', number: 2, available: false },
  { id: 'B1', row: 'B', number: 1, available: true },
];

router.get('/', (req, res) => {
  res.json({ seats });
});

router.post('/reserve', (req, res) => {
  const { id } = req.body;
  const seat = seats.find(s => s.id === id);
  if (!seat) {
    return res.status(404).json({ error: 'Seat not found' });
  }
  if (!seat.available) {
    return res.status(400).json({ error: 'Seat already reserved' });
  }
  seat.available = false;
  res.json({ success: true, seat });
});

module.exports = router;

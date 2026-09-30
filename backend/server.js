// Simple Express backend placeholder for seat booking
const express = require('express');
const cors = require('cors');
const seatRoutes = require('./routes/seats');

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok' });
});

// Seat reservation routes
app.use('/api/seats', seatRoutes);
app.use('/seats', seatRoutes);

app.listen(PORT, () => {
  console.log(`Backend server running on port ${PORT}`);
});

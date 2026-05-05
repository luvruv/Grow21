const express = require('express');
const cors = require('cors');
const apiRoutes = require('./routes/apiRoutes');

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json()); // Parses incoming JSON requests

// Request Logging Middleware
app.use((req, res, next) => {
  const timestamp = new Date().toISOString();
  console.log(`\n[${timestamp}] ➡️  ${req.method} ${req.url}`);
  if (req.body && Object.keys(req.body).length > 0) {
    // Mask passwords for security in logs
    const safeBody = { ...req.body };
    if (safeBody.password) safeBody.password = '***';
    if (safeBody.ParentPassword) safeBody.ParentPassword = '***';
    if (safeBody.ChildPassword) safeBody.ChildPassword = '***';
    console.log(`  📦 Payload:`, JSON.stringify(safeBody));
  }
  if (Object.keys(req.query).length > 0) {
    console.log(`  🔍 Query:`, req.query);
  }

  // Log the response when it finishes
  const originalSend = res.send.bind(res);
  res.send = function (body) {
    console.log(`  ⬅️  Response: ${res.statusCode}`);
    return originalSend(body);
  };
  const originalJson = res.json.bind(res);
  res.json = function (body) {
    console.log(`  ⬅️  Response: ${res.statusCode}`, JSON.stringify(body).substring(0, 200));
    return originalJson(body);
  };

  next();
});

// Welcome Route
app.get('/', (req, res) => {
  res.send('Welcome to the GROW21 DBMS Project Backend API!');
});

// API Routes
app.use('/api', apiRoutes);

// Start server — bind to 0.0.0.0 so physical devices on LAN can reach it
app.listen(PORT, '0.0.0.0', () => {
  console.log(`\n🚀 Server is running on http://0.0.0.0:${PORT}`);
  console.log(`   Local:   http://localhost:${PORT}`);
  // Show the LAN IP for physical device access
  const nets = require('os').networkInterfaces();
  for (const name of Object.keys(nets)) {
    for (const net of nets[name]) {
      if (net.family === 'IPv4' && !net.internal) {
        console.log(`   Network: http://${net.address}:${PORT}  ← Use this on your phone`);
      }
    }
  }
  console.log('');
});

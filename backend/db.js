require('dotenv').config();

const mysql = require('mysql2');

const sslEnabled = String(process.env.DB_SSL || '').toLowerCase() === 'true';

const pool = mysql.createPool({
  host:     process.env.DB_HOST,
  port:     Number(process.env.DB_PORT) || 3306,
  user:     process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
  multipleStatements: true,
  ...(sslEnabled ? { ssl: { rejectUnauthorized: false } } : {})
});

// Test the pool connection on startup
pool.getConnection((err, connection) => {
  if (err) {
    console.error('Error connecting to MySQL database pool:', err);
    return;
  }
  console.log(`Successfully connected to MySQL database: ${process.env.DB_NAME} @ ${process.env.DB_HOST}`);
  connection.release();
});

module.exports = pool;

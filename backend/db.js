// Loads C:/Grow21/.env (project root) so backend/ and the script cwd both work.
require('dotenv').config({ path: require('path').resolve(__dirname, '..', '.env') });

const mysql = require('mysql2');

const sslEnabled = String(process.env.DB_SSL || '').toLowerCase() === 'true';

const db = mysql.createConnection({
  host:     process.env.DB_HOST,
  port:     Number(process.env.DB_PORT) || 3306,
  user:     process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  multipleStatements: true,                        // needed for "CALL proc(...); SELECT @out;"
  ...(sslEnabled ? { ssl: { rejectUnauthorized: false } } : {})
});

db.connect((err) => {
  if (err) {
    console.error('Error connecting to MySQL database:', err);
    return;
  }
  console.log(`Successfully connected to MySQL database: ${process.env.DB_NAME} @ ${process.env.DB_HOST}`);
});

module.exports = db;

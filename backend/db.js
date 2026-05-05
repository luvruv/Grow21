const mysql = require('mysql2');

const db = mysql.createConnection({
  host: 'localhost',
  user: 'root',
  password: 'bhavya', // Please change this to your actual MySQL root password
  database: 'Grow21_DB'
});

db.connect((err) => {
  if (err) {
    console.error('Error connecting to MySQL database:', err);
    return;
  }
  console.log('Successfully connected to MySQL database: Grow21_DB');
});

module.exports = db;

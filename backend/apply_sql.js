require('dotenv').config({ path: require('path').resolve(__dirname, '..', '.env') });

const fs = require('fs');
const path = require('path');
const mysql = require('mysql2');

const sslEnabled = String(process.env.DB_SSL || '').toLowerCase() === 'true';

// Default to the cloud-init script at project root; override with: node apply_sql.js <path>
const sqlPath = path.resolve(__dirname, '..', process.argv[2] || 'Grow21_Cloud_Init.sql');

const connection = mysql.createConnection({
  host:     process.env.DB_HOST,
  port:     Number(process.env.DB_PORT) || 3306,
  user:     process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  multipleStatements: true,
  ...(sslEnabled ? { ssl: { rejectUnauthorized: false } } : {})
});

connection.connect((err) => {
  if (err) throw err;
  console.log(`Connected to MySQL: ${process.env.DB_NAME} @ ${process.env.DB_HOST}`);
  console.log(`Applying SQL from: ${sqlPath}`);

  const sqlScript = fs.readFileSync(sqlPath, 'utf8');

  // mysql2's multipleStatements does not understand the DELIMITER directive,
  // so split out procedure bodies and run each as a single statement.
  const parts = sqlScript.split('DELIMITER //');
  const queriesToRun = [];

  queriesToRun.push(parts[0]);

  for (let i = 1; i < parts.length; i++) {
    const subParts = parts[i].split('DELIMITER ;');
    const sps = subParts[0].split('//').map(s => s.trim()).filter(s => s.length > 0);
    queriesToRun.push(...sps);

    if (subParts.length > 1) {
      queriesToRun.push(subParts[1]);
    }
  }

  let currentIdx = 0;
  function runNext() {
    if (currentIdx >= queriesToRun.length) {
      console.log('Database initialized successfully.');
      process.exit(0);
    }
    const q = queriesToRun[currentIdx].trim();
    if (!q) {
      currentIdx++;
      return runNext();
    }
    connection.query(q, (err) => {
      if (err) {
        console.error(`Error at chunk ${currentIdx}: ${err.message}`);
        console.error(q.substring(0, 200) + (q.length > 200 ? '...' : ''));
        process.exit(1);
      }
      currentIdx++;
      runNext();
    });
  }

  runNext();
});

const fs = require('fs');
const mysql = require('mysql2');

const connection = mysql.createConnection({
  host: 'localhost',
  user: 'root',
  password: 'bhavya',
  multipleStatements: true
});

connection.connect((err) => {
  if (err) throw err;
  console.log('Connected to MySQL server.');
  
  // Drop DB if exists
  connection.query('DROP DATABASE IF EXISTS Grow21_DB;', (err) => {
    if (err) throw err;
    console.log('Database dropped.');
    
    // Read SQL file
    let sqlScript = fs.readFileSync('c:/Grow21/Grow21_DBMS_Complete.sql', 'utf8');
    
    // We need to split the script into parts separated by DELIMITER //
    // mysql2 multipleStatements does not support DELIMITER keyword
    const parts = sqlScript.split('DELIMITER //');
    
    let queriesToRun = [];
    
    // First part is regular SQL with ;
    queriesToRun.push(parts[0]);
    
    // Subsequent parts contain procedures ending with //, and then DELIMITER ;
    for (let i = 1; i < parts.length; i++) {
        const subParts = parts[i].split('DELIMITER ;');
        // subParts[0] contains the SPs separated by //
        const sps = subParts[0].split('//').map(s => s.trim()).filter(s => s.length > 0);
        queriesToRun.push(...sps);
        
        if (subParts.length > 1) {
            queriesToRun.push(subParts[1]); // the rest of the script
        }
    }
    
    // Execute sequentially
    let currentIdx = 0;
    function runNext() {
        if (currentIdx >= queriesToRun.length) {
            console.log('Database recreated successfully from script.');
            process.exit(0);
        }
        
        let q = queriesToRun[currentIdx].trim();
        if (!q) {
            currentIdx++;
            runNext();
            return;
        }
        
        connection.query(q, (err) => {
            if (err) {
                console.error('Error executing query at index ' + currentIdx + ': ' + err.message);
                console.error(q.substring(0, 100) + '...');
                process.exit(1);
            }
            currentIdx++;
            runNext();
        });
    }
    
    runNext();
  });
});
